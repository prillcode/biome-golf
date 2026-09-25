package pro.apdev.biomegolf.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.hole.HoleDefinition;

/**
 * Minecraft-free derivation of the M7 S1 protected zones from authored course
 * metadata.
 *
 * <p>Per the recorded M7 S1 decision (2026-09-10), protection is keyed on the
 * tee/cup vicinity only: each hole contributes one {@code tee} and one
 * {@code cup} {@link ProtectedZone} from its authored {@code HoleDefinition}
 * positions. Greens are protected through cup vicinity and tee boxes through
 * tee vicinity; no new green metadata is authored in M7.</p>
 *
 * <p>M8.10 S0 extends this with a per-position {@link ProtectionVerdict} that
 * combines the tee/cup cylinders with whole-course landscape perimeters
 * (most-restrictive-wins).</p>
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
	 * M8.10 S0: the most restrictive verdict at a block position across the
	 * tee/cup vicinity cylinders and every course landscape perimeter.
	 *
	 * <p>Tee/cup cylinders always contribute {@link ProtectionVerdict#DENY_NON_OP};
	 * a landscape perimeter contributes according to its lock state. A position
	 * inside several regions resolves to the strictest verdict, so a locked
	 * perimeter overrides an unlocked one and any cylinder.</p>
	 */
	public static ProtectionVerdict resolve(List<ProtectedZone> zones,
			List<CourseLandscape> landscapes, int x, int y, int z) {
		Objects.requireNonNull(zones, "zones");
		Objects.requireNonNull(landscapes, "landscapes");
		ProtectionVerdict verdict = ProtectionVerdict.ALLOW;
		for (ProtectedZone zone : zones) {
			if (zone.contains(x, y, z)) {
				verdict = verdict.mostRestrictive(ProtectionVerdict.DENY_NON_OP);
			}
		}
		for (CourseLandscape landscape : landscapes) {
			verdict = verdict.mostRestrictive(landscape.verdictFor(x, y, z));
		}
		return verdict;
	}

	/** Convenience overload for a course with no landscape perimeter. */
	public static ProtectionVerdict resolve(List<ProtectedZone> zones, int x, int y, int z) {
		return resolve(zones, List.of(), x, y, z);
	}

	/**
	 * M8.10 S3: the verdict used for TNT placement, ignition, and newly spawned
	 * primed TNT. Landscape perimeters are expanded by {@code blastMargin} on
	 * every axis so a charge placed just outside the boundary that could still
	 * reach protected blocks is covered; tee/cup cylinders stay exact.
	 */
	public static ProtectionVerdict resolveForTnt(List<ProtectedZone> zones,
			List<CourseLandscape> landscapes, double blastMargin, int x, int y, int z) {
		Objects.requireNonNull(zones, "zones");
		Objects.requireNonNull(landscapes, "landscapes");
		if (!(blastMargin >= 0.0) || !Double.isFinite(blastMargin)) {
			throw new IllegalArgumentException("blastMargin must be finite and >= 0: " + blastMargin);
		}
		List<CourseLandscape> expanded = new ArrayList<>(landscapes.size());
		for (CourseLandscape landscape : landscapes) {
			expanded.add(landscape.expandedBy(blastMargin));
		}
		return resolve(zones, expanded, x, y, z);
	}

	/**
	 * Pure break-policy decision used by the Fabric guard. Dev-level players are
	 * exempt; ordinary players are blocked for the cup itself or any zone member.
	 */
	public static boolean mayBreak(List<ProtectedZone> zones, int x, int y, int z,
			boolean cupBlock, boolean hasDevPermission) {
		return mayBreak(zones, List.of(), x, y, z, cupBlock, hasDevPermission);
	}

	/**
	 * M8.10 S0: full break policy across tee/cup cylinders and landscape
	 * perimeters. {@link ProtectionVerdict#DENY_ALL} denies every player,
	 * {@link ProtectionVerdict#DENY_NON_OP} denies non-operators only, and the
	 * cup block itself is always non-operator protected so it can never be
	 * destroyed by an ordinary player (operators may still repair it).
	 */
	public static boolean mayBreak(List<ProtectedZone> zones, List<CourseLandscape> landscapes,
			int x, int y, int z, boolean cupBlock, boolean hasOperatorPermission) {
		ProtectionVerdict verdict = resolve(zones, landscapes, x, y, z);
		if (verdict.denies(hasOperatorPermission)) {
			return false;
		}
		return hasOperatorPermission || !cupBlock;
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
