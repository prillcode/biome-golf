package pro.apdev.biomegolf.club;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * M8.11 contract: the triangular meter must sample its perfect/centre value exactly
 * so a dead-centre click can produce zero deviation.
 */
class SwingMeterTest {

	private static final float TOL = 1.0e-6f;

	@Test
	void reachesBothSweepEndpoints() {
		assertEquals(0.0f, SwingMeter.value(0, 16), TOL);
		assertEquals(1.0f, SwingMeter.value(16, 16), TOL);
		assertEquals(0.0f, SwingMeter.value(32, 16), TOL);
	}

	@Test
	void evenSweepSamplesCentreExactly() {
		// The M8.11 defect: an odd half-sweep could never hit 0.5. An even one does.
		assertEquals(SwingMeter.CENTER, SwingMeter.value(8, 16), TOL);
		assertEquals(SwingMeter.CENTER, SwingMeter.value(24, 16), TOL);
		assertEquals(SwingMeter.CENTER, SwingMeter.value(12, 24), TOL);
	}

	@Test
	void powerSweepReachesFullPowerAndCentre() {
		assertEquals(1.0f, SwingMeter.value(SwingMeter.POWER_HALF_SWEEP_TICKS,
			SwingMeter.POWER_HALF_SWEEP_TICKS), TOL);
		assertEquals(SwingMeter.CENTER, SwingMeter.value(
			SwingMeter.POWER_HALF_SWEEP_TICKS / 2, SwingMeter.POWER_HALF_SWEEP_TICKS), TOL);
	}

	@Test
	void shippedAccuracySweepIsEvenAndSamplesCentre() {
		assertTrue(SwingMeter.centerReachable(SwingMeter.ACCURACY_HALF_SWEEP_TICKS),
			"accuracy half-sweep must be even so the perfect value is reachable");
		assertEquals(SwingMeter.CENTER, SwingMeter.value(
			SwingMeter.ACCURACY_HALF_SWEEP_TICKS / 2, SwingMeter.ACCURACY_HALF_SWEEP_TICKS), TOL);
	}

	@Test
	void oddSweepCannotSampleCentre() {
		// Regression guard for the original 15-tick defect.
		assertFalse(SwingMeter.centerReachable(15));
		for (int t = 0; t < 30; t++) {
			assertTrue(Math.abs(SwingMeter.value(t, 15) - SwingMeter.CENTER) > 0.0f,
				"odd sweep must never equal centre at tick " + t);
		}
	}

	@Test
	void sweepIsPeriodicSymmetricAndBounded() {
		for (int t = 0; t < 200; t++) {
			float v = SwingMeter.value(t, 16);
			assertTrue(v >= 0.0f && v <= 1.0f, "value out of range at tick " + t);
			assertEquals(v, SwingMeter.value(t + 32, 16), TOL, "period must be 2 * halfSweep");
			assertEquals(v, SwingMeter.value(-t, 16), TOL, "negative ticks must wrap");
		}
	}

	@Test
	void perfectBandMatchesCentre() {
		assertTrue(SwingMeter.isPerfect(SwingMeter.CENTER));
		assertTrue(SwingMeter.isPerfect((float) (SwingMeter.CENTER + SwingMeter.PERFECT_BAND)));
		assertFalse(SwingMeter.isPerfect((float) (SwingMeter.CENTER + SwingMeter.PERFECT_BAND + 0.01)));
	}

	@Test
	void nonPositiveHalfSweepIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> SwingMeter.value(0, 0));
		assertThrows(IllegalArgumentException.class, () -> SwingMeter.value(0, -3));
	}
}
