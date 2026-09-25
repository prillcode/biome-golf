package pro.apdev.biomegolf.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.hole.HoleBoundary;
import pro.apdev.biomegolf.hole.HoleDefinition;

class DevelopmentHoleLayoutTest {

	@Test
	void defaultHoleProducesTheBoundedOceanPlatform() {
		HoleDefinition hole = hole(
			new Vec3(0.5, 63.25, 0.5),
			new Vec3(16.5, 63.25, 0.5));

		DevelopmentHoleBuilder.Layout layout = DevelopmentHoleBuilder.layout(hole);

		assertEquals(-2, layout.minX());
		assertEquals(18, layout.maxX());
		assertEquals(62, layout.floorY());
		assertEquals(-3, layout.minZ());
		assertEquals(3, layout.maxZ());
		assertEquals(63, layout.clearMinY());
		assertEquals(80, layout.clearMaxY());
		assertEquals(16, layout.cupX());
		assertEquals(63, layout.cupY());
		assertEquals(0, layout.cupZ());
	}

	@Test
	void layoutFollowsConfiguredHorizontalTeeAndCup() {
		HoleDefinition hole = hole(
			new Vec3(10.5, 70.25, -4.5),
			new Vec3(-5.5, 70.25, 8.5));

		DevelopmentHoleBuilder.Layout layout = DevelopmentHoleBuilder.layout(hole);

		assertEquals(-8, layout.minX());
		assertEquals(12, layout.maxX());
		assertEquals(69, layout.floorY());
		assertEquals(-8, layout.minZ());
		assertEquals(11, layout.maxZ());
		assertEquals(-6, layout.cupX());
		assertEquals(70, layout.cupY());
		assertEquals(8, layout.cupZ());
	}

	@Test
	void slopedHoleIsRejectedInsteadOfDestructivelyFlattened() {
		HoleDefinition hole = hole(
			new Vec3(0.5, 63.25, 0.5),
			new Vec3(16.5, 66.25, 0.5));

		assertThrows(IllegalArgumentException.class, () -> DevelopmentHoleBuilder.layout(hole));
	}

	private static HoleDefinition hole(Vec3 tee, Vec3 cup) {
		return new HoleDefinition(
			"test:dev", 1, "minecraft:overworld", tee, cup, 4,
			new HoleBoundary(
				new Vec3(-100.0, -64.0, -100.0),
				new Vec3(100.0, 384.0, 100.0)));
	}
}
