package com.prillcode.minecraftgolf.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.dev.M5DevelopmentCourse;

class CourseProtectionTest {

	@Test
	void buildsTeeAndCupZonesForEveryHole() {
		List<ProtectedZone> zones =
			CourseProtection.zonesFor(M5DevelopmentCourse.definition(), CourseProtectionConfig.DEFAULT);

		assertEquals(6, zones.size());
		for (int holeNumber = 1; holeNumber <= 3; holeNumber++) {
			int hole = holeNumber;
			assertEquals(1, zones.stream().filter(z -> z.holeNumber() == hole && z.kind().equals("tee")).count());
			assertEquals(1, zones.stream().filter(z -> z.holeNumber() == hole && z.kind().equals("cup")).count());
		}
	}

	@Test
	void protectsEveryM5TeeCupAndGreenUnderCupVicinity() {
		CourseDefinition course = M5DevelopmentCourse.definition();
		List<ProtectedZone> zones =
			CourseProtection.zonesFor(course, CourseProtectionConfig.DEFAULT);

		// Every authored tee and cup block position.
		for (int hole = 1; hole <= 3; hole++) {
			assertTrue(CourseProtection.isProtected(zones, course.hole(hole).tee()));
			assertTrue(CourseProtection.isProtected(zones, course.hole(hole).cup()));
		}
		// Greens are protected via cup vicinity (recorded S1 decision): the farthest
		// Hole 3 green block and every Hole 1 tee-box pad block.
		assertTrue(CourseProtection.isProtected(zones, -204, 70, 436));
		assertTrue(CourseProtection.isProtected(zones, -209, 70, 431));
		assertTrue(CourseProtection.isProtected(zones, -209, 74, 497));
	}

	@Test
	void leavesTerrainAndPracticeRangeBreakable() {
		List<ProtectedZone> zones =
			CourseProtection.zonesFor(M5DevelopmentCourse.definition(), CourseProtectionConfig.DEFAULT);

		// Practice range and ordinary fairway positions are outside every zone.
		assertFalse(CourseProtection.isProtected(zones, -160, 63, -150));
		assertFalse(CourseProtection.isProtected(zones, -246, 62, -202));
		assertFalse(CourseProtection.isProtected(zones, -300, 70, 470));
	}

	@Test
	void singleHoleZonesProtectOnlyThatHole() {
		List<ProtectedZone> zones =
			CourseProtection.zonesFor(M5DevelopmentCourse.definition().hole(2), CourseProtectionConfig.DEFAULT);

		assertEquals(2, zones.size());
		assertTrue(CourseProtection.isProtected(zones, -322, 71, 405));
		assertTrue(CourseProtection.isProtected(zones, -369, 70, 413));
		assertFalse(CourseProtection.isProtected(zones, -207, 71, 426));
	}

	@Test
	void rejectsInvalidConfigValues() {
		assertThrows(IllegalArgumentException.class, () -> new CourseProtectionConfig(0.0, 8.0));
		assertThrows(IllegalArgumentException.class, () -> new CourseProtectionConfig(12.0, 0.0));
		assertThrows(IllegalArgumentException.class, () -> new CourseProtectionConfig(Double.NaN, 8.0));
	}

	@Test
	void breakPolicyProtectsZonesAndCupButExemptsDevOperators() {
		List<ProtectedZone> zones = List.of(
			new ProtectedZone("tee", 1, new com.prillcode.minecraftgolf.golf.Vec3(0, 64, 0), 5, 3));

		assertFalse(CourseProtection.mayBreak(zones, 0, 64, 0, false, false));
		assertFalse(CourseProtection.mayBreak(zones, 100, 64, 100, true, false));
		assertTrue(CourseProtection.mayBreak(zones, 100, 64, 100, false, false));
		assertTrue(CourseProtection.mayBreak(zones, 0, 64, 0, false, true));
		assertTrue(CourseProtection.mayBreak(zones, 100, 64, 100, true, true));
	}
}
