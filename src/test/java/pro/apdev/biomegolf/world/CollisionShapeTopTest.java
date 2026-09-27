package pro.apdev.biomegolf.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

class CollisionShapeTopTest {

	@Test
	void fullBlockTopIsOne() {
		assertEquals(1.0, CollisionShapeTop.at(Shapes.block(), 0.5, 0.5), 1.0E-9);
	}

	@Test
	void mossCarpetTopIsOneSixteenth() {
		VoxelShape carpet = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0 / 16.0, 1.0);
		assertEquals(1.0 / 16.0, CollisionShapeTop.at(carpet, 0.5, 0.5), 1.0E-9);
	}

	@Test
	void bottomSlabTopIsOneHalf() {
		VoxelShape slab = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);
		assertEquals(0.5, CollisionShapeTop.at(slab, 0.5, 0.5), 1.0E-9);
	}

	@Test
	void fencePostIsFollowedOnlyUnderItsFootprint() {
		VoxelShape fence = Shapes.box(0.4375, 0.0, 0.4375, 0.5625, 1.5, 0.5625);
		assertEquals(1.5, CollisionShapeTop.at(fence, 0.5, 0.5), 1.0E-9);
		assertTrue(Double.isNaN(CollisionShapeTop.at(fence, 0.1, 0.1)));
	}

	@Test
	void stairHalvesReportTheirOwnTop() {
		// Approximates a stair: a half-height side and a full-height side.
		VoxelShape stair = Shapes.or(
			Shapes.box(0.0, 0.0, 0.0, 0.5, 0.5, 1.0),
			Shapes.box(0.5, 0.0, 0.0, 1.0, 1.0, 1.0));
		assertEquals(0.5, CollisionShapeTop.at(stair, 0.25, 0.5), 1.0E-9);
		assertEquals(1.0, CollisionShapeTop.at(stair, 0.75, 0.5), 1.0E-9);
	}

	@Test
	void emptyShapeHasNoTop() {
		assertTrue(Double.isNaN(CollisionShapeTop.at(Shapes.empty(), 0.5, 0.5)));
	}
}
