package com.prillcode.minecraftgolf.course;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.prillcode.minecraftgolf.hole.HoleDefinition;

/** Immutable definition of one or more ordered authored holes in a single dimension. */
public record CourseDefinition(
	String id,
	String displayName,
	String dimension,
	GeneratedLayoutIdentity generatedLayout,
	List<HoleDefinition> holes
) {
	public CourseDefinition {
		requireNonBlank(id, "id");
		requireNonBlank(displayName, "displayName");
		requireNonBlank(dimension, "dimension");
		Objects.requireNonNull(generatedLayout, "generatedLayout");
		Objects.requireNonNull(holes, "holes");
		holes = List.copyOf(holes);
		if (holes.isEmpty()) {
			throw new IllegalArgumentException("course must contain at least one hole");
		}

		Set<String> ids = new HashSet<>();
		for (int index = 0; index < holes.size(); index++) {
			HoleDefinition hole = Objects.requireNonNull(holes.get(index), "holes must not contain null");
			int expectedNumber = index + 1;
			if (hole.number() != expectedNumber) {
				throw new IllegalArgumentException("holes must be ordered and numbered 1.." + holes.size()
					+ " without gaps");
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
			throw new IllegalArgumentException("hole number must be between 1 and " + holes.size());
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
