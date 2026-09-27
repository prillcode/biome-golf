package pro.apdev.biomegolf.entity;

import org.slf4j.Logger;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.ball.BallPhysics;
import pro.apdev.biomegolf.ball.BallState;
import pro.apdev.biomegolf.ball.PhysicsConfig;
import pro.apdev.biomegolf.ball.ShotPhysicsProfile;
import pro.apdev.biomegolf.ball.ShotDistanceTracker;
import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.server.ActiveHoleService;
import pro.apdev.biomegolf.server.BallCameraService;
import pro.apdev.biomegolf.surface.SurfaceDefinition;
import pro.apdev.biomegolf.world.GolfBlockSurfaceResolver;
import pro.apdev.biomegolf.world.MinecraftBallCollisionWorld;

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
	private static final String NBT_PRACTICE = "golf_practice";
	private static final String NBT_LANDING_RETENTION = "golf_landing_retention";
	private static final String NBT_ROLLING_MULTIPLIER = "golf_rolling_multiplier";
	private static final String NBT_SHOT_DISTANCE = "golf_shot_distance";

	/** M10 probe: tag marking a mirrored vanilla visual entity. */
	private static final String BALL_VISUAL_TAG = "minecraft_golf_ball_visual";
	private static final String NBT_VISUAL_UUID = "golf_visual_uuid";

	/**
	 * M10.3 S1: the mirror only needs to exist while a client-light player is close
	 * enough to track the ball. This is deliberately generous (vanilla item-entity
	 * tracking sits well inside it) so the proxy never disappears while a Bedrock client
	 * can still see it.
	 */
	private static final double MIRROR_TRACKING_RANGE_BLOCKS = 64.0;

	/**
	 * Owner player UUID or {@code null} while the ball is unclaimed.
	 * A player can only strike a ball they own (M2, ARCH §12); a resting ball is
	 * claimed by whoever first launches it. Persisted to NBT between loads.
	 */
	private java.util.UUID ownerUuid;
	private boolean practiceBall;

	private final GolfBlockSurfaceResolver surfaceResolver = new GolfBlockSurfaceResolver();
	private final MinecraftBallCollisionWorld collisionWorld;

	/** Authoritative state; null until the first server tick. */
	private BallState state;
	private ShotPhysicsProfile shotProfile = ShotPhysicsProfile.STANDARD;
	private final ShotDistanceTracker shotDistance = new ShotDistanceTracker();

	/** Whether the state has been anchored to the entity's current position. */
	private boolean initialized;

	/** M10 probe: vanilla mirror entity (a dropped snowball) for clients without the mod. */
	private ItemEntity visual;
	private java.util.UUID visualUuid;

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

		// M10 probe: keep the vanilla mirror in place even while the ball rests.
		tickVisual();

		if (state.resting()) {
			// Resting is terminal until launch() is called; no per-tick work.
			return;
		}

		Vec3 previousPosition = state.position();
		BallState next = BallPhysics.step(state, PHYSICS_CONFIG, collisionWorld, shotProfile);
		if (!next.equals(state)) {
			state = next;
			shotDistance.advance(previousPosition, next.position());
			applyPosition(state.position());
		}
		ActiveHoleService.instance().onBallMoved(this, previousPosition, next.position());
		if (state.resting()) {
			entityData.set(DATA_RESTING, true);
			LOGGER.info("Golf ball {} came to rest at {}", getId(), fmt(state.position()));
			// M10.2: hand control back before travel/hole-out changes the view.
			BallCameraService.instance().onBallRest(this);
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
		shotDistance.reset();
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
		shotDistance.reset();
		initialized = true;
		entityData.set(DATA_RESTING, true);
		applyPosition(position);
		BallCameraService.instance().onBallRest(this);
		LOGGER.info("Golf ball {} placed at rest at {}", getId(), fmt(position));
	}

	/** Current authoritative state, or null before the first server tick. */
	public BallState ballState() {
		return state;
	}

	/** Horizontal distance traveled by the current/most recent shot, in blocks. */
	public int shotDistanceBlocks() {
		return shotDistance.roundedBlocks();
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

	/** Marks this owned ball as a practice ball. Server-side only. */
	public void markAsPracticeBall() {
		if (!level().isClientSide()) {
			practiceBall = true;
		}
	}

	/** Whether this ball was explicitly created for practice. */
	public boolean isPracticeBall() {
		return practiceBall;
	}

	/** Whether the given player may strike this ball (owns it, or it is unclaimed). */
	public boolean canBeStruckBy(java.util.UUID playerUuid) {
		return playerUuid != null && (ownerUuid == null || ownerUuid.equals(playerUuid));
	}

	/**
	 * M10.2: vanilla-visible entity a client-light server camera can follow. The
	 * custom ball entity is unknown to Bedrock/Geyser's entity cache, so follow the
	 * vanilla item mirror instead (spawning it now if it has not ticked yet).
	 */
	public Entity cameraTarget() {
		if (level().isClientSide()) {
			return this;
		}
		if (state != null && (visual == null || visual.isRemoved())) {
			tickVisual();
		}
		return visual != null ? visual : this;
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
		shotDistance.setBlocks(input.getDoubleOr(NBT_SHOT_DISTANCE, 0.0));
		shotProfile = new ShotPhysicsProfile(
			input.getDoubleOr(NBT_LANDING_RETENTION, 1.0),
			input.getDoubleOr(NBT_ROLLING_MULTIPLIER, 1.0));
		String owner = input.getStringOr(NBT_OWNER, "");
		ownerUuid = (owner == null || owner.isEmpty()) ? null : java.util.UUID.fromString(owner);
		practiceBall = input.getBooleanOr(NBT_PRACTICE, false);
		String visualId = input.getStringOr(NBT_VISUAL_UUID, "");
		visualUuid = (visualId == null || visualId.isEmpty()) ? null : java.util.UUID.fromString(visualId);
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
		output.putDouble(NBT_SHOT_DISTANCE, shotDistance.blocks());
		output.putString(NBT_OWNER, ownerUuid == null ? "" : ownerUuid.toString());
		output.putBoolean(NBT_PRACTICE, practiceBall);
		output.putString(NBT_VISUAL_UUID, visualUuid == null ? "" : visualUuid.toString());
	}

	// ------------------------------------------------------------------
	// M10 ball-visual probe: mirror the authoritative ball with a vanilla item
	// entity so clients that cannot resolve the custom golf-ball entity (vanilla
	// Java, and Bedrock through Geyser) still see it. Server-only presentation;
	// the logical ball remains the single source of truth.
	// ------------------------------------------------------------------

	private void tickVisual() {
		ServerLevel serverLevel = (ServerLevel) level();
		Vec3 p = state.position();
		// M10.3 S1: only mirror while a client-light player is near the ball. With no
		// such player online/near (the normal all-modded-Java world) no mirror exists,
		// so modded Java sees only the custom golf ball.
		if (!shouldMirror(serverLevel)) {
			if (visual != null) {
				visual.discard();
				visual = null;
				visualUuid = null;
			}
			return;
		}
		if (visual == null || visual.isRemoved()) {
			visual = null;
			if (visualUuid != null) {
				Entity existing = serverLevel.getEntity(visualUuid);
				if (existing instanceof ItemEntity item && !item.isRemoved()) {
					visual = item;
				}
			}
			if (visual == null) {
				ItemEntity item = new ItemEntity(serverLevel, p.x(), p.y(), p.z(),
						new ItemStack(Items.SNOWBALL));
				item.setNoGravity(true);
				item.setPickUpDelay(32767);
				item.setUnlimitedLifetime();
				item.setInvulnerable(true);
				item.addTag(BALL_VISUAL_TAG);
				serverLevel.addFreshEntity(item);
				visual = item;
				visualUuid = item.getUUID();
			}
		}
		visual.setPos(p.x(), p.y(), p.z());
		visual.setDeltaMovement(0.0, 0.0, 0.0);
	}

	/**
	 * M10.3 S1: whether a client that cannot resolve the custom ball is online and within
	 * mirror tracking range in this dimension. Per-player hiding is not a vanilla
	 * primitive, so this keep-alive is the Tier 1 gate; a mixed session may briefly show
	 * the proxy to modded Java, but the default all-Java experience no longer does.
	 */
	private boolean shouldMirror(ServerLevel level) {
		MinecraftServer server = level.getServer();
		if (server == null) {
			return false;
		}
		Vec3 p = state.position();
		double rangeSq = MIRROR_TRACKING_RANGE_BLOCKS * MIRROR_TRACKING_RANGE_BLOCKS;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!BallCameraService.isClientLight(player)) {
				continue;
			}
			if (!player.level().dimension().equals(level.dimension())) {
				continue;
			}
			double dx = player.getX() - p.x();
			double dz = player.getZ() - p.z();
			if (dx * dx + dz * dz <= rangeSq) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!level().isClientSide()) {
			BallCameraService.instance().onBallRest(this);
			if (visual != null) {
				visual.discard();
				visual = null;
			}
		}
		super.remove(reason);
	}
}
