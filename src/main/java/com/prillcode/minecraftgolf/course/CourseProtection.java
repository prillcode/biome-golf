package com.prillcode.minecraftgolf.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

/**
 * Minecraft-free derivation of the M7 S1 protected zones from authored course
 * metadata.
 *
 * <p>Per the recorded M7 S1 decision (2026-09-10), protection is keyed on the
 * tee/cup vicinity only: each hole contributes one {@code tee} and one
 * {@code cup} {@link ProtectedZone} from its authored {@code HoleDefinition}
 * positions. Greens are protected through cup vicinity and tee boxes through
 * tee vicinity; no new green metadata is authored in M7.</p>
 */
public final class CourseProtection {

	private CourseProtection() {
	}

	/** Builds the tee and cup vicinity zones for every hole of a course. */
	public static List<ProtectedZone> zonesFor(CourseDefinition course, CourseProtectionConfig config) {
		Objects.requireNonNull(course, "course");
		Objects.requireNonNull(config, "config");
		List<ProtectedZone> zones = new ArrayList<>(course.holes().size() * 2);
		for (HoleDefinition hole : course.holes()) {
			zones.add(teeZone(hole, config));
			zones.add(cupZone(hole, config));
		}
		return List.copyOf(zones);
	}

	/** Builds the tee and cup vicinity zones for a single authored hole. */
	public static List<ProtectedZone> zonesFor(HoleDefinition hole, CourseProtectionConfig config) {
		Objects.requireNonNull(hole, "hole");
		Objects.requireNonNull(config, "config");
		return List.of(teeZone(hole, config), cupZone(hole, config));
	}

	/** Returns whether the exact block coordinate lies inside any protected zone. */
	public static boolean isProtected(List<ProtectedZone> zones, int x, int y, int z) {
		Objects.requireNonNull(zones, "zones");
		for (ProtectedZone zone : zones) {
			if (zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/** Returns whether the position lies inside any protected zone. */
	public static boolean isProtected(List<ProtectedZone> zones, Vec3 position) {
		Objects.requireNonNull(position, "position");
		for (ProtectedZone zone : zones) {
			if (zone.contains(position)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Pure break-policy decision used by the Fabric guard. Dev-level players are
	 * exempt; ordinary players are blocked for the cup itself or any zone member.
	 */
	public static boolean mayBreak(List<ProtectedZone> zones, int x, int y, int z,
			boolean cupBlock, boolean hasDevPermission) {
		if (hasDevPermission) {
			return true;
		}
		return !cupBlock && !isProtected(zones, x, y, z);
	}

	private static ProtectedZone teeZone(HoleDefinition hole, CourseProtectionConfig config) {
		return new ProtectedZone("tee", hole.number(), hole.tee(),
			config.vicinityRadius(), config.vicinityVerticalHalfHeight());
	}

	private static ProtectedZone cupZone(HoleDefinition hole, CourseProtectionConfig config) {
		return new ProtectedZone("cup", hole.number(), hole.cup(),
			config.vicinityRadius(), config.vicinityVerticalHalfHeight());
	}
}
