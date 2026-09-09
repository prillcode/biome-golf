package com.prillcode.minecraftgolf.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

class CourseDefinitionTest {

	@Test
	void requiresExactlyOrderedParFourThreeFiveHoles() {
		CourseDefinition course = course(holes());

		assertEquals(3, course.holes().size());
		assertEquals(12, course.totalPar());
		assertEquals("test:2", course.hole(2).id());
		assertThrows(IllegalArgumentException.class, () -> course.hole(0));
		assertThrows(IllegalArgumentException.class, () -> course(List.of(hole(1, 4), hole(2, 3))));
		assertThrows(IllegalArgumentException.class,
			() -> course(List.of(hole(1, 4), hole(3, 3), hole(2, 5))));
		assertThrows(IllegalArgumentException.class,
			() -> course(List.of(hole(1, 3), hole(2, 4), hole(3, 5))));
	}

	@Test
	void rejectsDuplicateIdsAndCrossDimensionHoles() {
		HoleDefinition duplicate = definition("test:1", 2, 3, "minecraft:overworld");
		assertThrows(IllegalArgumentException.class,
			() -> course(List.of(hole(1, 4), duplicate, hole(3, 5))));

		HoleDefinition nether = definition("test:2", 2, 3, "minecraft:the_nether");
		assertThrows(IllegalArgumentException.class,
			() -> course(List.of(hole(1, 4), nether, hole(3, 5))));
	}

	private static CourseDefinition course(List<HoleDefinition> holes) {
		return new CourseDefinition("test", "Test Course", "minecraft:overworld",
			new GeneratedLayoutIdentity("test-course", 1), holes);
	}

	private static List<HoleDefinition> holes() {
		return List.of(hole(1, 4), hole(2, 3), hole(3, 5));
	}

	private static HoleDefinition hole(int number, int par) {
		return definition("test:" + number, number, par, "minecraft:overworld");
	}

	private static HoleDefinition definition(String id, int number, int par, String dimension) {
		Vec3 tee = new Vec3(number * 20.0, 64.25, 0.0);
		Vec3 cup = new Vec3(number * 20.0 + 10.0, 64.25, 0.0);
		return new HoleDefinition(id, number, dimension, tee, cup, par,
			new HoleBoundary(new Vec3(0.0, 0.0, -10.0), new Vec3(100.0, 100.0, 10.0)),
			new GeneratedLayoutIdentity("test-hole-" + number, 1), HoleTransition.at(tee));
	}
}
