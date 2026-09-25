package pro.apdev.biomegolf.club;

import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.ball.ShotPhysicsProfile;

/**
 * Resolves a shot's initial velocity from a club, an aim direction, and the
 * three-click result (power + accuracy) (ARCHITECTURE.md §8, §12; PRD §5).
 *
 * <p>Pure function: identical inputs produce an identical result for a given
 * club+aim+power+accuracy, so it is unit-testable on the plain JVM and is the
 * single server-authoritative resolution point (the M3 meters feed this; the
 * M2 full-power path is the special case power=1, accuracy=perfect).</p>
 *
 * <p>Aim uses Minecraft's rotation convention so the server can feed a player's
 * {@code getYRot()}/{@code getXRot()} straight in:
 * <ul>
 *   <li>{@code aimYawDegrees} — Minecraft yaw; 0 faces +Z (south), increases
 *       clockwise from above (east = -90).</li>
 *   <li>{@code aimPitchDegrees} — Minecraft pitch; 0 level, positive down.</li>
 * </ul>
 * The club's independently tuned horizontal/upward components govern trajectory;
 * display loft is metadata only (putter upward speed remains near zero).
 * {@code aimPitchDegrees &gt; 45} is rejected as a non-legal (staring-at-feet) shot.</p>
 *
 * <p>Power and accuracy model:
 * <ul>
 *   <li>{@code power} (0..1) scales the club's full-power speed. Because a fixed
 *       launch angle is kept regardless of power, a gentle chip keeps its club's
 *       trajectory shape but travels a shorter distance.</li>
 *   <li>{@code accuracy} (0..1, 0.5 = perfect) produces a lateral left/right
 *       deviation of the aim direction. Miss magnitude |2a−1| (0..1) maps to an
 *       angular fan; the spread is tighter for higher {@code accuracySensitivity}
 *       clubs so precise/accurate clubs are less forgiving to a careless click.</li>
 * </ul></p>
 */
public final class ShotResolver {

	/** Perfect accuracy reads 0.5 on the 0..1 accuracy lane. */
	public static final double PERFECT_ACCURACY = 0.5;
	/** Angular half-aperture (both sides of aim) reached by a fully-missed accuracy click (degrees). */
	public static final double FULL_MISS_FAN_DEG = 14.0;

	private ShotResolver() {
	}

	/**
	 * Full-power, perfectly-on-target shot (power 1.0, accuracy exactly 0.5).
	 * Convenience kept for the M2 dev-launch and full-power callers.
	 */
	public static Vec3 initialVelocity(
			ClubDefinition club, double aimYawDegrees, double aimPitchDegrees, double maxSpeed) {
		return initialVelocity(club, aimYawDegrees, aimPitchDegrees, 1.0, PERFECT_ACCURACY, maxSpeed);
	}

	/**
	 * Resolves a shot with an explicit power (0..1) and accuracy (0..1, 0.5 perfect).
	 *
	 * @return the initial velocity, or {@code null} if the aim pitch is non-legal
	 */
	public static Vec3 initialVelocity(
			ClubDefinition club, double aimYawDegrees, double aimPitchDegrees,
			double power, double accuracy, double maxSpeed) {
		return initialVelocity(club, aimYawDegrees, aimPitchDegrees, power, accuracy,
			maxSpeed, ShotPhysicsProfile.STANDARD);
	}

	public static Vec3 initialVelocity(
			ClubDefinition club, double aimYawDegrees, double aimPitchDegrees,
			double power, double accuracy, double maxSpeed, ShotPhysicsProfile profile) {
		if (aimPitchDegrees > 45.0) {
			return null;
		}
		// Clamp the inputs defensively (a malicious/buggy client could send out-of-range).
		power = legalPower(power);
		accuracy = legalAccuracy(accuracy);

		double horizontal = club.fullPowerHorizontalSpeed() * power * profile.launchHorizontalMultiplier();
		double up = club.fullPowerUpwardSpeed() * power * profile.launchVerticalMultiplier();
		double speed = Math.hypot(horizontal, up);
		if (speed > maxSpeed) {
			double clampScale = maxSpeed / speed;
			horizontal *= clampScale;
			up *= clampScale;
		}

		// Lateral angular deviation from the accuracy click. miss magnitude 0..1,
		// 0 = perfect (2a-1 == 0). Full-miss fan is scaled by club forgiveness:
		// higher accuracySensitivity (tight clubs like the putter) => tighter fan.
		double miss = Math.abs(2.0 * accuracy - 1.0);
		double devDeg = miss * FULL_MISS_FAN_DEG / (1.0 + club.accuracySensitivity());
		int side = (accuracy < PERFECT_ACCURACY) ? -1 : 1; // left/right
		double yaw = Math.toRadians(aimYawDegrees + side * devDeg);

		double dirX = -Math.sin(yaw);
		double dirZ = Math.cos(yaw);

		return Vec3.of(dirX * horizontal, up, dirZ * horizontal);
	}

	/** Clamp power into [0,1] (server-side defense before resolution). */
	public static double legalPower(double power) {
		if (Double.isNaN(power)) {
			return 0.0;
		}
		return Math.max(0.0, Math.min(1.0, power));
	}

	/** Clamp accuracy into [0,1]. */
	public static double legalAccuracy(double accuracy) {
		if (Double.isNaN(accuracy)) {
			return PERFECT_ACCURACY;
		}
		return Math.max(0.0, Math.min(1.0, accuracy));
	}
}
