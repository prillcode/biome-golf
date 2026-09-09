package com.prillcode.minecraftgolf.dev;

import java.util.ArrayList;
import java.util.List;

import com.prillcode.minecraftgolf.course.GeneratedLayoutIdentity;

/** Reviewed M5 ocean-campus bounds and initial version marker pads. */
public final class M5DevelopmentLayout {

	public static final long DEVELOPMENT_SEED = -1928790872702396508L;
	public static final GeneratedLayoutIdentity IDENTITY =
		new GeneratedLayoutIdentity("minecraft_golf:m5_ocean_campus", 3);
	public static final BlockVolume CAMPUS_ENVELOPE = volume(-320, 48, -256, 448, 112, 512);

	private M5DevelopmentLayout() {
	}

	public static DevelopmentCoursePlan plan() {
		List<AuthoredRegion> regions = List.of(
			new AuthoredRegion("practice-range", volume(-304, 62, -224, -80, 96, 32)),
			new AuthoredRegion("hole-1-par-4", volume(-48, 62, -224, 208, 96, -96)),
			new AuthoredRegion("hole-2-par-3", volume(-48, 62, -64, 160, 96, 64)),
			new AuthoredRegion("hole-3-par-5", volume(-48, 62, 96, 432, 96, 496)));

		List<LayoutOperation> operations = new ArrayList<>();
		operations.addAll(practiceRange());
		// Small pads reserve the hole layout origins before S4-S6 add their geometry.
		operations.addAll(List.of(marker(-48, -224), marker(-48, -64), marker(-48, 96)));
		return new DevelopmentCoursePlan(IDENTITY, "minecraft:overworld", CAMPUS_ENVELOPE,
			regions, operations);
	}

	private static List<LayoutOperation> practiceRange() {
		List<LayoutOperation> operations = new ArrayList<>();

		// Driver lane, with readable 50/100/150-block markers and target backstop.
		operations.add(fill(-296, 63, -210, -104, 70, -194, LayoutBlock.AIR));
		operations.add(fill(-296, 62, -210, -104, 62, -194, LayoutBlock.GRASS_BLOCK));
		for (int x : List.of(-246, -196, -146)) {
			operations.add(fill(x, 62, -210, x, 62, -194, LayoutBlock.GOLD_BLOCK));
			operations.add(fill(x, 63, -202, x, 63, -202, LayoutBlock.GOLF_CUP));
		}
		operations.add(fill(-105, 63, -205, -105, 67, -199, LayoutBlock.TARGET));

		// Wedge lane and raised target face.
		operations.add(fill(-288, 63, -160, -220, 70, -128, LayoutBlock.AIR));
		operations.add(fill(-288, 62, -160, -220, 62, -128, LayoutBlock.GRASS_BLOCK));
		operations.add(fill(-232, 62, -151, -222, 62, -137, LayoutBlock.SAND));
		operations.add(fill(-223, 63, -147, -223, 66, -141, LayoutBlock.TARGET));
		operations.add(fill(-228, 63, -144, -228, 63, -144, LayoutBlock.GOLF_CUP));

		// Putting green with a physical practice cup and adjacent fairway/rough contrast.
		operations.add(fill(-160, 63, -176, -104, 68, -136, LayoutBlock.AIR));
		operations.add(fill(-160, 62, -176, -104, 62, -136, LayoutBlock.GRASS_BLOCK));
		operations.add(fill(-160, 62, -176, -154, 62, -136, LayoutBlock.DIRT));
		operations.add(fill(-112, 63, -158, -112, 63, -158, LayoutBlock.GOLF_CUP));

		// Recovery and Minecraft-native surface lanes, separated from normal turf.
		operations.add(fill(-296, 63, -96, -152, 68, -48, LayoutBlock.AIR));
		operations.add(fill(-296, 62, -96, -248, 62, -88, LayoutBlock.WATER));
		operations.add(fill(-240, 62, -96, -192, 62, -88, LayoutBlock.SAND));
		operations.add(fill(-184, 62, -96, -136, 62, -88, LayoutBlock.ICE));
		operations.add(fill(-184, 62, -72, -136, 62, -64, LayoutBlock.SLIME_BLOCK));

		return operations;
	}

	private static LayoutOperation marker(int x, int z) {
		return new LayoutOperation(volume(x, 62, z, x + 2, 62, z + 2), LayoutBlock.GRASS_BLOCK);
	}

	private static LayoutOperation fill(
		int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LayoutBlock block
	) {
		return new LayoutOperation(volume(minX, minY, minZ, maxX, maxY, maxZ), block);
	}

	private static BlockVolume volume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return new BlockVolume(new BlockPoint(minX, minY, minZ), new BlockPoint(maxX, maxY, maxZ));
	}
}
