package com.prillcode.minecraftgolf.course;

import java.util.List;
import java.util.Objects;

/** Final three-hole scorecard calculated from authoritative terminal results. */
public record CourseScorecard(String courseId, List<HoleScore> holes) {

	public CourseScorecard {
		Objects.requireNonNull(courseId, "courseId");
		Objects.requireNonNull(holes, "holes");
		if (courseId.isBlank()) {
			throw new IllegalArgumentException("courseId must not be blank");
		}
		holes = List.copyOf(holes);
		if (holes.size() != 3) {
			throw new IllegalArgumentException("final M5 scorecard must contain exactly three holes");
		}
		for (int index = 0; index < holes.size(); index++) {
			HoleScore score = Objects.requireNonNull(holes.get(index), "holes must not contain null");
			if (score.holeNumber() != index + 1) {
				throw new IllegalArgumentException("scorecard holes must be ordered 1, 2, 3");
			}
		}
	}

	public int totalStrokes() {
		return holes.stream().mapToInt(HoleScore::strokes).sum();
	}

	public int totalPar() {
		return holes.stream().mapToInt(HoleScore::par).sum();
	}

	public int scoreToPar() {
		return totalStrokes() - totalPar();
	}
}
