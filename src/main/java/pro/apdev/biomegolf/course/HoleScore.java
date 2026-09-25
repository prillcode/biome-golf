package pro.apdev.biomegolf.course;

import java.util.Objects;

import pro.apdev.biomegolf.hole.HoleCompletionReason;
import pro.apdev.biomegolf.hole.PlayerHoleState;

/** Final authoritative score for one course hole. */
public record HoleScore(
	String holeId,
	int holeNumber,
	int par,
	int strokes,
	int penaltyStrokes,
	HoleCompletionReason completionReason
) {
	public HoleScore {
		Objects.requireNonNull(holeId, "holeId");
		Objects.requireNonNull(completionReason, "completionReason");
		if (holeId.isBlank()) {
			throw new IllegalArgumentException("holeId must not be blank");
		}
		if (holeNumber <= 0 || par <= 0 || strokes <= 0) {
			throw new IllegalArgumentException("hole number, par, and strokes must be positive");
		}
		if (penaltyStrokes < 0 || penaltyStrokes > strokes) {
			throw new IllegalArgumentException("penalty strokes must be between zero and total strokes");
		}
	}

	public static HoleScore from(PlayerHoleState state) {
		Objects.requireNonNull(state, "state");
		if (!state.isComplete()) {
			throw new IllegalArgumentException("only a terminal hole state can become a score");
		}
		return new HoleScore(state.hole().id(), state.hole().number(), state.hole().par(),
			state.strokes(), state.penaltyStrokes(), state.completionReason());
	}

	public int scoreToPar() {
		return strokes - par;
	}
}
