package com.prillcode.minecraftgolf.hole;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable server-owned scoring state for one player on one hole.
 *
 * <p>A stroke is recorded only after the server accepts and launches a shot.
 * Each MVP penalty also adds one stroke. Reaching Double Par plus two, holing out, or
 * picking up makes the state terminal.</p>
 */
public record PlayerHoleState(
	HoleDefinition hole,
	int strokes,
	List<PenaltyType> penalties,
	HoleStatus status,
	HoleCompletionReason completionReason
) {

	public PlayerHoleState {
		Objects.requireNonNull(hole, "hole");
		Objects.requireNonNull(penalties, "penalties");
		Objects.requireNonNull(status, "status");
		penalties = List.copyOf(penalties);
		if (penalties.stream().anyMatch(Objects::isNull)) {
			throw new NullPointerException("penalties must not contain null");
		}
		if (strokes < 0 || strokes > hole.strokeLimit()) {
			throw new IllegalArgumentException("strokes must be between zero and the stroke limit");
		}
		if (penalties.size() > strokes) {
			throw new IllegalArgumentException("penalty strokes cannot exceed total strokes");
		}
		if (status == HoleStatus.IN_PROGRESS) {
			if (completionReason != null) {
				throw new IllegalArgumentException("an in-progress hole cannot have a completion reason");
			}
			if (strokes >= hole.strokeLimit()) {
				throw new IllegalArgumentException("a player at the stroke limit must be complete");
			}
		} else {
			Objects.requireNonNull(completionReason, "completionReason");
			if (completionReason == HoleCompletionReason.HOLED_OUT && strokes == 0) {
				throw new IllegalArgumentException("a hole-out requires an accepted shot");
			}
			if ((completionReason == HoleCompletionReason.STROKE_LIMIT
				|| completionReason == HoleCompletionReason.PICKED_UP)
				&& strokes != hole.strokeLimit()) {
				throw new IllegalArgumentException("stroke-limit and picked-up scores must equal Double Par plus two");
			}
		}
	}

	public static PlayerHoleState start(HoleDefinition hole) {
		return new PlayerHoleState(hole, 0, List.of(), HoleStatus.IN_PROGRESS, null);
	}

	/** Records one server-accepted and launched golf shot. */
	public PlayerHoleState recordAcceptedShot() {
		requireInProgress();
		return withAddedStroke(penalties);
	}

	/** Applies the one-stroke MVP penalty while retaining its explicit cause. */
	public PlayerHoleState applyPenalty(PenaltyType penalty) {
		requireInProgress();
		Objects.requireNonNull(penalty, "penalty");
		List<PenaltyType> updatedPenalties = new ArrayList<>(penalties);
		updatedPenalties.add(penalty);
		return withAddedStroke(updatedPenalties);
	}

	/** Finalizes the score after authoritative cup detection captures the ball. */
	public PlayerHoleState holeOut() {
		requireInProgress();
		if (strokes == 0) {
			throw new IllegalStateException("cannot hole out before an accepted shot");
		}
		return new PlayerHoleState(hole, strokes, penalties, HoleStatus.COMPLETE,
			HoleCompletionReason.HOLED_OUT);
	}

	/** Picks up the ball and assigns the configured Double Par plus two score. */
	public PlayerHoleState pickUp() {
		requireInProgress();
		return new PlayerHoleState(hole, hole.strokeLimit(), penalties, HoleStatus.COMPLETE,
			HoleCompletionReason.PICKED_UP);
	}

	public int penaltyStrokes() {
		return penalties.size();
	}

	public int scoreToPar() {
		return strokes - hole.par();
	}

	public GolfScoreTerm scoreTerm() {
		if (strokes == 0) {
			throw new IllegalStateException("no score term exists before the first stroke");
		}
		return GolfScoreTerm.forScore(strokes, hole.par());
	}

	public boolean isComplete() {
		return status == HoleStatus.COMPLETE;
	}

	public boolean canPlay() {
		return status == HoleStatus.IN_PROGRESS;
	}

	private PlayerHoleState withAddedStroke(List<PenaltyType> updatedPenalties) {
		int updatedStrokes = strokes + 1;
		if (updatedStrokes == hole.strokeLimit()) {
			return new PlayerHoleState(hole, updatedStrokes, updatedPenalties, HoleStatus.COMPLETE,
				HoleCompletionReason.STROKE_LIMIT);
		}
		return new PlayerHoleState(hole, updatedStrokes, updatedPenalties, HoleStatus.IN_PROGRESS, null);
	}

	private void requireInProgress() {
		if (isComplete()) {
			throw new IllegalStateException("hole is already complete: " + completionReason);
		}
	}
}
