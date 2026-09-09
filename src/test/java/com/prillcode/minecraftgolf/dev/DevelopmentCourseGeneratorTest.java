package com.prillcode.minecraftgolf.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DevelopmentCourseGeneratorTest {

	@Test
	void m5PlanIsVersionedAndEveryMutationStaysInsideDeclaredFootprints() {
		DevelopmentCoursePlan plan = M5DevelopmentLayout.plan();

		assertEquals("minecraft_golf:m5_ocean_campus", plan.identity().id());
		assertEquals(1, plan.identity().version());
		assertEquals(new BlockPoint(-320, 48, -256), plan.campusEnvelope().min());
		assertEquals(new BlockPoint(448, 112, 512), plan.campusEnvelope().max());
		assertEquals(4, plan.authoredRegions().size());
		assertTrue(plan.desiredBlocks().keySet().stream().allMatch(plan::isInsideAuthoredRegion));
		assertTrue(plan.desiredBlocks().keySet().stream().allMatch(plan.campusEnvelope()::contains));
	}

	@Test
	void preparationIsDeterministicIdempotentAndPreservesOutsideBlocks() {
		DevelopmentCoursePlan plan = M5DevelopmentLayout.plan();
		FakeWorld world = new FakeWorld();
		BlockPoint outside = new BlockPoint(300, 62, 0);
		world.blocks.put(outside, LayoutBlock.GRASS_BLOCK);

		DevelopmentCourseGenerator.GenerationResult first = DevelopmentCourseGenerator.prepare(plan, world);
		Map<BlockPoint, LayoutBlock> firstSnapshot = Map.copyOf(world.blocks);
		DevelopmentCourseGenerator.GenerationResult second = DevelopmentCourseGenerator.prepare(plan, world);

		assertEquals(plan.desiredBlocks().size(), first.changedBlocks());
		assertEquals(0, second.changedBlocks());
		assertEquals(firstSnapshot, world.blocks);
		assertEquals(LayoutBlock.GRASS_BLOCK, world.blocks.get(outside));
		assertTrue(world.writes.stream().allMatch(plan::isInsideAuthoredRegion));
	}

	@Test
	void unsafePreflightRejectsBeforeWritingAnything() {
		DevelopmentCoursePlan plan = M5DevelopmentLayout.plan();
		FakeWorld world = new FakeWorld();
		BlockPoint unsafe = plan.desiredBlocks().keySet().stream().skip(4).findFirst().orElseThrow();
		world.unsafe.add(unsafe);

		DevelopmentCourseGenerator.UnsafeTerrainException error = assertThrows(
			DevelopmentCourseGenerator.UnsafeTerrainException.class,
			() -> DevelopmentCourseGenerator.prepare(plan, world));

		assertEquals(unsafe, error.point());
		assertTrue(world.writes.isEmpty());
	}

	@Test
	void planRejectsOperationsOutsideAuthoredRegions() {
		BlockVolume envelope = volume(0, 0, 0, 20, 20, 20);
		AuthoredRegion region = new AuthoredRegion("safe", volume(1, 1, 1, 5, 5, 5));

		assertThrows(IllegalArgumentException.class, () -> new DevelopmentCoursePlan(
			M5DevelopmentLayout.IDENTITY, "minecraft:overworld", envelope, List.of(region),
			List.of(new LayoutOperation(volume(5, 5, 5, 6, 5, 5), LayoutBlock.GRASS_BLOCK))));
	}

	private static BlockVolume volume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return new BlockVolume(new BlockPoint(minX, minY, minZ), new BlockPoint(maxX, maxY, maxZ));
	}

	private static final class FakeWorld implements DevelopmentCourseGenerator.WorldAccess {
		private final Map<BlockPoint, LayoutBlock> blocks = new HashMap<>();
		private final Set<BlockPoint> unsafe = new HashSet<>();
		private final Set<BlockPoint> writes = new HashSet<>();

		@Override
		public boolean canReplace(BlockPoint point, LayoutBlock desired, Set<LayoutBlock> generatedPalette) {
			return !unsafe.contains(point);
		}

		@Override
		public boolean matches(BlockPoint point, LayoutBlock desired) {
			return blocks.get(point) == desired;
		}

		@Override
		public void set(BlockPoint point, LayoutBlock desired) {
			writes.add(point);
			blocks.put(point, desired);
		}
	}
}
