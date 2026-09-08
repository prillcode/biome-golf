package com.prillcode.minecraftgolf.hole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;

class HoleDefinitionTest {

	private static final HoleBoundary BOUNDARY = new HoleBoundary(
		new Vec3(0.0, 0.0, 0.0),
		new Vec3(100.0, 100.0, 100.0)
	);

	@Test
	void definesOneConfiguredHoleAndDoubleParPlusTwoLimit() {
		HoleDefinition hole = new HoleDefinition(
			"family_test:1",
			1,
			"minecraft:overworld",
			new Vec3(10.5, 65.0, 10.5),
			new Vec3(90.5, 64.0, 90.5),
			4,
			BOUNDARY
		);

		assertEquals(10, hole.strokeLimit());
		assertEquals("family_test:1", hole.id());
	}

	@Test
	void rejectsInvalidIdentityNumberParAndDimension() {
		assertThrows(IllegalArgumentException.class, () -> hole(" ", 1, "minecraft:overworld", 4));
		assertThrows(IllegalArgumentException.class, () -> hole("test:1", 0, "minecraft:overworld", 4));
		assertThrows(IllegalArgumentException.class, () -> hole("test:1", 1, "", 4));
		assertThrows(IllegalArgumentException.class, () -> hole("test:1", 1, "minecraft:overworld", 0));
		assertThrows(IllegalArgumentException.class,
			() -> hole("test:1", 1, "minecraft:overworld", Integer.MAX_VALUE));
	}

	@Test
	void requiresTeeAndCupInsidePlayableBoundary() {
		assertThrows(IllegalArgumentException.class, () -> new HoleDefinition(
			"test:1", 1, "minecraft:overworld",
			new Vec3(-0.01, 50.0, 50.0), new Vec3(90.0, 50.0, 90.0), 4, BOUNDARY
		));
		assertThrows(IllegalArgumentException.class, () -> new HoleDefinition(
			"test:1", 1, "minecraft:overworld",
			new Vec3(10.0, 50.0, 10.0), new Vec3(100.01, 50.0, 90.0), 4, BOUNDARY
		));
	}

	private static HoleDefinition hole(String id, int number, String dimension, int par) {
		return new HoleDefinition(
			id, number, dimension,
			new Vec3(10.0, 50.0, 10.0),
			new Vec3(90.0, 50.0, 90.0),
			par,
			BOUNDARY
		);
	}
}
