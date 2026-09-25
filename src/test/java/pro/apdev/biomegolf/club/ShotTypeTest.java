package pro.apdev.biomegolf.club;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShotTypeTest {
	@Test
	void serverMatrixMatchesContextualChoices() {
		assertEquals(java.util.List.of(ShotType.STINGER), ShotType.choicesFor(GolfClubs.DRIVER));
		assertEquals(java.util.List.of(ShotType.CHIP), ShotType.choicesFor(GolfClubs.MID_IRON));
		assertEquals(java.util.List.of(ShotType.CHIP, ShotType.FLOP), ShotType.choicesFor(GolfClubs.WEDGE));
		assertTrue(ShotType.FLOP.allowedFor(GolfClubs.SHORT_IRON));
		assertFalse(ShotType.CHIP.allowedFor(GolfClubs.PUTTER));
	}

	@Test
	void profilesGiveDistinctLaunchShapes() {
		double standard = ShotResolver.initialVelocity(GolfClubs.WEDGE, 0, 0, 1, 0.5, 100).y();
		double chip = ShotResolver.initialVelocity(GolfClubs.WEDGE, 0, 0, 1, 0.5, 100,
			ShotType.CHIP.profile(GolfClubs.WEDGE)).y();
		double flop = ShotResolver.initialVelocity(GolfClubs.WEDGE, 0, 0, 1, 0.5, 100,
			ShotType.FLOP.profile(GolfClubs.WEDGE)).y();
		assertTrue(chip < standard);
		assertTrue(flop > standard);
		assertTrue(ShotType.CHIP.profile(GolfClubs.WEDGE).rollingFrictionMultiplier()
			< ShotType.STANDARD.profile(GolfClubs.WEDGE).rollingFrictionMultiplier());
	}
}
