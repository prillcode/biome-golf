package com.prillcode.minecraftgolf.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.prillcode.minecraftgolf.hole.PlayerHoleState;

/** Immutable server-owned single-player progress through the M5 course. */
public record PlayerCourseState(
	CourseDefinition course,
	int currentHoleIndex,
	List<PlayerHoleState> completedHoles,
	PlayerHoleState currentHole,
	CourseStatus status
) {
	public PlayerCourseState {
		Objects.requireNonNull(course, "course");
		Objects.requireNonNull(completedHoles, "completedHoles");
		Objects.requireNonNull(status, "status");
		completedHoles = List.copyOf(completedHoles);
		validateCompletedHoles(course, completedHoles);

		if (status == CourseStatus.IN_PROGRESS) {
			Objects.requireNonNull(currentHole, "currentHole");
			if (currentHoleIndex < 0 || currentHoleIndex >= course.holes().size()) {
				throw new IllegalArgumentException("in-progress current hole index is out of range");
			}
			if (completedHoles.size() != currentHoleIndex) {
				throw new IllegalArgumentException("completed holes must precede the current hole");
			}
			if (!course.holes().get(currentHoleIndex).equals(currentHole.hole())) {
				throw new IllegalArgumentException("current hole state does not match course order");
			}
		} else {
			if (currentHole != null || currentHoleIndex != course.holes().size()
					|| completedHoles.size() != course.holes().size()) {
				throw new IllegalArgumentException("complete course must contain all final hole results");
			}
		}
	}

	public static PlayerCourseState start(CourseDefinition course) {
		Objects.requireNonNull(course, "course");
		return new PlayerCourseState(course, 0, List.of(),
			PlayerHoleState.start(course.holes().getFirst()), CourseStatus.IN_PROGRESS);
	}

	/** Replaces the current hole state after a server-owned shot, penalty, or completion. */
	public PlayerCourseState updateCurrentHole(PlayerHoleState updated) {
		requireInProgress();
		Objects.requireNonNull(updated, "updated");
		if (!currentHole.hole().equals(updated.hole())) {
			throw new IllegalArgumentException("updated state must belong to the current hole");
		}
		if (currentHole.isComplete()) {
			throw new IllegalStateException("terminal current hole must advance before further updates");
		}
		if (updated.strokes() < currentHole.strokes()) {
			throw new IllegalArgumentException("updated hole state cannot reduce strokes");
		}
		return new PlayerCourseState(course, currentHoleIndex, completedHoles, updated, status);
	}

	/** Advances only from a terminal hole, completing the course after Hole 3. */
	public PlayerCourseState advance() {
		requireInProgress();
		if (!currentHole.isComplete()) {
			throw new IllegalStateException("current hole must be terminal before advancing");
		}
		List<PlayerHoleState> updatedCompleted = new ArrayList<>(completedHoles);
		updatedCompleted.add(currentHole);
		int nextIndex = currentHoleIndex + 1;
		if (nextIndex == course.holes().size()) {
			return new PlayerCourseState(course, nextIndex, updatedCompleted, null, CourseStatus.COMPLETE);
		}
		return new PlayerCourseState(course, nextIndex, updatedCompleted,
			PlayerHoleState.start(course.holes().get(nextIndex)), CourseStatus.IN_PROGRESS);
	}

	public PlayerCourseState reset() {
		return start(course);
	}

	/** Restarts only the current hole while preserving earlier finalized scores. */
	public PlayerCourseState restartCurrentHole() {
		requireInProgress();
		return new PlayerCourseState(course, currentHoleIndex, completedHoles,
			PlayerHoleState.start(course.holes().get(currentHoleIndex)), CourseStatus.IN_PROGRESS);
	}

	public boolean isComplete() {
		return status == CourseStatus.COMPLETE;
	}

	/** True when the last authored hole is terminal but not yet folded into the scorecard. */
	public boolean isFinalHoleTerminal() {
		return !isComplete()
			&& currentHoleIndex == course.holes().size() - 1
			&& currentHole.isComplete();
	}

	public int completedStrokes() {
		return completedHoles.stream().mapToInt(PlayerHoleState::strokes).sum();
	}

	public int completedPar() {
		return completedHoles.stream().mapToInt(state -> state.hole().par()).sum();
	}

	public int completedScoreToPar() {
		return completedStrokes() - completedPar();
	}

	public CourseScorecard finalScorecard() {
		if (!isComplete()) {
			throw new IllegalStateException("final scorecard is available only after course completion");
		}
		return new CourseScorecard(course.id(), completedHoles.stream().map(HoleScore::from).toList());
	}

	private void requireInProgress() {
		if (isComplete()) {
			throw new IllegalStateException("course is already complete");
		}
	}

	private static void validateCompletedHoles(CourseDefinition course, List<PlayerHoleState> completed) {
		if (completed.size() > course.holes().size()) {
			throw new IllegalArgumentException("too many completed hole states");
		}
		for (int index = 0; index < completed.size(); index++) {
			PlayerHoleState state = Objects.requireNonNull(completed.get(index),
				"completedHoles must not contain null");
			if (!state.isComplete()) {
				throw new IllegalArgumentException("completed hole list must contain only terminal states");
			}
			if (!course.holes().get(index).equals(state.hole())) {
				throw new IllegalArgumentException("completed holes must match course order");
			}
		}
	}
}
