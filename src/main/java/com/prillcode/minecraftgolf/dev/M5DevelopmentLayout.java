package com.prillcode.minecraftgolf.dev;

import java.util.ArrayList;
import java.util.List;

import com.prillcode.minecraftgolf.course.GeneratedLayoutIdentity;

/** Reviewed M5 ocean-campus bounds and initial version marker pads. */
public final class M5DevelopmentLayout {

	public static final long DEVELOPMENT_SEED = -1928790872702396508L;
	public static final GeneratedLayoutIdentity IDENTITY =
		new GeneratedLayoutIdentity("minecraft_golf:m5_ocean_campus", 11);
	public static final BlockVolume CAMPUS_ENVELOPE = volume(-640, 32, -256, 448, 192, 640);

	private M5DevelopmentLayout() {
	}

	public static DevelopmentCoursePlan plan() {
		List<AuthoredRegion> regions = List.of(
			new AuthoredRegion("practice-range", volume(-304, 62, -224, -80, 96, 32)),
			new AuthoredRegion("legacy-flat-hole-1-cleanup", volume(-48, 62, -224, 208, 96, -96)),
			new AuthoredRegion("legacy-flat-hole-2-cleanup", volume(-48, 62, -64, 160, 96, 64)),
			new AuthoredRegion("natural-hole-1-par-4", volume(-384, 48, 384, -176, 128, 528)),
			new AuthoredRegion("natural-hole-2-par-3", volume(-384, 48, 384, -256, 128, 464)),
			new AuthoredRegion("natural-hole-3-par-5", volume(-400, 32, 384, -176, 128, 528)));

		List<LayoutOperation> operations = new ArrayList<>();
		operations.addAll(practiceRange());
		operations.addAll(legacyFlatCleanup());
		operations.addAll(holeOne());
		operations.addAll(holeTwo());
		operations.addAll(holeThree());
		operations.addAll(teeBoxesAndMarkers());
		return new DevelopmentCoursePlan(IDENTITY, "minecraft:overworld", CAMPUS_ENVELOPE,
			regions, operations);
	}

	private static List<LayoutOperation> holeOne() {
		List<LayoutOperation> operations = new ArrayList<>();
		// Selectively open a winding wooded fairway without changing its natural ground.
		for (int[] center : List.of(
			new int[] {-207, 494}, new int[] {-223, 485}, new int[] {-240, 477},
			new int[] {-256, 468}, new int[] {-273, 459}, new int[] {-289, 451},
			new int[] {-306, 442}, new int[] {-322, 433}, new int[] {-339, 425},
			new int[] {-355, 416}
		)) {
			operations.add(clearVegetation(center[0] - 10, 68, center[1] - 8,
				center[0] + 10, 105, center[1] + 8));
		}

		// Minimal level overlays at the tee and green; all intervening terrain remains natural.
		operations.add(fill(-210, 75, 491, -204, 80, 497, LayoutBlock.AIR));
		operations.add(fill(-360, 71, 411, -350, 78, 421, LayoutBlock.AIR));
		operations.add(fill(-355, 70, 411, -355, 70, 411, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-357, 70, 412, -353, 70, 412, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-359, 70, 413, -352, 70, 413, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-360, 70, 414, -350, 70, 418, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-359, 70, 419, -351, 70, 419, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-357, 70, 420, -352, 70, 420, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-355, 70, 421, -354, 70, 421, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-355, 71, 416, -355, 71, 416, LayoutBlock.GOLF_CUP));
		operations.add(fill(-355, 72, 416, -355, 72, 416, LayoutBlock.GOLF_FLAG));
		operations.add(fill(-355, 73, 416, -355, 73, 416, LayoutBlock.GOLF_FLAG_TOP));

		// A visible striped gate warns that the natural ravine crosses driver range.
		operations.add(clearVegetation(-276, 68, 437, -272, 78, 441));
		operations.add(clearVegetation(-288, 68, 461, -284, 78, 465));
		for (int y = 69; y <= 74; y++) {
			LayoutBlock stripe = y % 2 == 0
				? LayoutBlock.YELLOW_CONCRETE
				: LayoutBlock.BLACK_CONCRETE;
			operations.add(fill(-274, y, 439, -274, y, 439, stripe));
			operations.add(fill(-286, y, 463, -286, y, 463, stripe));
		}
		return operations;
	}

	private static List<LayoutOperation> holeThree() {
		List<LayoutOperation> operations = new ArrayList<>();
		// Open the tee and first-drive sightline; the natural ground remains untouched.
		for (int[] center : List.of(
			new int[] {-366, 490}, new int[] {-349, 483}, new int[] {-332, 476},
			new int[] {-315, 469}, new int[] {-298, 462}, new int[] {-281, 455},
			new int[] {-264, 448}
		)) {
			operations.add(clearVegetation(center[0] - 10, 68, center[1] - 8,
				center[0] + 10, 110, center[1] + 8));
		}

		// After the drive landing area, two wooded approach corridors wrap a retained
		// natural center island: the north route is wider, while the direct south route
		// challenges the greenside bunker.
		for (int[] center : List.of(
			new int[] {-248, 454}, new int[] {-231, 447}, new int[] {-215, 438}
		)) {
			operations.add(clearVegetation(center[0] - 9, 68, center[1] - 7,
				center[0] + 9, 110, center[1] + 7));
		}
		for (int[] center : List.of(
			new int[] {-248, 440}, new int[] {-231, 434}, new int[] {-215, 428}
		)) {
			operations.add(clearVegetation(center[0] - 7, 68, center[1] - 6,
				center[0] + 7, 110, center[1] + 6));
		}

		// A compact bunker guards the direct line into the irregular natural green.
		operations.add(fill(-222, 71, 418, -211, 80, 429, LayoutBlock.AIR));
		operations.add(fill(-221, 70, 420, -216, 70, 427, LayoutBlock.SAND));
		operations.add(fill(-215, 70, 422, -213, 70, 426, LayoutBlock.SAND));

		operations.add(fill(-216, 71, 416, -196, 82, 438, LayoutBlock.AIR));
		operations.add(fill(-209, 70, 418, -204, 70, 418, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-212, 70, 419, -201, 70, 420, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-214, 70, 421, -199, 70, 431, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-212, 70, 432, -201, 70, 434, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-209, 70, 435, -204, 70, 436, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-207, 71, 426, -207, 71, 426, LayoutBlock.GOLF_CUP));
		operations.add(fill(-207, 72, 426, -207, 72, 426, LayoutBlock.GOLF_FLAG));
		operations.add(fill(-207, 73, 426, -207, 73, 426, LayoutBlock.GOLF_FLAG_TOP));
		return operations;
	}

	private static List<LayoutOperation> holeTwo() {
		List<LayoutOperation> operations = new ArrayList<>();
		// A concise wooded carry corridor keeps the ravine as the central par-3 decision.
		for (int[] center : List.of(
			new int[] {-368, 416}, new int[] {-356, 414}, new int[] {-345, 412},
			new int[] {-333, 408}, new int[] {-322, 405}
		)) {
			operations.add(clearVegetation(center[0] - 8, 68, center[1] - 8,
				center[0] + 8, 105, center[1] + 8));
		}

		// Remove the obsolete canopy cup created by earlier metadata-only playtests.
		operations.add(fill(-280, 70, 433, -280, 70, 433, LayoutBlock.AIR));

		// Small irregular green centered on the reviewed stable-ground location.
		operations.add(fill(-330, 71, 398, -314, 82, 413, LayoutBlock.AIR));
		operations.add(fill(-323, 70, 400, -321, 70, 400, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-326, 70, 401, -319, 70, 401, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-328, 70, 402, -317, 70, 402, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-329, 70, 403, -315, 70, 408, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-328, 70, 409, -316, 70, 409, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-326, 70, 410, -318, 70, 410, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-324, 70, 411, -320, 70, 411, LayoutBlock.GREEN_WOOL));
		operations.add(fill(-322, 71, 405, -322, 71, 405, LayoutBlock.GOLF_CUP));
		operations.add(fill(-322, 72, 405, -322, 72, 405, LayoutBlock.GOLF_FLAG));
		operations.add(fill(-322, 73, 405, -322, 73, 405, LayoutBlock.GOLF_FLAG_TOP));
		return operations;
	}

	private static List<LayoutOperation> teeBoxesAndMarkers() {
		return List.of(
			fill(-210, 74, 491, -204, 74, 497, LayoutBlock.GREEN_WOOL),
			fill(-371, 70, 413, -364, 70, 420, LayoutBlock.GREEN_WOOL),
			fill(-371, 69, 486, -363, 69, 495, LayoutBlock.GREEN_WOOL),
			// Hole 1: markers flank the southwest-facing tee line.
			fill(-205, 74, 491, -205, 74, 491, LayoutBlock.BLUE_CONCRETE),
			fill(-209, 74, 497, -209, 74, 497, LayoutBlock.BLUE_CONCRETE),
			// Hole 2: markers flank the east-facing tee line.
			fill(-369, 70, 413, -369, 70, 413, LayoutBlock.BLUE_CONCRETE),
			fill(-369, 70, 420, -369, 70, 420, LayoutBlock.BLUE_CONCRETE),
			// Hole 3: markers flank the northeast-facing tee line.
			fill(-368, 69, 494, -368, 69, 494, LayoutBlock.BLUE_CONCRETE),
			fill(-370, 69, 488, -370, 69, 488, LayoutBlock.BLUE_CONCRETE));
	}

	private static List<LayoutOperation> legacyFlatCleanup() {
		return List.of(
			fill(-40, 63, -190, 200, 82, -130, LayoutBlock.AIR),
			fill(-40, 62, -190, 200, 62, -130, LayoutBlock.WATER),
			fill(-32, 63, -32, 72, 82, 32, LayoutBlock.AIR),
			fill(-32, 62, -32, 72, 62, 32, LayoutBlock.WATER));
	}

	private static List<LayoutOperation> practiceRange() {
		List<LayoutOperation> operations = new ArrayList<>();

		// Driver lane, with readable 50/100/150-block markers and target backstop.
		operations.add(fill(-296, 63, -210, -104, 70, -194, LayoutBlock.AIR));
		operations.add(fill(-296, 62, -210, -104, 62, -194, LayoutBlock.GRASS_BLOCK));
		for (int x : List.of(-246, -196, -146)) {
			operations.add(fill(x, 62, -210, x, 62, -194, LayoutBlock.GOLD_BLOCK));
			operations.add(fill(x, 63, -202, x, 63, -202, LayoutBlock.GOLF_CUP));
			operations.add(fill(x, 64, -202, x, 64, -202, LayoutBlock.GOLF_FLAG));
			operations.add(fill(x, 65, -202, x, 65, -202, LayoutBlock.GOLF_FLAG_TOP));
		}
		operations.add(fill(-105, 63, -205, -105, 67, -199, LayoutBlock.TARGET));

		// Wedge lane and raised target face.
		operations.add(fill(-288, 63, -160, -220, 70, -128, LayoutBlock.AIR));
		operations.add(fill(-288, 62, -160, -220, 62, -128, LayoutBlock.GRASS_BLOCK));
		operations.add(fill(-232, 62, -151, -222, 62, -137, LayoutBlock.SAND));
		operations.add(fill(-223, 63, -147, -223, 66, -141, LayoutBlock.TARGET));
		operations.add(fill(-228, 63, -144, -228, 63, -144, LayoutBlock.GOLF_CUP));
		operations.add(fill(-228, 64, -144, -228, 64, -144, LayoutBlock.GOLF_FLAG));
		operations.add(fill(-228, 65, -144, -228, 65, -144, LayoutBlock.GOLF_FLAG_TOP));

		// Putting green with a physical practice cup and adjacent fairway/rough contrast.
		operations.add(fill(-160, 63, -176, -104, 68, -136, LayoutBlock.AIR));
		operations.add(fill(-160, 62, -176, -104, 62, -136, LayoutBlock.GRASS_BLOCK));
		operations.add(fill(-160, 62, -176, -154, 62, -136, LayoutBlock.DIRT));
		operations.add(fill(-112, 63, -158, -112, 63, -158, LayoutBlock.GOLF_CUP));
		operations.add(fill(-112, 64, -158, -112, 64, -158, LayoutBlock.GOLF_FLAG));
		operations.add(fill(-112, 65, -158, -112, 65, -158, LayoutBlock.GOLF_FLAG_TOP));

		// Recovery and Minecraft-native surface lanes, separated from normal turf.
		operations.add(fill(-296, 63, -96, -152, 68, -48, LayoutBlock.AIR));
		operations.add(fill(-296, 62, -96, -248, 62, -88, LayoutBlock.WATER));
		operations.add(fill(-240, 62, -96, -192, 62, -88, LayoutBlock.SAND));
		operations.add(fill(-184, 62, -96, -136, 62, -88, LayoutBlock.ICE));
		operations.add(fill(-184, 62, -72, -136, 62, -64, LayoutBlock.SLIME_BLOCK));

		return operations;
	}

	private static LayoutOperation fill(
		int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LayoutBlock block
	) {
		return new LayoutOperation(volume(minX, minY, minZ, maxX, maxY, maxZ), block);
	}

	private static LayoutOperation clearVegetation(
		int minX, int minY, int minZ, int maxX, int maxY, int maxZ
	) {
		return new LayoutOperation(volume(minX, minY, minZ, maxX, maxY, maxZ),
			LayoutBlock.AIR, ReplacementRule.VEGETATION_ONLY);
	}

	private static BlockVolume volume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return new BlockVolume(new BlockPoint(minX, minY, minZ), new BlockPoint(maxX, maxY, maxZ));
	}
}
