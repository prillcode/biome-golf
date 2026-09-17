package com.prillcode.minecraftgolf.course;

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
