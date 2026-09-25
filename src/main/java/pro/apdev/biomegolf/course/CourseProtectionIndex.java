package pro.apdev.biomegolf.course;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pro.apdev.biomegolf.hole.HoleDefinition;

/**
 * Immutable, dimension-keyed protected-region snapshot with independently
 * replaceable sources.
 *
 * <p>M8.10 S2 indexes whole-course landscape perimeters alongside the tee/cup
 * vicinity zones. Authored landscapes cover both drafts and finalized courses
 * (a draft only contributes its perimeter; finalized courses contribute zones
 * and perimeter), while the runtime configured course/hole contributes zones
 * only. Query the combined policy through {@link #verdict}.</p>
 */
public final class CourseProtectionIndex {
	private static final CourseProtectionConfig CONFIG = CourseProtectionConfig.DEFAULT;

	private List<CourseDefinition> authoredCourses = List.of();
	private List<CourseLandscape> authoredLandscapes = List.of();
	private CourseDefinition configuredCourse;
	private HoleDefinition configuredHole;
	private volatile Map<String, List<ProtectedZone>> zonesByDimension = Map.of();
	private volatile Map<String, List<CourseLandscape>> landscapesByDimension = Map.of();

	/** Replaces the authored finalized courses and their landscapes. */
	public synchronized void replaceAuthoredCourses(List<CourseDefinition> courses,
			List<CourseLandscape> landscapes) {
		authoredCourses = List.copyOf(Objects.requireNonNull(courses, "courses"));
		authoredLandscapes = List.copyOf(Objects.requireNonNull(landscapes, "landscapes"));
		rebuild();
	}

	/** Replaces the authored finalized courses, keeping no landscape perimeters. */
	public synchronized void replaceAuthoredCourses(List<CourseDefinition> courses) {
		replaceAuthoredCourses(courses, List.of());
	}

	public synchronized void replaceConfiguredCourse(CourseDefinition course) {
		configuredCourse = Objects.requireNonNull(course, "course");
		configuredHole = null;
		rebuild();
	}

	public synchronized void replaceConfiguredHole(HoleDefinition hole) {
		configuredCourse = null;
		configuredHole = Objects.requireNonNull(hole, "hole");
		rebuild();
	}

	public synchronized void clearConfigured() {
		configuredCourse = null;
		configuredHole = null;
		rebuild();
	}

	public synchronized void clear() {
		authoredCourses = List.of();
		authoredLandscapes = List.of();
		configuredCourse = null;
		configuredHole = null;
		zonesByDimension = Map.of();
		landscapesByDimension = Map.of();
	}

	public List<ProtectedZone> zones(String dimension) {
		return zonesByDimension.getOrDefault(Objects.requireNonNull(dimension, "dimension"), List.of());
	}

	/** M8.10 S2: the authored landscape perimeters that apply in a dimension. */
	public List<CourseLandscape> landscapes(String dimension) {
		return landscapesByDimension.getOrDefault(Objects.requireNonNull(dimension, "dimension"), List.of());
	}

	/** The combined protection verdict at a block position in a dimension. */
	public ProtectionVerdict verdict(String dimension, int x, int y, int z) {
		return CourseProtection.resolve(zones(dimension), landscapes(dimension), x, y, z);
	}

	/**
	 * M8.10 S3: the TNT verdict at a block position, with landscape perimeters
	 * expanded by the configured blast safety margin.
	 */
	public ProtectionVerdict tntVerdict(String dimension, int x, int y, int z) {
		return CourseProtection.resolveForTnt(zones(dimension), landscapes(dimension),
			CONFIG.tntBlastSafetyMargin(), x, y, z);
	}

	/** Whether the position is protected by any zone or landscape (any non-ALLOW verdict). */
	public boolean isProtected(String dimension, int x, int y, int z) {
		return verdict(dimension, x, y, z) != ProtectionVerdict.ALLOW;
	}

	private void rebuild() {
		Map<String, List<ProtectedZone>> mutableZones = new LinkedHashMap<>();
		for (CourseDefinition course : authoredCourses) add(mutableZones, course);
		if (configuredCourse != null) add(mutableZones, configuredCourse);
		if (configuredHole != null) add(mutableZones, configuredHole.dimension(),
			CourseProtection.zonesFor(configuredHole, CONFIG));
		Map<String, List<ProtectedZone>> immutableZones = new LinkedHashMap<>();
		mutableZones.forEach((dimension, zones) -> immutableZones.put(dimension, List.copyOf(zones)));
		zonesByDimension = Map.copyOf(immutableZones);

		Map<String, List<CourseLandscape>> mutableLandscapes = new LinkedHashMap<>();
		for (CourseLandscape landscape : authoredLandscapes) {
			mutableLandscapes.computeIfAbsent(landscape.dimension(), ignored -> new ArrayList<>())
				.add(landscape);
		}
		Map<String, List<CourseLandscape>> immutableLandscapes = new LinkedHashMap<>();
		mutableLandscapes.forEach((dimension, landscapes) ->
			immutableLandscapes.put(dimension, List.copyOf(landscapes)));
		landscapesByDimension = Map.copyOf(immutableLandscapes);
	}

	private static void add(Map<String, List<ProtectedZone>> target, CourseDefinition course) {
		add(target, course.dimension(), CourseProtection.zonesFor(course, CONFIG));
	}

	private static void add(Map<String, List<ProtectedZone>> target, String dimension,
			List<ProtectedZone> zones) {
		target.computeIfAbsent(dimension, ignored -> new ArrayList<>()).addAll(zones);
	}
}
