package com.prillcode.minecraftgolf.dev;

import java.util.List;

import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.GeneratedLayoutIdentity;
import com.prillcode.minecraftgolf.course.HoleTransition;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

/** Authoritative metadata matching the three reviewed M5 generated footprints. */
public final class M5DevelopmentCourse {

	private M5DevelopmentCourse() {
	}

	public static CourseDefinition definition() {
		return new CourseDefinition(
			"minecraft_golf:m5_development",
			"M5 Development Course",
			"minecraft:overworld",
			M5DevelopmentLayout.IDENTITY,
			List.of(
				hole("minecraft_golf:m5_hole_1", 1, 4,
					new Vec3(-31.5, 63.25, -159.5), new Vec3(184.5, 63.25, -159.5),
					volume(-48, 48, -224, 208, 112, -96), 0.0),
				hole("minecraft_golf:m5_hole_2", 2, 3,
					new Vec3(-15.5, 63.25, 0.5), new Vec3(48.5, 63.25, 0.5),
					volume(-48, 48, -64, 160, 112, 64), 0.0),
				hole("minecraft_golf:m5_hole_3", 3, 5,
					new Vec3(-31.5, 63.25, 144.5), new Vec3(400.5, 71.25, 448.5),
					volume(-48, 48, 96, 432, 112, 496), -35.0)));
	}

	private static HoleDefinition hole(
		String id, int number, int par, Vec3 tee, Vec3 cup, HoleBoundary boundary, double yaw
	) {
		return new HoleDefinition(id, number, "minecraft:overworld", tee, cup, par, boundary,
			new GeneratedLayoutIdentity(id + "_layout", 1),
			new HoleTransition(new Vec3(tee.x(), tee.y() + 1.0, tee.z() + 2.0), yaw, 0.0));
	}

	private static HoleBoundary volume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return new HoleBoundary(new Vec3(minX, minY, minZ), new Vec3(maxX, maxY, maxZ));
	}
}
