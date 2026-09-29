package pro.apdev.biomegolf.club;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.ball.PhysicsConfig;
import pro.apdev.biomegolf.golf.Vec3;

/**
 * Headless contract for the M3 deterministic shot calculation: partial power
 * and accuracy-driven lateral deviation. Pure and repeatable (PRD §5, ARCH §8.3).
 */
class ShotMeterTest {

	private static final double MAX_SPEED = PhysicsConfig.DEFAULT.maxLaunchSpeed();
	private static final double TOL = 1e-9;

	// Facing south (+Z), yaw 0.

	private static Vec3 resolve(ClubDefinition c, double power, double accuracy) {
		return ShotResolver.initialVelocity(c, 0.0, 0.0, power, accuracy, MAX_SPEED);
	}

	@Test
	void perfectAccuracyIsAlongAim() {
		// Perfect accuracy at any power keeps the ball on the aim line (x ~ 0).
		for (double power : new double[] { 0.25, 0.6, 1.0 }) {
			Vec3 v = resolve(GolfClubs.DRIVER, power, ShotResolver.PERFECT_ACCURACY);
			assertNotNull(v);
			assertEquals(0.0, v.x(), TOL, "perfect center should have no lateral x at power " + power);
			assertTrue(v.z() > 0.0, "should travel forward (+Z)");
			assertTrue(v.y() > 0.0, "driver should still launch up");
		}
	}

	@Test
	void partialPowerReducesTotalSpeedProportionally() {
		Vec3 full = resolve(GolfClubs.DRIVER, 1.0, 0.5);
		Vec3 half = resolve(GolfClubs.DRIVER, 0.5, 0.5);
		assertTrue(half.length() < full.length(), "half power must be slower");
		assertEquals(0.5 * full.length(), half.length(), TOL, "power scales speed linearly");
	}

	@Test
	void partialPowerKeepsClubLoftTrajectory() {
		ClubDefinition c = GolfClubs.WEDGE;
		Vec3 full = resolve(c, 1.0, 0.5);
		Vec3 chip = resolve(c, 0.3, 0.5);
		double launchFull = Math.toDegrees(Math.atan2(full.y(), full.horizontalLength()));
		double launchChip = Math.toDegrees(Math.atan2(chip.y(), chip.horizontalLength()));
		assertEquals(launchFull, launchChip, 1e-6, "loft shape preserved at partial power");
	}

	@Test
	void accuracyMissDeviationIsMonotonicAndSigned() {
		// Facing south: accuracy > 0.5 deviates to the player's right (-X),
		// accuracy < 0.5 to the left (+X); farther from 0.5 => bigger dev.
		Vec3 left = resolve(GolfClubs.DRIVER, 1.0, 0.2);   // misses low-side
		Vec3 perfect = resolve(GolfClubs.DRIVER, 1.0, 0.5);
		Vec3 right = resolve(GolfClubs.DRIVER, 1.0, 0.8);  // misses high-side
		assertTrue(perfect.x() == 0.0, "perfect centered");
		assertTrue(left.x() > 0.0, "accuracy 0.2 should drift left (+X): " + left.x());
		assertTrue(right.x() < 0.0, "accuracy 0.8 should drift right (-X): " + right.x());
		// A full miss fans further than a gentle miss.
		Vec3 nearPerfect = resolve(GolfClubs.DRIVER, 1.0, 0.56);
		assertTrue(Math.abs(left.x()) > Math.abs(nearPerfect.x()),
				"full miss fans more than a tiny miss");
	}

	@Test
	void perfectBandProducesZeroDeviationForEveryClub() {
		double band = SwingMeter.PERFECT_BAND;
		for (ClubDefinition club : GolfClubs.ALL) {
			assertEquals(0.0, resolve(club, 1.0, 0.5).x(), TOL,
				club.id() + " centre must be exactly straight");
			assertEquals(0.0, resolve(club, 1.0, 0.5 - band).x(), TOL,
				club.id() + " inside band must be straight");
			assertEquals(0.0, resolve(club, 1.0, 0.5 + band).x(), TOL,
				club.id() + " inside band must be straight");
		}
	}

	@Test
	void missOutsideBandFansAndFullMissFanIsUnchanged() {
		double band = SwingMeter.PERFECT_BAND;
		Vec3 justOutside = resolve(GolfClubs.DRIVER, 1.0, 0.5 + band + 0.02);
		assertTrue(justOutside.x() < 0.0, "a click outside the band should fan right");

		Vec3 fullMiss = resolve(GolfClubs.DRIVER, 1.0, 0.0);
		double expectedDeg = ShotResolver.FULL_MISS_FAN_DEG
			/ (1.0 + GolfClubs.DRIVER.accuracySensitivity());
		double actualDeg = Math.abs(Math.toDegrees(Math.atan2(fullMiss.x(), fullMiss.z())));
		assertEquals(expectedDeg, actualDeg, 1e-6,
			"the band remap must preserve the existing full-miss fan");
	}

	@Test
	void zeroPowerIsStationary() {
		Vec3 v = resolve(GolfClubs.DRIVER, 0.0, 0.5);
		assertNotNull(v);
		assertEquals(0.0, v.length(), TOL, "zero power should not move the ball");
	}

	@Test
	void outOfRangeAndNaNInputsAreClampedNotThrown() {
		Vec3 over = resolve(GolfClubs.DRIVER, 3.0, 0.5);
		assertNotNull(over);
		assertTrue(over.length() <= MAX_SPEED + TOL, "power clamped to maxSpeed");
		assertNotNull(resolve(GolfClubs.DRIVER, -1.0, 0.5));
		assertNotNull(resolve(GolfClubs.DRIVER, 0.5, 9.0)); // accuracy clamps
		Vec3 nanPower = resolve(GolfClubs.DRIVER, Double.NaN, 0.5);
		assertNotNull(nanPower);
		assertEquals(0.0, nanPower.length(), TOL, "NaN power treated as zero");
	}

	@Test
	void repeatableInputIsRepeatable() {
		Vec3 a = resolve(GolfClubs.LONG_IRON, 0.7, 0.2);
		Vec3 b = resolve(GolfClubs.LONG_IRON, 0.7, 0.2);
		assertEquals(a, b);
	}

	@Test
	void putterStaysLowAtAnyLegalPower() {
		for (double power : new double[] { 0.4, 1.0 }) {
			Vec3 v = resolve(GolfClubs.PUTTER, power, 0.5);
			assertNotNull(v);
			assertTrue(v.y() < v.horizontalLength() * 0.1, "putter stays rolling, not lofty");
		}
	}
}
