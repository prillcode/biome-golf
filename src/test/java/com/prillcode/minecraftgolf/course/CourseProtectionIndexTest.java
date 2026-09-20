package com.prillcode.minecraftgolf.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

class CourseProtectionIndexTest {
	@Test
	void indexesAllCoursesByDimensionAndReplacesDeletedEntries() {
		CourseProtectionIndex index = new CourseProtectionIndex();
		CourseDefinition overworldA = course("a", "minecraft:overworld", 0);
		CourseDefinition overworldB = course("b", "minecraft:overworld", 100);
		CourseDefinition nether = course("nether", "minecraft:the_nether", 0);
		index.replaceAuthoredCourses(List.of(overworldA, overworldB, nether));

		assertTrue(index.isProtected("minecraft:overworld", 0, 64, 0));
		assertTrue(index.isProtected("minecraft:overworld", 100, 64, 0));
		assertTrue(index.isProtected("minecraft:the_nether", 0, 64, 0));
		assertFalse(index.isProtected("minecraft:the_nether", 100, 64, 0));

		index.replaceAuthoredCourses(List.of(overworldB, nether));
		assertFalse(index.isProtected("minecraft:overworld", 0, 64, 0));
		assertTrue(index.isProtected("minecraft:overworld", 100, 64, 0));
	}

	@Test
	void configuredHoleCoexistsWithAuthoredAndCanBeClearedIndependently() {
		CourseProtectionIndex index = new CourseProtectionIndex();
		index.replaceAuthoredCourses(List.of(course("a", "minecraft:overworld", 0)));
		index.replaceConfiguredHole(hole("dev", "minecraft:overworld", 200));
		assertTrue(index.isProtected("minecraft:overworld", 0, 64, 0));
		assertTrue(index.isProtected("minecraft:overworld", 200, 64, 0));

		index.clearConfigured();
		assertTrue(index.isProtected("minecraft:overworld", 0, 64, 0));
		assertFalse(index.isProtected("minecraft:overworld", 200, 64, 0));
		index.clear();
		assertFalse(index.isProtected("minecraft:overworld", 0, 64, 0));
	}

	@Test
	void indexesLandscapePerimetersByDimensionIncludingDraftOnlyCourses() {
		CourseProtectionIndex index = new CourseProtectionIndex();
		// A finalized course contributes zones + perimeter; a draft-only course contributes
		// only its perimeter (no holes are authored yet).
		CourseLandscape finalizedLandscape = landscape("a", "minecraft:overworld", 0, true);
		CourseLandscape draftLandscape = landscape("wip", "minecraft:overworld", 500, false);
		index.replaceAuthoredCourses(List.of(course("a", "minecraft:overworld", 0)),
			List.of(finalizedLandscape, draftLandscape));

		assertTrue(index.isProtected("minecraft:overworld", 0, 64, 0));
		assertTrue(index.isProtected("minecraft:overworld", 500, 64, 0));
		assertFalse(index.isProtected("minecraft:overworld", 900, 64, 0));
		assertFalse(index.isProtected("minecraft:the_nether", 0, 64, 0));

		// Replacing authored courses with no perimeters drops landscape-only protection.
		index.replaceAuthoredCourses(List.of(course("a", "minecraft:overworld", 0)));
		assertTrue(index.isProtected("minecraft:overworld", 0, 64, 0));
		assertFalse(index.isProtected("minecraft:overworld", 500, 64, 0));
	}

	@Test
	void verdictCombinesZonesAndLandscapesWithMostRestrictiveWins() {
		CourseProtectionIndex index = new CourseProtectionIndex();
		CourseLandscape unlocked = landscape("soft", "minecraft:overworld", 0, false);
		CourseLandscape locked = landscape("hard", "minecraft:overworld", 0, true);

		// Zone only: tee/cup vicinity.
		index.replaceAuthoredCourses(List.of(course("a", "minecraft:overworld", 0)), List.of());
		assertEquals(ProtectionVerdict.DENY_NON_OP, index.verdict("minecraft:overworld", 0, 64, 0));
		assertEquals(ProtectionVerdict.ALLOW, index.verdict("minecraft:overworld", 900, 64, 900));

		// Unlocked perimeter over the same position, then a locked one stacked on top.
		index.replaceAuthoredCourses(List.of(), List.of(unlocked));
		assertEquals(ProtectionVerdict.DENY_NON_OP, index.verdict("minecraft:overworld", 0, 64, 0));
		index.replaceAuthoredCourses(List.of(), List.of(unlocked, locked));
		assertEquals(ProtectionVerdict.DENY_ALL, index.verdict("minecraft:overworld", 0, 64, 0));

		index.clear();
		assertEquals(ProtectionVerdict.ALLOW, index.verdict("minecraft:overworld", 0, 64, 0));
		assertTrue(index.landscapes("minecraft:overworld").isEmpty());
	}

	private static CourseLandscape landscape(String courseId, String dimension, double centerX,
			boolean locked) {
		return new CourseLandscape(courseId, dimension, new HoleBoundary(
			new Vec3(centerX - 50, 0, -50), new Vec3(centerX + 50, 320, 50)), locked);
	}

	private static CourseDefinition course(String id, String dimension, double x) {
		return new CourseDefinition(id, id, dimension, new GeneratedLayoutIdentity(id, 1),
			List.of(hole(id + ":1", dimension, x)));
	}

	private static HoleDefinition hole(String id, String dimension, double x) {
		Vec3 tee = new Vec3(x, 64, 0);
		return new HoleDefinition(id, 1, dimension, tee, new Vec3(x + 30, 64, 0), 4,
			new HoleBoundary(new Vec3(x - 20, 0, -20), new Vec3(x + 60, 100, 20)));
	}
}
