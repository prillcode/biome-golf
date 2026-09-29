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
 * accurate flight. Only the launch profile changes: landing and rolling behavior
 * keep coming from the shot type, so the lie composes with Standard/Stinger/etc.</p>
 *
 * <p>When no tee anchor is known (for example practice with no range tee), the ball
 * is treated as teed so the penalty is never applied without a reference point.</p>
 */
public final class LieRules {

	/** Horizontal radius (blocks) around a tee anchor that counts as a tee lie. */
	public static final double TEE_LIE_RADIUS = 8.0;

	/** Vertical tolerance (blocks) around a tee anchor that counts as a tee lie. */
	public static final double TEE_LIE_VERTICAL_TOLERANCE = 4.0;

	/** Decked-Driver launch scaling applied on top of the shot-type launch profile. */
	public static final double DECK_LAUNCH_HORIZONTAL = 0.90;
	public static final double DECK_LAUNCH_VERTICAL = 0.75;

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

	/** Composes the lie onto a shot-type launch profile (unpenalized lies pass through). */
	public static ShotPhysicsProfile applyTo(ShotPhysicsProfile profile, BallLie lie) {
		Objects.requireNonNull(profile, "profile");
		Objects.requireNonNull(lie, "lie");
		return lie == BallLie.DECK
			? profile.withLaunchScaled(DECK_LAUNCH_HORIZONTAL, DECK_LAUNCH_VERTICAL)
			: profile;
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
