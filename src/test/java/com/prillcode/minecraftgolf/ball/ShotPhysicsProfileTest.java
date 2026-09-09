package com.prillcode.minecraftgolf.ball;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
