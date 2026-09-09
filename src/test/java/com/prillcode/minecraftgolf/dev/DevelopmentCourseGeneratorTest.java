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
		assertEquals(4, plan.identity().version());
		assertEquals(new BlockPoint(-320, 48, -256), plan.campusEnvelope().min());
		assertEquals(new BlockPoint(448, 112, 512), plan.campusEnvelope().max());
		assertEquals(4, plan.authoredRegions().size());
		assertTrue(plan.desiredBlocks().keySet().stream().allMatch(plan::isInsideAuthoredRegion));
		assertTrue(plan.desiredBlocks().keySet().stream().allMatch(plan.campusEnvelope()::contains));
	}

	@Test
	void holeOneMatchesParFourMetadataAndContainsRiskRewardLandmarks() {
		DevelopmentCoursePlan plan = M5DevelopmentLayout.plan();
		Map<BlockPoint, LayoutBlock> blocks = plan.desiredBlocks();
		var hole = M5DevelopmentCourse.definition().hole(1);

		assertEquals(4, hole.par());
		assertEquals(new BlockPoint(-32, 62, -160), floorBelow(hole.tee()));
		assertEquals(LayoutBlock.GRASS_BLOCK, blocks.get(floorBelow(hole.tee())));
		assertEquals(LayoutBlock.GOLF_CUP, blocks.get(new BlockPoint(184, 63, -160)));
		assertEquals(LayoutBlock.WATER, blocks.get(new BlockPoint(70, 62, -168)));
		assertEquals(LayoutBlock.GRASS_BLOCK, blocks.get(new BlockPoint(70, 62, -150)));
		assertEquals(LayoutBlock.SAND, blocks.get(new BlockPoint(160, 62, -152)));
		assertTrue(hole.boundary().contains(hole.tee()));
		assertTrue(hole.boundary().contains(hole.cup()));
	}

	@Test
	void practiceRangeContainsEveryRequiredTrainingSurfaceAndTarget() {
		Map<BlockPoint, LayoutBlock> blocks = M5DevelopmentLayout.plan().desiredBlocks();

		assertEquals(LayoutBlock.GOLD_BLOCK, blocks.get(new BlockPoint(-246, 62, -200)));
		assertEquals(LayoutBlock.GOLF_CUP, blocks.get(new BlockPoint(-246, 63, -202)));
		assertEquals(LayoutBlock.GOLF_CUP, blocks.get(new BlockPoint(-196, 63, -202)));
		assertEquals(LayoutBlock.GOLF_CUP, blocks.get(new BlockPoint(-146, 63, -202)));
		assertEquals(LayoutBlock.TARGET, blocks.get(new BlockPoint(-105, 65, -202)));
		assertEquals(LayoutBlock.SAND, blocks.get(new BlockPoint(-228, 62, -144)));
		assertEquals(LayoutBlock.GOLF_CUP, blocks.get(new BlockPoint(-228, 63, -144)));
		assertEquals(LayoutBlock.GOLF_CUP, blocks.get(new BlockPoint(-112, 63, -158)));
		assertEquals(LayoutBlock.DIRT, blocks.get(new BlockPoint(-158, 62, -150)));
		assertEquals(LayoutBlock.WATER, blocks.get(new BlockPoint(-270, 62, -92)));
		assertEquals(LayoutBlock.ICE, blocks.get(new BlockPoint(-160, 62, -92)));
		assertEquals(LayoutBlock.SLIME_BLOCK, blocks.get(new BlockPoint(-160, 62, -68)));
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

	private static BlockPoint floorBelow(com.prillcode.minecraftgolf.golf.Vec3 position) {
		return new BlockPoint((int) Math.floor(position.x()),
			(int) Math.floor(position.y() - 0.25 - 0.01), (int) Math.floor(position.z()));
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
