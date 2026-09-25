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
		// Retuned 2026-09: stingers launch higher for carry, but must stay flatter than a
		// chip and keep their distinct rollout so the low-flight identity is preserved.
		assertTrue(ShotPhysicsProfile.STINGER.launchVerticalMultiplier() >= 0.5);
		assertTrue(ShotPhysicsProfile.STINGER.launchVerticalMultiplier()
			< ShotPhysicsProfile.CHIP.launchVerticalMultiplier());
		assertEquals(0.65, ShotPhysicsProfile.STINGER.rollingFrictionMultiplier());
		assertEquals(0.60, ShotPhysicsProfile.STINGER.landingHorizontalRetention());
		assertTrue(ShotPhysicsProfile.STINGER.rollingFrictionMultiplier()
			< ShotPhysicsProfile.LOFTED_CLUB.rollingFrictionMultiplier());
		assertTrue(ShotPhysicsProfile.FLOP.launchVerticalMultiplier() > 1.0);
		assertTrue(ShotPhysicsProfile.CHIP.rollingFrictionMultiplier()
			< ShotPhysicsProfile.LOFTED_CLUB.rollingFrictionMultiplier());
		assertTrue(ShotPhysicsProfile.FLOP.landingHorizontalRetention()
			< ShotPhysicsProfile.CHIP.landingHorizontalRetention());
	}
}
