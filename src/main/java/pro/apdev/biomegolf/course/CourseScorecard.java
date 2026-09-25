package pro.apdev.biomegolf.course;

import java.util.List;
import java.util.Objects;

/** Final scorecard calculated from authoritative terminal hole results. */
public record CourseScorecard(String courseId, List<HoleScore> holes) {

	public CourseScorecard {
		Objects.requireNonNull(courseId, "courseId");
		Objects.requireNonNull(holes, "holes");
		if (courseId.isBlank()) {
			throw new IllegalArgumentException("courseId must not be blank");
		}
		holes = List.copyOf(holes);
		if (holes.isEmpty()) {
			throw new IllegalArgumentException("final scorecard must contain at least one hole");
		}
		int previousHoleNumber = 0;
		for (int index = 0; index < holes.size(); index++) {
			HoleScore score = Objects.requireNonNull(holes.get(index), "holes must not contain null");
			if (score.holeNumber() != previousHoleNumber + 1 && previousHoleNumber != 0) {
				throw new IllegalArgumentException(
					"scorecard holes must use consecutive authored hole numbers");
			}
			previousHoleNumber = score.holeNumber();
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
