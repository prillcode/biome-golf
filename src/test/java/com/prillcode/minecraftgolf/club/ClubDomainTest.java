package com.prillcode.minecraftgolf.club;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.ball.PhysicsConfig;
import com.prillcode.minecraftgolf.golf.Vec3;

/**
 * Headless contract for the M2 club domain (ClubDefinition/GolfClubs/ShotResolver).
 */
class ClubDomainTest {

	private static final double MAX_SPEED = PhysicsConfig.DEFAULT.maxLaunchSpeed(); // 4.0

	// ------------------------------------------------------------------
	// GolfClubs catalog invariants
	// ------------------------------------------------------------------

	@Test
	void catalogHasSevenPrdClubs() {
		assertEquals(7, GolfClubs.ALL.size());
	}

	@Test
	void catalogIdsAreUniqueDowncaseIdentifierSafe() {
		Set<String> seen = new HashSet<>();
		for (ClubDefinition c : GolfClubs.ALL) {
			String id = c.id();
			assertTrue(id.matches("[a-z][a-z0-9_]*"), "bad id: " + id);
			assertTrue(seen.add(id), "duplicate id: " + id);
		}
		assertEquals(7, seen.size());
	}

	@Test
	void eachClubHasDisplayNameAndCarry() {
		for (ClubDefinition c : GolfClubs.ALL) {
			assertFalse(c.displayName().isBlank(), c.id());
			assertTrue(c.nominalCarry() > 0.0, c.id());
		}
	}

	@Test
	void byIdFindsAllAndUnknownIsEmpty() {
		for (ClubDefinition c : GolfClubs.ALL) {
			assertTrue(GolfClubs.byId(c.id()).isPresent(), c.id());
			assertEquals(c, GolfClubs.byId(c.id()).orElseThrow());
		}
		assertTrue(GolfClubs.byId("driver").isPresent());
		assertTrue(GolfClubs.byId("no_such_club").isEmpty());
		assertTrue(GolfClubs.byId(null).isEmpty());
	}

	@Test
	void noClubExceedsPhysicsLaunchCeiling() {
		for (ClubDefinition c : GolfClubs.ALL) {
			assertFalse(c.exceedsMaxSpeed(MAX_SPEED),
					"club " + c.id() + " fullPowerSpeed " + c.fullPowerSpeed()
							+ " exceeds ceiling " + MAX_SPEED);
		}
	}

	@Test
	void onlyPutterIsPutting() {
		int putting = 0;
		for (ClubDefinition c : GolfClubs.ALL) {
			if (c.putting()) {
				putting++;
			}
		}
		assertEquals(1, putting, "exactly the putter should be a putter");
		assertTrue(GolfClubs.PUTTER.putting());
		assertFalse(GolfClubs.DRIVER.putting());
	}

	// ------------------------------------------------------------------
	// Validation
	// ------------------------------------------------------------------

	@Test
	void rejectsBlankIdAndName() {
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> new ClubDefinition(" ", "Driver", 82, 1.9, 12, 0.8, false));
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> new ClubDefinition("driver", "", 82, 1.9, 12, 0.8, false));
	}

	@Test
	void rejectsBadSpeedAndAngle() {
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> new ClubDefinition("x", "X", 82, 0.0, 12, 0.8, false));
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> new ClubDefinition("x", "X", 82, 1.9, 95.0, 0.8, false));
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> new ClubDefinition("x", "X", 82, 1.9, -1.0, 0.8, false));
	}

	// ------------------------------------------------------------------
	// ShotResolver
	// ------------------------------------------------------------------

	@Test
	void driverShotAimsBackToMinecraftSouth() {
		// yaw 0 faces +Z (south); expect positive Z velocity, no X.
		Vec3 v = ShotResolver.initialVelocity(GolfClubs.DRIVER, 0.0, 0.0, MAX_SPEED);
		assertNotNull(v);
		assertEquals(0.0, v.x(), 1e-9);
		assertTrue(v.z() > 0.0, "expected +Z for yaw 0: " + v);
		double expected = GolfClubs.DRIVER.fullPowerSpeed() * Math.cos(Math.toRadians(12));
		assertEquals(expected, v.z(), 1e-9);
		// up component from loft
		assertTrue(v.y() > 0.0, "driver should launch up");
	}

	@Test
	void driverShotEastIsPositiveX() {
		Vec3 v = ShotResolver.initialVelocity(GolfClubs.DRIVER, -90.0, 0.0, MAX_SPEED);
		assertNotNull(v);
		assertTrue(v.x() > 0.0, "east (yaw -90) is +X in Minecraft: " + v);
		assertEquals(0.0, v.z(), 1e-9);
	}

	@Test
	void putterIsNearRolling() {
		Vec3 v = ShotResolver.initialVelocity(GolfClubs.PUTTER, 0.0, 0.0, MAX_SPEED);
		assertNotNull(v);
		// Putter loft ~0.5 deg -> tiny up, mostly horizontal forward roll.
		assertTrue(v.y() < v.z() * 0.1, "putter should roll, not fly high: " + v);
	}

	@Test
	void steeplyDownAimIsIllegal() {
		assertNull(ShotResolver.initialVelocity(GolfClubs.DRIVER, 0.0, 60.0, MAX_SPEED));
	}

	@Test
	void speedIsCappedAtCeilingNotAbove() {
		ClubDefinition tooFast = new ClubDefinition("t", "T", 999, 50.0, 12, 0.8, false);
		Vec3 v = ShotResolver.initialVelocity(tooFast, 0.0, 0.0, MAX_SPEED);
		assertNotNull(v);
		assertTrue(v.length() <= MAX_SPEED + 1e-9, "speed exceeds ceiling: " + v.length());
	}

	@Test
	void repeatableInputGivesRepeatableResult() {
		Vec3 a = ShotResolver.initialVelocity(GolfClubs.LONG_IRON, 25.0, -4.0, MAX_SPEED);
		Vec3 b = ShotResolver.initialVelocity(GolfClubs.LONG_IRON, 25.0, -4.0, MAX_SPEED);
		assertEquals(a, b);
	}
}
