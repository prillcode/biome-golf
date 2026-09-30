package pro.apdev.biomegolf.club;

import java.util.List;
import java.util.Objects;

import pro.apdev.biomegolf.ball.ShotPhysicsProfile;
import pro.apdev.biomegolf.golf.Vec3;

/**
 * Minecraft-free lie classification and Driver deck-penalty data (M8.12).
 *
 * <p>The exploit this closes: a Driver hit from the fairway behaved exactly like a
 * teed Driver, so long par 5s were reachable with a free full-power bomb. A ball at
 * rest inside a tee volume is {@link BallLie#TEE}; anywhere else is
 * {@link BallLie#DECK}, and a decked Driver gets a lower, shorter, slightly less
 * accurate flight. The Standard deck Driver also gets increased landing retention
 * for a low runner; its subsequent rolling friction stays on the Standard profile.
 * Explicit Stinger behavior remains unchanged.</p>
 *
 * <p>When no tee anchor is known (for example practice with no range tee), the ball
 * is treated as teed so the penalty is never applied without a reference point.
 * An explicitly marked practice ball also always plays the tee profile, so a builder
 * testing distances anywhere in a course gets the Driver's true flight (M8.15).</p>
 */
public final class LieRules {

	/** Horizontal radius (blocks) around a tee anchor that counts as a tee lie. */
	public static final double TEE_LIE_RADIUS = 8.0;

	/** Vertical tolerance (blocks) around a tee anchor that counts as a tee lie. */
	public static final double TEE_LIE_VERTICAL_TOLERANCE = 4.0;

	/** Decked-Driver launch scaling applied on top of the shot-type launch profile. */
	public static final double DECK_LAUNCH_HORIZONTAL = 0.90;
	public static final double DECK_LAUNCH_VERTICAL = 0.75;

	/** Standard deck Driver's runner landing retention; Stinger is 0.60. */
	public static final double DECK_STANDARD_LANDING_RETENTION = 0.40;

	/** Decked-Driver accuracy fan multiplier (harder, without touching the meter). */
	public static final double DECK_ACCURACY_SPREAD = 1.5;

	/** HUD-only estimate of the decked Driver's remaining carry fraction. */
	public static final double DECK_DISPLAY_CARRY_FACTOR = 0.75;

	private LieRules() {
	}

	/** Convenience for a single anchor; {@code null} means "no tee reference". */
	public static BallLie classify(Vec3 ball, Vec3 teeAnchor) {
		return classify(ball, teeAnchor == null ? List.of() : List.of(teeAnchor));
	}

	/**
	 * Classifies a resting ball against every known tee anchor. An empty anchor list
	 * yields {@link BallLie#TEE} so an unknown context never penalizes a shot.
	 */
	public static BallLie classify(Vec3 ball, List<Vec3> teeAnchors) {
		Objects.requireNonNull(ball, "ball");
		Objects.requireNonNull(teeAnchors, "teeAnchors");
		for (Vec3 anchor : teeAnchors) {
			if (anchor == null) {
				continue;
			}
			double dx = ball.x() - anchor.x();
			double dz = ball.z() - anchor.z();
			if (Math.hypot(dx, dz) <= TEE_LIE_RADIUS
					&& Math.abs(ball.y() - anchor.y()) <= TEE_LIE_VERTICAL_TOLERANCE) {
				return BallLie.TEE;
			}
		}
		return teeAnchors.isEmpty() ? BallLie.TEE : BallLie.DECK;
	}

	/** True when this lie penalizes the given club's tee-optimized flight. */
	public static boolean penalizes(ClubDefinition club, BallLie lie) {
		return lie == BallLie.DECK && club != null && "driver".equals(club.id());
	}

	/** Applies the deck Driver penalty to the selected shot profile. */
	public static ShotPhysicsProfile applyTo(ShotPhysicsProfile profile, BallLie lie, ShotType shotType) {
		Objects.requireNonNull(profile, "profile");
		Objects.requireNonNull(lie, "lie");
		Objects.requireNonNull(shotType, "shotType");
		if (lie != BallLie.DECK) return profile;

		ShotPhysicsProfile deckProfile = profile.withLaunchScaled(
			DECK_LAUNCH_HORIZONTAL, DECK_LAUNCH_VERTICAL);
		if (shotType == ShotType.STANDARD) {
			// Give the low Standard flight a runner-style landing without borrowing
			// Stinger's stronger rolling brake; keep the selected shot-type behavior.
			deckProfile = deckProfile.withLandingHorizontalRetention(
				DECK_STANDARD_LANDING_RETENTION);
		}
		return deckProfile;
	}

	/**
	 * Effective lie for a shot. An explicitly marked practice ball always plays the
	 * full tee profile, so a builder testing distances anywhere in a course gets the
	 * Driver's true flight; scored golf keeps the resolved lie.
	 */
	public static BallLie forShot(BallLie resolvedLie, boolean practiceBall) {
		return practiceBall ? BallLie.TEE : resolvedLie;
	}

	/** Accuracy fan multiplier for a lie ({@code 1.0} when neutral). */
	public static double accuracySpread(BallLie lie) {
		return lie == BallLie.DECK ? DECK_ACCURACY_SPREAD : 1.0;
	}

	/** Display-only carry fraction so the HUD can show effective distance. */
	public static double displayCarryFactor(ClubDefinition club, BallLie lie) {
		return penalizes(club, lie) ? DECK_DISPLAY_CARRY_FACTOR : 1.0;
	}
}
