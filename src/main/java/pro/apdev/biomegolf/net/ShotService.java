package pro.apdev.biomegolf.net;

import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.ball.PhysicsConfig;
import pro.apdev.biomegolf.ball.ShotPhysicsProfile;
import pro.apdev.biomegolf.club.BallLie;
import pro.apdev.biomegolf.club.ClubDefinition;
import pro.apdev.biomegolf.club.LieRules;
import pro.apdev.biomegolf.club.ShotResolver;
import pro.apdev.biomegolf.club.ShotType;
import pro.apdev.biomegolf.entity.GolfBallEntity;
import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.item.GolfItems;
import pro.apdev.biomegolf.server.ActiveHoleService;
import pro.apdev.biomegolf.server.ActiveHoleService.ShotPermission;
import pro.apdev.biomegolf.server.PracticeRangeService;
import pro.apdev.biomegolf.server.VisitorService;
import pro.apdev.biomegolf.surface.SurfaceDefinition;

/**
 * Server-side authoritative shot execution (M3, ARCH §8.2/§16): validates a
 * finalized shot request against the acting {@link ServerPlayer} and, if legal,
 * resolves and launches the ball through the equipped club + power/accuracy.
 * Shared by the networking receiver so there is exactly one place a shot can be
 * committed — a repeatable input yields a repeatable launch (no per-client
 * outcome control).
 */
public final class ShotService {

	private static final double MAX_STRIKE_DISTANCE_SQ = 6.0 * 6.0;

	private ShotService() {
	}

	/**
	 * Attempts a shot by {@code player} on the ball {@code ballId}.
	 *
	 * @return the {@link ShotOutcome}; {@code SUCCESS} when a launch occurred
	 */
	public static ShotOutcome attempt(ServerPlayer player, int ballId,
			float aimYawDeg, float aimPitchDeg, float power, float accuracy, ShotType requestedType) {
		if (VisitorService.isVisitor(player)) {
			return ShotOutcome.VISITOR;
		}
		ServerLevel level = (ServerLevel) player.level();
		GolfBallEntity ball = findBall(level, ballId);
		if (ball == null) {
			return ShotOutcome.BALL_NOT_FOUND;
		}

		UUID me = player.getUUID();
		if (ball.owner() != null && !ball.owner().equals(me)) {
			return ShotOutcome.NOT_YOUR_BALL;
		}
		if (!ball.isResting()) {
			return ShotOutcome.BALL_MOVING;
		}
		if (player.distanceToSqr(ball) > MAX_STRIKE_DISTANCE_SQ) {
			return ShotOutcome.BALL_TOO_FAR;
		}

		ShotPermission holePermission = ActiveHoleService.instance().shotPermission(player, ball);
		if (holePermission == ShotPermission.WRONG_BALL) {
			return ShotOutcome.NOT_ACTIVE_BALL;
		}
		if (holePermission == ShotPermission.MISSING_BALL) {
			return ShotOutcome.MISSING_ACTIVE_BALL;
		}
		if (holePermission == ShotPermission.HOLE_COMPLETE) {
			return ShotOutcome.HOLE_COMPLETE;
		}

		ClubDefinition club = heldClub(player);
		if (club == null) {
			return ShotOutcome.NO_CLUB;
		}
		ShotType shotType = requestedType == null ? ShotType.STANDARD : requestedType;
		if (!shotType.allowedFor(club)) {
			return ShotOutcome.INVALID_SHOT_TYPE;
		}
		SurfaceDefinition surface = ball.currentSurface();
		if (isDriver(club) && isSand(surface)) {
			return ShotOutcome.DRIVER_NOT_ALLOWED_ON_SAND;
		}

		double maxSpeed = PhysicsConfig.DEFAULT.maxLaunchSpeed();
		// Defensive clamps happen inside the resolver; power/accuracy sent here may
		// already be client-clamped but must never be trusted raw.
		double p = ShotResolver.legalPower(power);
		double a = ShotResolver.legalAccuracy(accuracy);

		// Lie context (M8.12): a Driver away from any tee loses its tee-only flight.
		BallLie lie = resolveLie(player, ball, level);
		ShotPhysicsProfile profile = shotType.profile(club);
		if (LieRules.penalizes(club, lie)) {
			profile = LieRules.applyTo(profile, lie, shotType);
		}
		Vec3 velocity = ShotResolver.initialVelocity(club, aimYawDeg, aimPitchDeg, p, a, maxSpeed,
			profile, LieRules.accuracySpread(lie));
		if (velocity == null) {
			return ShotOutcome.AIM_NOT_LEGAL;
		}
		velocity = applySurfaceShotPower(velocity, surface);

		// Claim on first strike; launch is the single server-authoritative mutation.
		if (ball.owner() == null) {
			ball.setOwner(me);
		}
		Vec3 shotOrigin = ball.ballState() == null
				? new Vec3(ball.position().x, ball.position().y, ball.position().z)
				: ball.ballState().position();
		ball.launch(velocity, profile);
		if (holePermission == ShotPermission.SCORING) {
			ActiveHoleService.instance().recordAcceptedShot(player, ball, shotOrigin);
		}
		MinecraftGolf.LOGGER.info("{} shot via {} power={} acc={} lie={} (wind-free) launched v={}",
				player.getName().getString(), club.id() + "/" + shotType, p, a, lie, velocity);
		return ShotOutcome.SUCCESS;
	}

	/**
	 * Derives the authoritative lie from the ball's resting position against the
	 * player's active-hole tee and the practice-range tee in the same dimension. An
	 * unknown context yields {@link BallLie#TEE}, so the deck penalty is never applied
	 * without a tee reference. Explicitly marked practice balls always resolve as
	 * {@link BallLie#TEE}, so a builder testing distances anywhere in a course gets the
	 * Driver's full flight while scored golf keeps the deck penalty.
	 */
	public static BallLie resolveLie(ServerPlayer player, GolfBallEntity ball, ServerLevel level) {
		Vec3 position = ballPosition(ball);
		String dimensionId = level.dimension().identifier().toString();
		java.util.List<Vec3> anchors = new java.util.ArrayList<>(2);
		ActiveHoleService.instance().state(player.getUUID())
			.map(state -> state.hole())
			.filter(hole -> dimensionId.equals(hole.dimension()))
			.ifPresent(hole -> anchors.add(hole.tee()));
		PracticeRangeService.Location practiceTee = PracticeRangeService.instance().tee();
		if (practiceTee != null && dimensionId.equals(practiceTee.dimension())) {
			anchors.add(practiceTee.position());
		}
		return LieRules.forShot(LieRules.classify(position, anchors), ball.isPracticeBall());
	}

	static Vec3 ballPosition(GolfBallEntity ball) {
		if (ball.ballState() != null) {
			return ball.ballState().position();
		}
		return new Vec3(ball.position().x, ball.position().y, ball.position().z);
	}

	/**
	 * Vanilla-compatible shot entry point (M10.1): aims along the player's current
	 * look direction and strikes their nearest resting ball. Used by
	 * {@code /golf swing} (and, later, a held-use interaction) so a client without
	 * the mod can still play. Validation is unchanged: everything funnels through
	 * {@link #attempt}.
	 */
	public static ShotOutcome attemptNearest(ServerPlayer player, float power, float accuracy,
			ShotType shotType) {
		GolfBallEntity ball = nearestStrikeableBall(player);
		if (ball == null) {
			return ShotOutcome.BALL_NOT_FOUND;
		}
		return attempt(player, ball.getId(), player.getYRot(), player.getXRot(), power, accuracy, shotType);
	}

	/**
	 * Nearest resting ball the player may strike, preferring the player's own ball
	 * over an unowned one. Distance is intentionally not filtered here so
	 * {@link #attempt} can report {@link ShotOutcome#BALL_TOO_FAR} rather than a
	 * misleading {@code BALL_NOT_FOUND}.
	 */
	public static GolfBallEntity nearestStrikeableBall(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		UUID me = player.getUUID();
		GolfBallEntity owned = null;
		double ownedDistance = Double.MAX_VALUE;
		GolfBallEntity unowned = null;
		double unownedDistance = Double.MAX_VALUE;
		List<? extends GolfBallEntity> balls =
				level.getEntities(EntityTypeTest.forClass(GolfBallEntity.class), ignored -> true);
		for (GolfBallEntity ball : balls) {
			if (!ball.isResting()) {
				continue;
			}
			double distance = player.distanceToSqr(ball);
			if (me.equals(ball.owner())) {
				if (distance < ownedDistance) {
					owned = ball;
					ownedDistance = distance;
				}
			} else if (ball.owner() == null && distance < unownedDistance) {
				unowned = ball;
				unownedDistance = distance;
			}
		}
		return owned != null ? owned : unowned;
	}

	static boolean isDriver(ClubDefinition club) {
		return "driver".equals(club.id());
	}

	static boolean isSand(SurfaceDefinition surface) {
		return SurfaceDefinition.SAND.id().equals(surface.id());
	}

	static Vec3 applySurfaceShotPower(Vec3 velocity, SurfaceDefinition surface) {
		return velocity.scale(surface.shotPowerMultiplier());
	}

	private static GolfBallEntity findBall(ServerLevel level, int id) {
		List<? extends GolfBallEntity> balls =
				level.getEntities(EntityTypeTest.forClass(GolfBallEntity.class), ignored -> true);
		for (GolfBallEntity ball : balls) {
			if (ball.getId() == id) {
				return ball;
			}
		}
		return null;
	}

	private static ClubDefinition heldClub(Player player) {
		// Recognises both the custom item and the M10 vanilla fallback representation.
		return GolfItems.clubOf(player.getMainHandItem());
	}
}
