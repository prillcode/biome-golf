package com.prillcode.minecraftgolf.world;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.ball.BallCollisionWorld;
import com.prillcode.minecraftgolf.ball.CollisionResult;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.surface.SurfaceDefinition;

/**
 * Minecraft-backed {@link BallCollisionWorld} used by the server-side golf-ball
 * entity (ARCHITECTURE.md §2.3, §10).
 *
 * <p>The physics engine moves a ball of radius {@code r} by sweeping its AABB
 * along a straight segment. Minecraft remains responsible for all block
 * geometry: candidate collision shapes come straight from the level, and the
 * segment is clamped with the same continuous per-axis sweep Minecraft uses
 * for its own entities (no teleporting, so thin geometry cannot be skipped
 * within a single call).</p>
 *
 * <p>Only blocks collide with the ball. Other entities (players, mobs, other
 * balls) never block a shot, per ARCHITECTURE.md §24 MVP scope.</p>
 *
 * <p>This adapter is created lazily on the server tick and is never touched on
 * the client; it lives in {@code src/main} because it only references
 * server-safe Minecraft classes.</p>
 */
public final class MinecraftBallCollisionWorld implements BallCollisionWorld {

	private static final double EPSILON = 1.0E-7;
	private static final double MAX_STEP_UP = 1.0;
	private static final double NEAR_GROUND_PROBE = 1.0;

	private final Level level;
	private final Entity contextEntity;
	private final double radius;
	private final GolfBlockSurfaceResolver surfaceResolver;

	public MinecraftBallCollisionWorld(Level level, Entity contextEntity, double radius,
			GolfBlockSurfaceResolver surfaceResolver) {
		this.level = level;
		this.contextEntity = contextEntity;
		this.radius = radius;
		this.surfaceResolver = surfaceResolver;
	}

	@Override
	public CollisionResult move(Vec3 from, Vec3 delta) {
		net.minecraft.world.phys.Vec3 mcFrom = toMc(from);
		net.minecraft.world.phys.Vec3 mcDelta = toMc(delta);

		if (mcDelta.lengthSqr() == 0.0) {
			return CollisionResult.moved(from);
		}

		// Ball bounding box centered on the current center.
		AABB box = new AABB(mcFrom.x - radius, mcFrom.y - radius, mcFrom.z - radius,
				mcFrom.x + radius, mcFrom.y + radius, mcFrom.z + radius);

		// Continuous per-axis sweep with all block shapes along the path; the
		// same algorithm Minecraft uses for its own entities, restricted to
		// blocks (the collider list is empty, so players never block a shot).
		net.minecraft.world.phys.Vec3 adjusted =
				Entity.collideBoundingBox(contextEntity, mcDelta, box, level, List.of());

		// Determine which axes were actually blocked (beyond float noise).
		Direction.Axis blockedAxis = null;
		for (Direction.Axis axis : Direction.Axis.VALUES) {
			double requested = mcDelta.get(axis);
			if (requested == 0.0) {
				continue;
			}
			double allowed = adjusted.get(axis);
			if (Math.abs(allowed - requested) > EPSILON * Math.max(1.0, Math.abs(requested))) {
				if (blockedAxis == null
						|| Math.abs(mcDelta.get(blockedAxis)) < Math.abs(requested)) {
					// Prefer the axis with the largest attempted travel so the
					// reported normal matches the dominant contact direction.
					blockedAxis = axis;
				}
			}
		}

		if (blockedAxis == null) {
			return CollisionResult.moved(toDomain(mcFrom.add(mcDelta)));
		}

		if (blockedAxis != Direction.Axis.Y) {
			net.minecraft.world.phys.Vec3 stepped = tryStepMove(
					box, mcDelta, adjusted,
					(candidateBox, requested) -> Entity.collideBoundingBox(
							contextEntity, requested, candidateBox, level, List.of()));
			if (stepped != null) {
				MinecraftGolf.LOGGER.info(
						"Golf ball {} stepped terrain rise={} at ({}, {}, {})",
						contextEntity.getId(), stepped.y, mcFrom.x, mcFrom.y, mcFrom.z);
				return CollisionResult.moved(toDomain(mcFrom.add(stepped)));
			}
		}

		// Contact normal points back toward the ball (opposite the attempted
		// travel on the blocked axis), matching the S01 synthetic worlds.
		double sign = -Math.signum(mcDelta.get(blockedAxis));
		Vec3 normal = switch (blockedAxis) {
			case X -> Vec3.of(sign, 0.0, 0.0);
			case Y -> Vec3.of(0.0, sign, 0.0);
			case Z -> Vec3.of(0.0, 0.0, sign);
		};

		Vec3 center = toDomain(mcFrom.add(adjusted));
		return CollisionResult.hit(center, normal);
	}

	/**
	 * Attempts a vanilla-style alternate path over a one-block ledge. This is
	 * deliberately limited to motion with lower terrain nearby: genuinely
	 * airborne wall hits and sheer faces taller than one block keep their normal
	 * collision response.
	 */
	static net.minecraft.world.phys.Vec3 tryStepMove(
			AABB box,
			net.minecraft.world.phys.Vec3 requested,
			net.minecraft.world.phys.Vec3 blocked,
			CollisionSweep sweep) {
		// The visible block staircase is treated as terrain only while lower ground
		// is nearby. This includes the upward phase immediately after a ground
		// bounce, which is the common way shots encounter Minecraft hills.
		net.minecraft.world.phys.Vec3 support = sweep.move(
				box, new net.minecraft.world.phys.Vec3(0.0, -NEAR_GROUND_PROBE, 0.0));
		if (support.y <= -NEAR_GROUND_PROBE + EPSILON) {
			return null;
		}

		net.minecraft.world.phys.Vec3 up =
				sweep.move(box, new net.minecraft.world.phys.Vec3(0.0, MAX_STEP_UP, 0.0));
		if (up.y <= EPSILON) {
			return null;
		}

		AABB raised = box.move(0.0, up.y, 0.0);
		net.minecraft.world.phys.Vec3 horizontalRequest =
				new net.minecraft.world.phys.Vec3(requested.x, 0.0, requested.z);
		net.minecraft.world.phys.Vec3 horizontal = sweep.move(raised, horizontalRequest);
		double normalProgress = blocked.x * blocked.x + blocked.z * blocked.z;
		double stepProgress = horizontal.x * horizontal.x + horizontal.z * horizontal.z;
		if (stepProgress <= normalProgress + EPSILON) {
			return null;
		}

		AABB advanced = raised.move(horizontal);
		net.minecraft.world.phys.Vec3 down = sweep.move(
				advanced, new net.minecraft.world.phys.Vec3(0.0, requested.y - up.y, 0.0));
		double rise = up.y + down.y;
		if (rise <= EPSILON || rise > MAX_STEP_UP + EPSILON) {
			return null;
		}
		return new net.minecraft.world.phys.Vec3(horizontal.x, rise, horizontal.z);
	}

	@FunctionalInterface
	interface CollisionSweep {
		net.minecraft.world.phys.Vec3 move(
				AABB box, net.minecraft.world.phys.Vec3 requested);
	}

	@Override
	public SurfaceDefinition surfaceAt(Vec3 ballCenter) {
		// Surface semantics come from the block the ball rests on or, when it
		// is not touching anything, the block nearest its center. Sampling uses
		// the ball's lowest point so a ball resting on top of a block resolves
		// that block (ARCHITECTURE.md §11).
		BlockPos pos = blockBelow(ballCenter);
		if (pos == null || level.isEmptyBlock(pos)) {
			pos = blockAt(ballCenter);
		}
		if (pos == null || level.isEmptyBlock(pos)) {
			return SurfaceDefinition.NORMAL;
		}
		return surfaceResolver.resolve(level.getBlockState(pos));
	}

	private BlockPos blockBelow(Vec3 center) {
		double y = center.y() - radius - 0.001;
		if (y < level.getMinY()) {
			return null;
		}
		return BlockPos.containing(center.x(), y, center.z());
	}

	private BlockPos blockAt(Vec3 center) {
		return BlockPos.containing(center.x(), center.y(), center.z());
	}

	private static net.minecraft.world.phys.Vec3 toMc(Vec3 v) {
		return new net.minecraft.world.phys.Vec3(v.x(), v.y(), v.z());
	}

	private static Vec3 toDomain(net.minecraft.world.phys.Vec3 v) {
		return Vec3.of(v.x, v.y, v.z);
	}
}
