package com.prillcode.minecraftgolf.course;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.prillcode.minecraftgolf.hole.HoleDefinition;

/** Immutable, dimension-keyed protected-zone snapshot with independently replaceable sources. */
public final class CourseProtectionIndex {
	private List<CourseDefinition> authoredCourses = List.of();
	private CourseDefinition configuredCourse;
	private HoleDefinition configuredHole;
	private volatile Map<String, List<ProtectedZone>> zonesByDimension = Map.of();

	public synchronized void replaceAuthoredCourses(List<CourseDefinition> courses) {
		authoredCourses = List.copyOf(Objects.requireNonNull(courses, "courses"));
		rebuild();
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
		configuredCourse = null;
		configuredHole = null;
		zonesByDimension = Map.of();
	}

	public List<ProtectedZone> zones(String dimension) {
		return zonesByDimension.getOrDefault(Objects.requireNonNull(dimension, "dimension"), List.of());
	}

	public boolean isProtected(String dimension, int x, int y, int z) {
		return CourseProtection.isProtected(zones(dimension), x, y, z);
	}

	private void rebuild() {
		Map<String, List<ProtectedZone>> mutable = new LinkedHashMap<>();
		for (CourseDefinition course : authoredCourses) add(mutable, course);
		if (configuredCourse != null) add(mutable, configuredCourse);
		if (configuredHole != null) add(mutable, configuredHole.dimension(),
			CourseProtection.zonesFor(configuredHole, CourseProtectionConfig.DEFAULT));
		Map<String, List<ProtectedZone>> immutable = new LinkedHashMap<>();
		mutable.forEach((dimension, zones) -> immutable.put(dimension, List.copyOf(zones)));
		zonesByDimension = Map.copyOf(immutable);
	}

	private static void add(Map<String, List<ProtectedZone>> target, CourseDefinition course) {
		add(target, course.dimension(), CourseProtection.zonesFor(course, CourseProtectionConfig.DEFAULT));
	}

	private static void add(Map<String, List<ProtectedZone>> target, String dimension,
			List<ProtectedZone> zones) {
		target.computeIfAbsent(dimension, ignored -> new ArrayList<>()).addAll(zones);
	}
}
