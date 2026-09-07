package com.prillcode.minecraftgolf.entity;

import org.slf4j.Logger;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.ball.BallPhysics;
import com.prillcode.minecraftgolf.ball.BallState;
import com.prillcode.minecraftgolf.ball.PhysicsConfig;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.world.GolfBlockSurfaceResolver;
import com.prillcode.minecraftgolf.world.MinecraftBallCollisionWorld;

/**
 * Server-authoritative golf ball entity (ARCHITECTURE.md §9, MILESTONES M1).
 *
 * <p>Every server tick the authoritative {@link BallState} is advanced by the
 * pure-Java {@link BallPhysics} engine through a {@link MinecraftBallCollisionWorld}
 * adapter. Minecraft provides only world geometry and synchronization: this
 * entity never uses Minecraft's own movement/gravity, so the S01 physics stays
 * the single source of truth for position and velocity.</p>
 *
 * <p>Convention: the entity position <em>is</em> the ball center (physics
 * coordinates), not the feet. Clients are kept in sync through the normal
 * entity tracker; the renderer (client slice S04) draws around this point.</p>
 *
 * <p>State lifecycle: a fresh spawn (summon) starts airborne with zero
 * velocity so it drops and settles under gravity. A chunk reload restores the
 * saved velocity/grounded/resting flags from NBT. A resting ball stays resting
 * until {@link #launch} is called.</p>
 */
public class GolfBallEntity extends Entity {

	private static final Logger LOGGER = MinecraftGolf.LOGGER;

	/** Physical ball radius in blocks (diameter 0.5); also used for rendering bounds later. */
	public static final double BALL_RADIUS = 0.25;

	private static final PhysicsConfig PHYSICS_CONFIG = PhysicsConfig.DEFAULT;

	private static final String NBT_VX = "golf_vx";
	private static final String NBT_VY = "golf_vy";
	private static final String NBT_VZ = "golf_vz";
	private static final String NBT_GROUNDED = "golf_grounded";
	private static final String NBT_RESTING = "golf_resting";
	private static final String NBT_OWNER = "golf_owner"; // owner player UUID, or "" when unowned

	/**
	 * Owner player UUID or {@code null} while the ball is unclaimed.
	 * A player can only strike a ball they own (M2, ARCH §12); a resting ball is
	 * claimed by whoever first launches it. Persisted to NBT between loads.
	 */
	private java.util.UUID ownerUuid;

	private final GolfBlockSurfaceResolver surfaceResolver = new GolfBlockSurfaceResolver();
	private final MinecraftBallCollisionWorld collisionWorld;

	/** Authoritative state; null until the first server tick. */
	private BallState state;

	/** Whether the state has been anchored to the entity's current position. */
	private boolean initialized;

	public GolfBallEntity(EntityType<? extends GolfBallEntity> type, Level level) {
		super(type, level);
		this.collisionWorld = new MinecraftBallCollisionWorld(level, this, BALL_RADIUS, surfaceResolver);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		// No synced fields: position is driven by the entity tracker and all
		// authoritative state lives server-side.
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// The ball is not damageable. M1 MVP has no combat interaction with it.
		return false;
	}

	@Override
	public boolean isPushable() {
		// Never pushed by players/mobs/pistons; physics owns all movement.
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			return;
		}

		if (!initialized) {
			initializeState();
		}
		if (state == null) {
			return;
		}

		if (state.resting()) {
			// Resting is terminal until launch() is called; no per-tick work.
			return;
		}

		BallState next = BallPhysics.step(state, PHYSICS_CONFIG, collisionWorld);
		if (!next.equals(state)) {
			state = next;
			applyPosition(state.position());
			if (state.resting()) {
				LOGGER.info("Golf ball {} came to rest at {}", getId(), fmt(state.position()));
			}
		}
	}

	/**
	 * Launches the ball from its current position with a clamped velocity.
	 * Server-side only; called by the dev launch controls (slice S03) and by
	 * future shot execution.
	 */
	public void launch(Vec3 velocity) {
		if (level().isClientSide()) {
			return;
		}
		if (state == null) {
			state = BallState.atRest(currentCenter());
		}
		Vec3 clamped = BallPhysics.clampLaunch(velocity, PHYSICS_CONFIG);
		state = BallState.launched(state.position(), clamped);
		applyPosition(state.position());
		LOGGER.info("Golf ball {} launched at {} v={}", getId(), fmt(state.position()), fmt(state.velocity()));
	}

	/** Current authoritative state, or null before the first server tick. */
	public BallState ballState() {
		return state;
	}

	public boolean isResting() {
		return state != null && state.resting();
	}

	/** Owner player UUID, or {@code null} when the ball is unclaimed. */
	public java.util.UUID owner() {
		return ownerUuid;
	}

	/** Claims (or reassigns) the ball to a player. Server-side only. */
	public void setOwner(java.util.UUID playerUuid) {
		if (level().isClientSide()) {
			return;
		}
		this.ownerUuid = playerUuid;
	}

	/** Whether the given player may strike this ball (owns it, or it is unclaimed). */
	public boolean canBeStruckBy(java.util.UUID playerUuid) {
		return playerUuid != null && (ownerUuid == null || ownerUuid.equals(playerUuid));
	}

	/** Convenience: ball center as Minecraft coordinates. */
	public net.minecraft.world.phys.Vec3 ballCenter() {
		Vec3 c = state == null ? currentCenter() : state.position();
		return new net.minecraft.world.phys.Vec3(c.x(), c.y(), c.z());
	}

	// ------------------------------------------------------------------
	// Internals
	// ------------------------------------------------------------------

	private void initializeState() {
		Vec3 center = currentCenter();
		if (savedResting) {
			state = BallState.atRest(center);
			LOGGER.info("Golf ball {} loaded at {} (resting)", getId(), fmt(center));
		} else {
			state = new BallState(center, savedVelocity, savedGrounded, false);
			if (state.velocity().length() > 0.0) {
				LOGGER.info("Golf ball {} loaded at {} moving v={}",
						getId(), fmt(center), fmt(state.velocity()));
			} else if (freshSpawn) {
				LOGGER.info("Golf ball {} spawned at {}; dropping under gravity",
						getId(), fmt(center));
			}
		}
		initialized = true;
	}

	private Vec3 currentCenter() {
		net.minecraft.world.phys.Vec3 pos = position();
		return Vec3.of(pos.x, pos.y, pos.z);
	}

	private void applyPosition(Vec3 center) {
		setPos(center.x(), center.y(), center.z());
	}

	private static String fmt(Vec3 v) {
		return String.format("(%.2f, %.2f, %.2f)", v.x(), v.y(), v.z());
	}

	// ------------------------------------------------------------------
	// Save / load. readAdditionalSaveData runs before the first tick but the
	// entity position may not be settled yet, so we only store flags here and
	// anchor the full BallState to the entity's position in initializeState().
	// ------------------------------------------------------------------

	private Vec3 savedVelocity = Vec3.ZERO;
	private boolean savedGrounded;
	private boolean savedResting;
	private boolean freshSpawn = true;

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		freshSpawn = false;
		savedVelocity = Vec3.of(
				input.getDoubleOr(NBT_VX, 0.0),
				input.getDoubleOr(NBT_VY, 0.0),
				input.getDoubleOr(NBT_VZ, 0.0));
		savedGrounded = input.getBooleanOr(NBT_GROUNDED, false);
		savedResting = input.getBooleanOr(NBT_RESTING, false);
		String owner = input.getStringOr(NBT_OWNER, "");
		ownerUuid = (owner == null || owner.isEmpty()) ? null : java.util.UUID.fromString(owner);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		// If the ball has not ticked yet (e.g. it was saved in a chunk that is
		// not currently ticking), persist it as freshly airborne at its current
		// position so it resumes falling on load instead of freezing mid-air.
		if (state == null) {
			state = new BallState(currentCenter(), Vec3.ZERO, false, false);
		}
		Vec3 v = state.velocity();
		output.putDouble(NBT_VX, v.x());
		output.putDouble(NBT_VY, v.y());
		output.putDouble(NBT_VZ, v.z());
		output.putBoolean(NBT_GROUNDED, state.grounded());
		output.putBoolean(NBT_RESTING, state.resting());
		output.putString(NBT_OWNER, ownerUuid == null ? "" : ownerUuid.toString());
	}
}
