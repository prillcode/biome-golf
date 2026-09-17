package com.prillcode.minecraftgolf.hole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;

class PlayerHoleStateTest {

	private static final HoleDefinition PAR_FOUR = new HoleDefinition(
		"family_test:1",
		1,
		"minecraft:overworld",
		new Vec3(10.0, 64.0, 10.0),
		new Vec3(90.0, 64.0, 90.0),
		4,
		new HoleBoundary(Vec3.ZERO, new Vec3(100.0, 100.0, 100.0))
	);

	@Test
	void acceptedShotsCountAndHoleOutFinalizesScore() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR)
			.recordAcceptedShot()
			.recordAcceptedShot()
			.recordAcceptedShot()
			.holeOut();

		assertEquals(3, state.strokes());
		assertEquals(3, state.acceptedShots());
		assertEquals(-1, state.scoreToPar());
		assertEquals(GolfScoreTerm.BIRDIE, state.scoreTerm());
		assertEquals(HoleStatus.COMPLETE, state.status());
		assertEquals(HoleCompletionReason.HOLED_OUT, state.completionReason());
		assertTrue(state.penalties().isEmpty());
	}

	@Test
	void penaltiesAreExplicitStrokesAndCanReachDoubleParPlusTwo() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR);
		for (int i = 0; i < 8; i++) {
			state = state.recordAcceptedShot();
		}
		state = state.applyPenalty(PenaltyType.WATER);
		state = state.applyPenalty(PenaltyType.OUT_OF_BOUNDS);

		assertEquals(10, state.strokes());
		assertEquals(8, state.acceptedShots());
		assertEquals(2, state.penaltyStrokes());
		assertEquals(PenaltyType.WATER, state.penalties().get(0));
		assertEquals(PenaltyType.OUT_OF_BOUNDS, state.penalties().get(1));
		assertEquals(HoleCompletionReason.STROKE_LIMIT, state.completionReason());
		assertTrue(state.isComplete());
		assertThrows(IllegalStateException.class, state::recordAcceptedShot);
	}

	@Test
	void acceptedShotAtDoubleParPlusTwoCompletesHole() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR);
		for (int i = 0; i < PAR_FOUR.strokeLimit(); i++) {
			state = state.recordAcceptedShot();
		}

		assertEquals(10, state.strokes());
		assertEquals(10, state.acceptedShots());
		assertEquals(HoleCompletionReason.STROKE_LIMIT, state.completionReason());
		assertFalse(state.canPlay());
	}

	@Test
	void pickUpAssignsDoubleParPlusTwoAndCompletesHole() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR)
			.recordAcceptedShot()
			.recordAcceptedShot()
			.pickUp();

		assertEquals(10, state.strokes());
		assertEquals(2, state.acceptedShots());
		assertEquals(6, state.scoreToPar());
		assertEquals(GolfScoreTerm.OVER_PAR, state.scoreTerm());
		assertEquals(HoleCompletionReason.PICKED_UP, state.completionReason());
	}

	@Test
	void constructorRejectsUnaccountedStrokesAndHoleOutWithoutAcceptedShot() {
		assertThrows(IllegalArgumentException.class, () -> new PlayerHoleState(PAR_FOUR, 2, 1,
			java.util.List.of(), HoleStatus.IN_PROGRESS, null));
		assertThrows(IllegalArgumentException.class, () -> new PlayerHoleState(PAR_FOUR, 1, 0,
			java.util.List.of(PenaltyType.WATER), HoleStatus.COMPLETE, HoleCompletionReason.HOLED_OUT));
	}

	@Test
	void unfinishedStateCannotHoleOutOrReportATermBeforeAnyShot() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR);
		assertThrows(IllegalStateException.class, state::holeOut);
		assertThrows(IllegalStateException.class, state::scoreTerm);
		assertEquals(0, state.strokes());
		assertEquals(HoleStatus.IN_PROGRESS, state.status());
	}

	@Test
	void penaltyHistoryIsImmutable() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR)
			.recordAcceptedShot()
			.applyPenalty(PenaltyType.WATER);

		assertThrows(UnsupportedOperationException.class,
			() -> state.penalties().add(PenaltyType.OUT_OF_BOUNDS));
	}

	@Test
	void terminologyCoversGolfScoresAndAdditionalOverParResults() {
		assertEquals(GolfScoreTerm.HOLE_IN_ONE, GolfScoreTerm.forScore(1, 3));
		assertEquals(GolfScoreTerm.ALBATROSS, GolfScoreTerm.forScore(2, 5));
		assertEquals(GolfScoreTerm.EAGLE, GolfScoreTerm.forScore(3, 5));
		assertEquals(GolfScoreTerm.BIRDIE, GolfScoreTerm.forScore(3, 4));
		assertEquals(GolfScoreTerm.PAR, GolfScoreTerm.forScore(4, 4));
		assertEquals(GolfScoreTerm.BOGEY, GolfScoreTerm.forScore(5, 4));
		assertEquals(GolfScoreTerm.DOUBLE_BOGEY, GolfScoreTerm.forScore(6, 4));
		assertEquals(GolfScoreTerm.OVER_PAR, GolfScoreTerm.forScore(7, 4));
		assertThrows(IllegalArgumentException.class, () -> GolfScoreTerm.forScore(0, 4));
		assertThrows(IllegalArgumentException.class, () -> GolfScoreTerm.forScore(1, 0));
	}
}
