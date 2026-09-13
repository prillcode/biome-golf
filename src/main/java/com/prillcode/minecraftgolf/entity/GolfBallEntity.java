package com.prillcode.minecraftgolf.entity;

import org.slf4j.Logger;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
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
import com.prillcode.minecraftgolf.ball.ShotPhysicsProfile;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.server.ActiveHoleService;
import com.prillcode.minecraftgolf.surface.SurfaceDefinition;
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

	private static final EntityDataAccessor<Boolean> DATA_RESTING =
			SynchedEntityData.defineId(GolfBallEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<String> DATA_OWNER =
			SynchedEntityData.defineId(GolfBallEntity.class, EntityDataSerializers.STRING);

	private static final String NBT_VX = "golf_vx";
	private static final String NBT_VY = "golf_vy";
	private static final String NBT_VZ = "golf_vz";
	private static final String NBT_GROUNDED = "golf_grounded";
	private static final String NBT_RESTING = "golf_resting";
	private static final String NBT_OWNER = "golf_owner"; // owner player UUID, or "" when unowned
	private static final String NBT_LANDING_RETENTION = "golf_landing_retention";
	private static final String NBT_ROLLING_MULTIPLIER = "golf_rolling_multiplier";

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
	private ShotPhysicsProfile shotProfile = ShotPhysicsProfile.STANDARD;

	/** Whether the state has been anchored to the entity's current position. */
	private boolean initialized;

	public GolfBallEntity(EntityType<? extends GolfBallEntity> type, Level level) {
		super(type, level);
		this.collisionWorld = new MinecraftBallCollisionWorld(level, this, BALL_RADIUS, surfaceResolver);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		// Position remains tracker-driven. These two read-only client hints let the
		// swing UI select a legal candidate; the server still re-validates both.
		builder.define(DATA_RESTING, false);
		builder.define(DATA_OWNER, "");
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

		Vec3 previousPosition = state.position();
		BallState next = BallPhysics.step(state, PHYSICS_CONFIG, collisionWorld, shotProfile);
		if (!next.equals(state)) {
			state = next;
			applyPosition(state.position());
		}
		ActiveHoleService.instance().onBallMoved(this, previousPosition, next.position());
		if (state.resting()) {
			entityData.set(DATA_RESTING, true);
			LOGGER.info("Golf ball {} came to rest at {}", getId(), fmt(state.position()));
			// Penalty recovery and hole-out may replace state during onBallMoved;
			// travel only when physics itself brought the active ball to rest.
			if (state == next) {
				ActiveHoleService.instance().onBallCameToRest(this);
			}
		}
	}

	/**
	 * Launches the ball from its current position with a clamped velocity.
	 * Server-side only; called by the dev launch controls (slice S03) and by
	 * future shot execution.
	 */
	public void launch(Vec3 velocity) {
		launch(velocity, ShotPhysicsProfile.STANDARD);
	}

	/** Launches with server-selected landing behavior for the struck club. */
	public void launch(Vec3 velocity, ShotPhysicsProfile profile) {
		if (level().isClientSide()) {
			return;
		}
		this.shotProfile = java.util.Objects.requireNonNull(profile, "profile");
		if (state == null) {
			state = BallState.atRest(currentCenter());
		}
		Vec3 clamped = BallPhysics.clampLaunch(velocity, PHYSICS_CONFIG);
		state = BallState.launched(state.position(), clamped);
		entityData.set(DATA_RESTING, false);
		applyPosition(state.position());
		LOGGER.info("Golf ball {} launched at {} v={}", getId(), fmt(state.position()), fmt(state.velocity()));
	}

	/** Places the server-owned ball at rest, used by tee and penalty recovery. */
	public void placeAtRest(Vec3 position) {
		if (level().isClientSide()) {
			return;
		}
		state = BallState.atRest(position);
		initialized = true;
		entityData.set(DATA_RESTING, true);
		applyPosition(position);
		LOGGER.info("Golf ball {} placed at rest at {}", getId(), fmt(position));
	}

	/** Current authoritative state, or null before the first server tick. */
	public BallState ballState() {
		return state;
	}

	public boolean isResting() {
		return level().isClientSide()
				? entityData.get(DATA_RESTING)
				: state != null && state.resting();
	}

	/** Resolves the authoritative support surface at the ball's current position. */
	public SurfaceDefinition currentSurface() {
		Vec3 position = state == null ? currentCenter() : state.position();
		return collisionWorld.surfaceAt(position);
	}

	/** Owner player UUID, or {@code null} when the ball is unclaimed. */
	public java.util.UUID owner() {
		if (!level().isClientSide()) {
			return ownerUuid;
		}
		String encoded = entityData.get(DATA_OWNER);
		if (encoded.isEmpty()) {
			return null;
		}
		try {
			return java.util.UUID.fromString(encoded);
		} catch (IllegalArgumentException malformed) {
			return null;
		}
	}

	/** Claims (or reassigns) the ball to a player. Server-side only. */
	public void setOwner(java.util.UUID playerUuid) {
		if (level().isClientSide()) {
			return;
		}
		this.ownerUuid = playerUuid;
		entityData.set(DATA_OWNER, playerUuid == null ? "" : playerUuid.toString());
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
		entityData.set(DATA_RESTING, state.resting());
		entityData.set(DATA_OWNER, ownerUuid == null ? "" : ownerUuid.toString());
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
		shotProfile = new ShotPhysicsProfile(
			input.getDoubleOr(NBT_LANDING_RETENTION, 1.0),
			input.getDoubleOr(NBT_ROLLING_MULTIPLIER, 1.0));
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
		output.putDouble(NBT_LANDING_RETENTION, shotProfile.landingHorizontalRetention());
		output.putDouble(NBT_ROLLING_MULTIPLIER, shotProfile.rollingFrictionMultiplier());
		output.putString(NBT_OWNER, ownerUuid == null ? "" : ownerUuid.toString());
	}
}
