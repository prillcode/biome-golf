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
		assertEquals(11, plan.identity().version());
		assertEquals(new BlockPoint(-640, 32, -256), plan.campusEnvelope().min());
		assertEquals(new BlockPoint(448, 192, 640), plan.campusEnvelope().max());
		assertEquals(6, plan.authoredRegions().size());
		assertTrue(plan.operations().stream()
			.allMatch(operation -> plan.campusEnvelope().contains(operation.volume())));
		assertTrue(plan.desiredBlocks().keySet().stream().allMatch(plan::isInsideAuthoredRegion));
		assertTrue(plan.desiredBlocks().keySet().stream().allMatch(plan.campusEnvelope()::contains));
	}

	@Test
	void everyFixedHoleHasPairedBlueTeeMarkers() {
		Map<BlockPoint, LayoutBlock> blocks = M5DevelopmentLayout.plan().desiredBlocks();

		for (BlockPoint marker : List.of(
			new BlockPoint(-205, 74, 491), new BlockPoint(-209, 74, 497),
			new BlockPoint(-369, 70, 413), new BlockPoint(-369, 70, 420),
			new BlockPoint(-368, 69, 494), new BlockPoint(-370, 69, 488)
		)) {
			assertEquals(LayoutBlock.BLUE_CONCRETE, blocks.get(marker));
		}
	}

	@Test
	void holeOneUsesMinimalOverlaysOnTheApprovedNaturalRoute() {
		DevelopmentCoursePlan plan = M5DevelopmentLayout.plan();
		Map<BlockPoint, LayoutBlock> blocks = plan.desiredBlocks();
		var hole = M5DevelopmentCourse.definition().hole(1);

		assertEquals(4, hole.par());
		assertEquals(new BlockPoint(-207, 74, 494), floorBelow(hole.tee()));
		assertEquals(LayoutBlock.GREEN_WOOL, blocks.get(floorBelow(hole.tee())));
		assertEquals(LayoutBlock.GOLF_CUP, blocks.get(new BlockPoint(-355, 71, 416)));
		assertEquals(LayoutBlock.GREEN_WOOL, blocks.get(new BlockPoint(-360, 70, 416)));
		assertTrue(!blocks.containsKey(new BlockPoint(-360, 70, 411)),
			"the Hole 1 green should use an authored non-rectangular outline");
		assertEquals(LayoutBlock.BLACK_CONCRETE, blocks.get(new BlockPoint(-274, 69, 439)));
		assertEquals(LayoutBlock.YELLOW_CONCRETE, blocks.get(new BlockPoint(-286, 74, 463)));
		assertTrue(!blocks.containsKey(new BlockPoint(-280, 60, 433)),
			"the natural ravine terrain below the route must remain untouched");
		assertTrue(hole.boundary().contains(hole.tee()));
		assertTrue(hole.boundary().contains(hole.cup()));
	}

	@Test
	void holeTwoTeeBoxIsSeparateFromHoleOneCup() {
		var course = M5DevelopmentCourse.definition();
		var holeOneCup = course.hole(1).cup();
		var holeTwo = course.hole(2);
		var holeTwoTee = holeTwo.tee();
		Map<BlockPoint, LayoutBlock> blocks = M5DevelopmentLayout.plan().desiredBlocks();

		assertTrue(!holeOneCup.equals(holeTwoTee));
		assertEquals(new BlockPoint(-368, 70, 416), floorBelow(holeTwoTee));
		assertEquals(LayoutBlock.GREEN_WOOL, blocks.get(floorBelow(holeTwoTee)));
		assertEquals(LayoutBlock.GREEN_WOOL, blocks.get(new BlockPoint(-355, 70, 413)));
		assertEquals(LayoutBlock.GREEN_WOOL, blocks.get(new BlockPoint(-355, 70, 420)));
		assertEquals(3, holeTwo.par());
		assertEquals(new BlockPoint(-322, 70, 405), floorBelow(holeTwo.cup()));
		assertEquals(LayoutBlock.GOLF_CUP, blocks.get(new BlockPoint(-322, 71, 405)));
		assertEquals(LayoutBlock.GREEN_WOOL, blocks.get(new BlockPoint(-329, 70, 405)));
		assertTrue(!blocks.containsKey(new BlockPoint(-330, 70, 398)),
			"the Hole 2 green should use an authored non-rectangular outline");
		assertEquals(LayoutBlock.AIR, blocks.get(new BlockPoint(-280, 70, 433)));
	}

	@Test
	void holeThreeProvidesTwoNaturalRoutesAndAGuardedSupportedGreen() {
		DevelopmentCoursePlan plan = M5DevelopmentLayout.plan();
		Map<BlockPoint, LayoutBlock> blocks = plan.desiredBlocks();
		var hole = M5DevelopmentCourse.definition().hole(3);

		assertEquals(LayoutBlock.GREEN_WOOL, blocks.get(new BlockPoint(-366, 69, 490)));
		for (BlockPoint sightline : List.of(
			new BlockPoint(-366, 90, 490),
			new BlockPoint(-298, 90, 462),
			new BlockPoint(-264, 90, 448),
			new BlockPoint(-231, 90, 447),
			new BlockPoint(-231, 90, 434)
		)) {
			assertTrue(plan.operations().stream().anyMatch(operation ->
				operation.volume().contains(sightline)
					&& operation.block() == LayoutBlock.AIR
					&& operation.replacementRule() == ReplacementRule.VEGETATION_ONLY));
		}
		assertTrue(!blocks.containsKey(new BlockPoint(-298, 60, 462)),
			"the natural ground below the drive corridor must remain untouched");
		assertEquals(5, hole.par());
		assertEquals(LayoutBlock.GOLF_CUP, blocks.get(new BlockPoint(-207, 71, 426)));
		assertEquals(LayoutBlock.GREEN_WOOL, blocks.get(floorBelow(hole.cup())),
			"the Hole 3 cup must be supported by the authored green");
		assertEquals(LayoutBlock.SAND, blocks.get(new BlockPoint(-219, 70, 423)));
		assertTrue(!blocks.containsKey(new BlockPoint(-215, 70, 416)),
			"the Hole 3 green should use an authored non-rectangular outline");
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
	void vegetationOnlyOperationClearsTreesButPreservesNaturalGround() {
		BlockVolume envelope = volume(0, 0, 0, 4, 4, 4);
		DevelopmentCoursePlan plan = new DevelopmentCoursePlan(
			M5DevelopmentLayout.IDENTITY, "minecraft:overworld", envelope,
			List.of(new AuthoredRegion("trees", envelope)),
			List.of(new LayoutOperation(envelope, LayoutBlock.AIR, ReplacementRule.VEGETATION_ONLY)));
		FakeWorld world = new FakeWorld();
		BlockPoint tree = new BlockPoint(2, 2, 2);
		BlockPoint ground = new BlockPoint(2, 1, 2);
		world.blocks.put(tree, LayoutBlock.GRASS_BLOCK);
		world.blocks.put(ground, LayoutBlock.DIRT);
		world.vegetation.add(tree);

		DevelopmentCourseGenerator.prepare(plan, world);

		assertEquals(LayoutBlock.AIR, world.blocks.get(tree));
		assertEquals(LayoutBlock.DIRT, world.blocks.get(ground));
		assertTrue(!world.writes.contains(ground));
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
		private final Set<BlockPoint> vegetation = new HashSet<>();

		@Override
		public boolean matchesReplacementRule(BlockPoint point, ReplacementRule replacementRule) {
			return replacementRule == ReplacementRule.ALWAYS || vegetation.contains(point);
		}

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
