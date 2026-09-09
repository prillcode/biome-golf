package com.prillcode.minecraftgolf.dev;

import java.util.List;

import com.prillcode.minecraftgolf.course.GeneratedLayoutIdentity;

/** Reviewed M5 ocean-campus bounds and initial version marker pads. */
public final class M5DevelopmentLayout {

	public static final GeneratedLayoutIdentity IDENTITY =
		new GeneratedLayoutIdentity("minecraft_golf:m5_ocean_campus", 1);
	public static final BlockVolume CAMPUS_ENVELOPE = volume(-320, 48, -256, 448, 112, 512);

	private M5DevelopmentLayout() {
	}

	public static DevelopmentCoursePlan plan() {
		List<AuthoredRegion> regions = List.of(
			new AuthoredRegion("practice-range", volume(-304, 62, -224, -80, 96, 32)),
			new AuthoredRegion("hole-1-par-4", volume(-48, 62, -224, 208, 96, -96)),
			new AuthoredRegion("hole-2-par-3", volume(-48, 62, -64, 160, 96, 64)),
			new AuthoredRegion("hole-3-par-5", volume(-48, 62, 96, 432, 96, 496)));

		// Small pads prove the framework before S3-S6 add reviewed authored geometry.
		List<LayoutOperation> operations = List.of(
			marker(-304, -224), marker(-48, -224), marker(-48, -64), marker(-48, 96));
		return new DevelopmentCoursePlan(IDENTITY, "minecraft:overworld", CAMPUS_ENVELOPE,
			regions, operations);
	}

	private static LayoutOperation marker(int x, int z) {
		return new LayoutOperation(volume(x, 62, z, x + 2, 62, z + 2), LayoutBlock.GRASS_BLOCK);
	}

	private static BlockVolume volume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return new BlockVolume(new BlockPoint(minX, minY, minZ), new BlockPoint(maxX, maxY, maxZ));
	}
}
