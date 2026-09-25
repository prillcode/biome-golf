package pro.apdev.biomegolf.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.hole.HoleBoundary;
import pro.apdev.biomegolf.hole.HoleDefinition;

class CourseDefinitionTest {

	@Test
	void acceptsAnyPositiveHoleCount() {
		assertEquals(1, course(List.of(hole(1, 4))).holes().size());
		assertEquals(2, course(List.of(hole(1, 4), hole(2, 3))).holes().size());
		CourseDefinition five = course(
			List.of(hole(1, 4), hole(2, 3), hole(3, 5), hole(4, 4), hole(5, 3)));
		assertEquals(5, five.holes().size());
		assertEquals(19, five.totalPar());
		assertEquals("test:2", five.hole(2).id());
		assertThrows(IllegalArgumentException.class, () -> five.hole(0));
		assertThrows(IllegalArgumentException.class, () -> five.hole(6));
		assertThrows(IllegalArgumentException.class, () -> course(List.of()));
	}

	@Test
	void acceptsArbitraryPars() {
		CourseDefinition course = course(List.of(hole(1, 3), hole(2, 6)));
		assertEquals(9, course.totalPar());
	}

	@Test
	void rejectsNumberingGapsAndMisordering() {
		assertThrows(IllegalArgumentException.class,
			() -> course(List.of(hole(1, 4), hole(3, 5))));
		assertThrows(IllegalArgumentException.class,
			() -> course(List.of(hole(1, 4), hole(2, 3), hole(4, 5))));
		assertThrows(IllegalArgumentException.class,
			() -> course(List.of(hole(1, 4), hole(3, 3), hole(2, 5))));
		assertThrows(IllegalArgumentException.class, () -> course(List.of(hole(2, 4))));
	}

	@Test
	void rejectsDuplicateIdsAndCrossDimensionHoles() {
		HoleDefinition duplicate = definition("test:1", 2, 3, "minecraft:overworld");
		assertThrows(IllegalArgumentException.class,
			() -> course(List.of(hole(1, 4), duplicate)));

		HoleDefinition nether = definition("test:2", 2, 3, "minecraft:the_nether");
		assertThrows(IllegalArgumentException.class,
			() -> course(List.of(hole(1, 4), nether, hole(3, 5))));
	}

	private static CourseDefinition course(List<HoleDefinition> holes) {
		return new CourseDefinition("test", "Test Course", "minecraft:overworld",
			new GeneratedLayoutIdentity("test-course", 1), holes);
	}

	private static HoleDefinition hole(int number, int par) {
		return definition("test:" + number, number, par, "minecraft:overworld");
	}

	private static HoleDefinition definition(String id, int number, int par, String dimension) {
		Vec3 tee = new Vec3(number * 20.0, 64.25, 0.0);
		Vec3 cup = new Vec3(number * 20.0 + 10.0, 64.25, 0.0);
		return new HoleDefinition(id, number, dimension, tee, cup, par,
			new HoleBoundary(new Vec3(0.0, 0.0, -10.0), new Vec3(200.0, 100.0, 10.0)),
			new GeneratedLayoutIdentity("test-hole-" + number, 1), HoleTransition.at(tee));
	}
}
