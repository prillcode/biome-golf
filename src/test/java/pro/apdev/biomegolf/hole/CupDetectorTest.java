package pro.apdev.biomegolf.hole;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.golf.Vec3;

class CupDetectorTest {

	private static final Vec3 CUP = new Vec3(10.5, 64.25, 20.5);

	@Test
	void capturesSlowBallWhosePathEntersCup() {
		assertTrue(CupDetector.entered(
			new Vec3(10.0, 64.3, 20.5),
			new Vec3(10.6, 64.2, 20.5),
			CUP,
			0.2
		));
	}

	@Test
	void doesNotCompleteForApproachOutsideOpeningOrWrongHeight() {
		assertFalse(CupDetector.entered(
			new Vec3(10.0, 64.25, 20.86),
			new Vec3(11.0, 64.25, 20.86), CUP, 0.2));
		assertFalse(CupDetector.entered(
			new Vec3(10.0, 65.0, 20.5),
			new Vec3(11.0, 65.0, 20.5), CUP, 0.2));
	}

	@Test
	void rejectsBallMovingTooFastToRemainInCupButDetectsTheCrossing() {
		Vec3 from = new Vec3(10.0, 64.25, 20.5);
		Vec3 to = new Vec3(11.0, 64.25, 20.5);

		assertTrue(CupDetector.intersects(from, to, CUP));
		assertFalse(CupDetector.entered(from, to, CUP, CupDetector.MAX_ENTRY_SPEED + 0.01));
		assertTrue(CupDetector.entered(from, to, CUP, CupDetector.MAX_ENTRY_SPEED));
	}

	@Test
	void reportsWhetherBallCenterRemainsInsideCaptureRegion() {
		assertTrue(CupDetector.contains(CUP, CUP));
		assertFalse(CupDetector.contains(new Vec3(11.0, 64.25, 20.5), CUP));
	}

	@Test
	void validatesSpeed() {
		assertThrows(IllegalArgumentException.class,
			() -> CupDetector.entered(CUP, CUP, CUP, Double.NaN));
		assertThrows(IllegalArgumentException.class,
			() -> CupDetector.entered(CUP, CUP, CUP, -0.1));
	}
}
