package com.prillcode.minecraftgolf.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;

class CourseLandscapeTest {

	private static final HoleBoundary BOX = new HoleBoundary(
		new Vec3(-10.0, -64.0, 20.0), new Vec3(10.0, 320.0, 40.0));

	@Test
	void containsBlockCoordinatesInclusively() {
		CourseLandscape landscape = new CourseLandscape("pine-hills", "minecraft:overworld", BOX, false);

		assertTrue(landscape.contains(0, 64, 30));
		assertTrue(landscape.contains(-10, -64, 20));
		assertTrue(landscape.contains(10, 320, 40));
		assertFalse(landscape.contains(-11, 64, 30));
		assertFalse(landscape.contains(0, 64, 19));
		assertFalse(landscape.contains(0, 321, 30));
	}

	@Test
	void containsPrecisePositions() {
		CourseLandscape landscape = new CourseLandscape("pine-hills", "minecraft:overworld", BOX, false);

		assertTrue(landscape.contains(new Vec3(10.0, 65.5, 40.0)));
		assertFalse(landscape.contains(new Vec3(10.5, 65.5, 40.0)));
	}

	@Test
	void unlockedPerimeterDeniesNonOperatorsOnly() {
		CourseLandscape unlocked = new CourseLandscape("pine-hills", "minecraft:overworld", BOX, false);

		assertEquals(ProtectionVerdict.DENY_NON_OP, unlocked.verdictFor(0, 64, 30));
		assertTrue(unlocked.verdictFor(0, 64, 30).denies(false));
		assertFalse(unlocked.verdictFor(0, 64, 30).denies(true));
	}

	@Test
	void lockedPerimeterDeniesEveryone() {
		CourseLandscape locked = new CourseLandscape("pine-hills", "minecraft:overworld", BOX, true);

		assertEquals(ProtectionVerdict.DENY_ALL, locked.verdictFor(0, 64, 30));
		assertTrue(locked.verdictFor(0, 64, 30).denies(false));
		assertTrue(locked.verdictFor(0, 64, 30).denies(true));
	}

	@Test
	void outsidePerimeterAlwaysAllows() {
		CourseLandscape locked = new CourseLandscape("pine-hills", "minecraft:overworld", BOX, true);

		assertEquals(ProtectionVerdict.ALLOW, locked.verdictFor(500, 64, 500));
		assertFalse(locked.verdictFor(500, 64, 500).denies(false));
		assertFalse(locked.verdictFor(500, 64, 500).denies(true));
		assertEquals(ProtectionVerdict.ALLOW, locked.verdictFor(new Vec3(500.0, 64.0, 500.0)));
	}

	@Test
	void normalizesCourseIdAndDimension() {
		CourseLandscape landscape =
			new CourseLandscape("  Pine-Hills  ", "Minecraft:Overworld", BOX, false);

		assertEquals("pine-hills", landscape.courseId());
		assertEquals("minecraft:overworld", landscape.dimension());
	}

	@Test
	void withLockedKeepsPerimeterAndTogglesTheLock() {
		CourseLandscape unlocked = new CourseLandscape("pine-hills", "minecraft:overworld", BOX, false);

		CourseLandscape locked = unlocked.withLocked(true);
		assertEquals(unlocked.bounds(), locked.bounds());
		assertEquals(unlocked.courseId(), locked.courseId());
		assertEquals(ProtectionVerdict.DENY_ALL, locked.verdictFor(0, 64, 30));
		assertEquals(ProtectionVerdict.DENY_NON_OP, unlocked.verdictFor(0, 64, 30));
	}

	@Test
	void rejectsInvalidMetadata() {
		assertThrows(IllegalArgumentException.class,
			() -> new CourseLandscape("", "minecraft:overworld", BOX, false));
		assertThrows(IllegalArgumentException.class,
			() -> new CourseLandscape("pine hills", "minecraft:overworld", BOX, false));
		assertThrows(IllegalArgumentException.class,
			() -> new CourseLandscape("pine-hills", "not a dimension", BOX, false));
		assertThrows(NullPointerException.class,
			() -> new CourseLandscape("pine-hills", "minecraft:overworld", null, false));
	}
}
