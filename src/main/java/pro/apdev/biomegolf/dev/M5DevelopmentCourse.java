package pro.apdev.biomegolf.dev;

import java.util.List;

import pro.apdev.biomegolf.course.CourseDefinition;
import pro.apdev.biomegolf.course.GeneratedLayoutIdentity;
import pro.apdev.biomegolf.course.HoleTransition;
import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.hole.HoleBoundary;
import pro.apdev.biomegolf.hole.HoleDefinition;

/** Authoritative metadata matching the three reviewed M5 generated footprints. */
public final class M5DevelopmentCourse {

	private M5DevelopmentCourse() {
	}

	private static final List<Integer> REQUIRED_PARS = List.of(4, 3, 5);

	public static CourseDefinition definition() {
		CourseDefinition definition = build();
		if (definition.holes().size() != REQUIRED_PARS.size()) {
			throw new IllegalStateException("M5 development course must contain exactly three holes");
		}
		for (int index = 0; index < REQUIRED_PARS.size(); index++) {
			if (definition.holes().get(index).par() != REQUIRED_PARS.get(index)) {
				throw new IllegalStateException(
					"M5 development course holes must be par 4, par 3, par 5 in order");
			}
		}
		return definition;
	}

	private static CourseDefinition build() {
		return new CourseDefinition(
			"minecraft_golf:m5_development",
			"M5 Development Course",
			"minecraft:overworld",
			M5DevelopmentLayout.IDENTITY,
			List.of(
				hole("minecraft_golf:m5_hole_1", 1, 4,
					new Vec3(-206.5, 75.25, 494.5), new Vec3(-354.5, 71.25, 416.5),
					volume(-384, 48, 384, -176, 128, 528), 118.0),
				hole("minecraft_golf:m5_hole_2", 2, 3,
					new Vec3(-367.5, 71.25, 416.5), new Vec3(-321.5, 71.25, 405.5),
					volume(-384, 48, 384, -256, 128, 464), -103.0),
				hole("minecraft_golf:m5_hole_3", 3, 5,
					new Vec3(-365.5, 70.25, 490.5), new Vec3(-206.5, 71.25, 426.5),
					volume(-400, 32, 384, -176, 128, 528), -112.0)));
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
