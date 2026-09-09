package com.prillcode.minecraftgolf.course;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.prillcode.minecraftgolf.hole.HoleDefinition;

/** Immutable M5 definition of exactly three ordered authored holes. */
public record CourseDefinition(
	String id,
	String displayName,
	String dimension,
	GeneratedLayoutIdentity generatedLayout,
	List<HoleDefinition> holes
) {
	private static final List<Integer> REQUIRED_PARS = List.of(3, 4, 5);

	public CourseDefinition {
		requireNonBlank(id, "id");
		requireNonBlank(displayName, "displayName");
		requireNonBlank(dimension, "dimension");
		Objects.requireNonNull(generatedLayout, "generatedLayout");
		Objects.requireNonNull(holes, "holes");
		holes = List.copyOf(holes);
		if (holes.size() != 3) {
			throw new IllegalArgumentException("M5 course must contain exactly three holes");
		}

		Set<String> ids = new HashSet<>();
		for (int index = 0; index < holes.size(); index++) {
			HoleDefinition hole = Objects.requireNonNull(holes.get(index), "holes must not contain null");
			int expectedNumber = index + 1;
			if (hole.number() != expectedNumber) {
				throw new IllegalArgumentException("holes must be ordered and numbered 1, 2, 3");
			}
			if (hole.par() != REQUIRED_PARS.get(index)) {
				throw new IllegalArgumentException("M5 course holes must be par 3, par 4, par 5 in order");
			}
			if (!dimension.equals(hole.dimension())) {
				throw new IllegalArgumentException("every hole must use the course dimension");
			}
			if (!ids.add(hole.id())) {
				throw new IllegalArgumentException("hole ids must be unique");
			}
		}
	}

	public HoleDefinition hole(int number) {
		if (number < 1 || number > holes.size()) {
			throw new IllegalArgumentException("hole number must be between 1 and 3");
		}
		return holes.get(number - 1);
	}

	public int totalPar() {
		return holes.stream().mapToInt(HoleDefinition::par).sum();
	}

	private static void requireNonBlank(String value, String name) {
		Objects.requireNonNull(value, name);
		if (value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
	}
}
