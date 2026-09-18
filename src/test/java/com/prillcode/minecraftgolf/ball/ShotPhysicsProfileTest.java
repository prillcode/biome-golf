package com.prillcode.minecraftgolf.ball;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShotPhysicsProfileTest {

	@Test
	void standardProfilePreservesAcceptedPutterPhysics() {
		assertEquals(1.0, ShotPhysicsProfile.STANDARD.landingHorizontalRetention());
		assertEquals(1.0, ShotPhysicsProfile.STANDARD.rollingFrictionMultiplier());
	}

	@Test
	void rejectsEnergyAddingOrNonPositiveValues() {
		assertThrows(IllegalArgumentException.class, () -> new ShotPhysicsProfile(1.01, 1.0));
		assertThrows(IllegalArgumentException.class, () -> new ShotPhysicsProfile(1.0, 0.0));
		assertThrows(IllegalArgumentException.class, () -> new ShotPhysicsProfile(1.0, 1.01));
	}

	@Test
	void alternateProfilesHaveDistinctLaunchAndRollContracts() {
		assertTrue(ShotPhysicsProfile.CHIP.launchVerticalMultiplier() < 1.0);
		assertTrue(ShotPhysicsProfile.STINGER.launchVerticalMultiplier() < 1.0);
		assertTrue(ShotPhysicsProfile.FLOP.launchVerticalMultiplier() > 1.0);
		assertTrue(ShotPhysicsProfile.CHIP.rollingFrictionMultiplier()
			< ShotPhysicsProfile.LOFTED_CLUB.rollingFrictionMultiplier());
		assertTrue(ShotPhysicsProfile.FLOP.landingHorizontalRetention()
			< ShotPhysicsProfile.CHIP.landingHorizontalRetention());
	}
}
