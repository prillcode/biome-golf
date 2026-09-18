package com.prillcode.minecraftgolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.GolfScoreTerm;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleCompletionReason;
import com.prillcode.minecraftgolf.hole.HoleDefinition;
import com.prillcode.minecraftgolf.hole.PlayerHoleState;
import com.prillcode.minecraftgolf.net.HoleStatePayload.Phase;

class HoleStatePayloadTest {

	private static final HoleDefinition PAR_FOUR = new HoleDefinition(
		"test:1", 1, "minecraft:overworld",
		new Vec3(10.0, 64.0, 10.0),
		new Vec3(90.0, 64.0, 90.0),
		4,
		new HoleBoundary(Vec3.ZERO, new Vec3(200.0, 200.0, 200.0)));

	@Test
	void practiceSnapshot_hasCorrectPhaseAndHoleMetadata() {
		HoleStatePayload p = HoleStatePayload.practice(PAR_FOUR);
		assertEquals("minecraft_golf:hole_state_v5", p.type().id().toString());
		assertEquals(Phase.PRACTICE, p.phase());
		assertEquals(1, p.holeNumber());
		assertEquals(4, p.par());
		assertEquals(10, p.strokeLimit());
		assertEquals(0, p.strokes());
		assertEquals(0, p.penaltyCount());
		assertEquals(0, p.scoreToPar());
		assertEquals(90.0, p.cupX());
		assertEquals(90.0, p.cupZ());
		assertEquals(-1, p.distanceToCupBlocks());
		assertNull(p.completionReason());
		assertNull(p.scoreTerm());
	}

	@Test
	void noCourseSnapshotIsExplicitAndActionableByPresentation() {
		HoleStatePayload p = HoleStatePayload.noCourse();

		assertEquals(Phase.PRACTICE, p.phase());
		assertEquals(0, p.holeNumber());
		assertEquals(0, p.par());
		assertEquals(0, p.strokeLimit());
		assertFalse(p.roundAdvanceAvailable());
		assertNull(p.completionReason());
		assertNull(p.scoreTerm());
	}

	@Test
	void activeSnapshot_propagatesStrokesAndPenalties() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR)
			.recordAcceptedShot()
			.recordAcceptedShot();

		HoleStatePayload p = HoleStatePayload.active(state);
		assertEquals(Phase.ACTIVE, p.phase());
		assertEquals(2, p.strokes());
		assertEquals(2, p.acceptedShots());
		assertEquals(0, p.penaltyCount());
		assertEquals(0, p.scoreToPar());
		assertEquals(113, p.distanceToCupBlocks());
		assertNull(p.completionReason());
		assertNull(p.scoreTerm());
	}

	@Test
	void activeSnapshot_measuresHorizontalDistanceFromAuthoritativeBallPosition() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR);

		HoleStatePayload p = HoleStatePayload.active(state, new Vec3(87.0, 20.0, 86.0));

		assertEquals(5, p.distanceToCupBlocks());
	}

	@Test
	void courseTotalsAreAuthoritativeSnapshotValues() {
		HoleStatePayload p = HoleStatePayload.active(PlayerHoleState.start(PAR_FOUR))
			.withCourseTotals(7, 7, 12);

		assertEquals(7, p.courseStrokes());
		assertEquals(7, p.courseParPlayed());
		assertEquals(12, p.courseTotalPar());
	}

	@Test
	void roundAdvanceAvailabilityIsExplicitAndPreservedWithCourseTotals() {
		assertFalse(HoleStatePayload.active(PlayerHoleState.start(PAR_FOUR))
			.roundAdvanceAvailable());
		HoleStatePayload p = HoleStatePayload.complete(
			PlayerHoleState.start(PAR_FOUR).recordAcceptedShot().pickUp())
			.withRoundAdvanceAvailable(true)
			.withCourseTotals(7, 4, 12);

		assertTrue(p.roundAdvanceAvailable());
		assertEquals(7, p.courseStrokes());
	}

	@Test
	void activeSnapshot_zeroStrokesYieldsZeroScoreToPar() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR);
		HoleStatePayload p = HoleStatePayload.active(state);
		assertEquals(0, p.strokes());
		assertEquals(0, p.scoreToPar());
	}

	@Test
	void activeSnapshot_penaltiesAreIncluded() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR)
			.recordAcceptedShot()
			.applyPenalty(com.prillcode.minecraftgolf.hole.PenaltyType.OUT_OF_BOUNDS);

		HoleStatePayload p = HoleStatePayload.active(state);
		assertEquals(2, p.strokes());
		assertEquals(1, p.acceptedShots());
		assertEquals(1, p.penaltyCount());
	}

	@Test
	void missingBallSnapshot_hasCorrectPhase() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR).recordAcceptedShot();
		HoleStatePayload p = HoleStatePayload.missingBall(state);
		assertEquals(Phase.MISSING_BALL, p.phase());
		assertEquals(1, p.strokes());
		assertNull(p.completionReason());
	}

	@Test
	void completeSnapshot_holeOut_includesTermAndReason() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR)
			.recordAcceptedShot()
			.recordAcceptedShot()
			.recordAcceptedShot()
			.holeOut();

		HoleStatePayload p = HoleStatePayload.complete(state);
		assertEquals(Phase.COMPLETE, p.phase());
		assertEquals(3, p.strokes());
		assertEquals(-1, p.scoreToPar());
		assertEquals(HoleCompletionReason.HOLED_OUT, p.completionReason());
		assertEquals(GolfScoreTerm.BIRDIE, p.scoreTerm());
	}

	@Test
	void completeSnapshot_pickUp_hasPickedUpReason() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR)
			.recordAcceptedShot()
			.pickUp();

		HoleStatePayload p = HoleStatePayload.complete(state);
		assertEquals(Phase.COMPLETE, p.phase());
		assertEquals(HoleCompletionReason.PICKED_UP, p.completionReason());
		assertNotNull(p.scoreTerm());
	}

	@Test
	void completeSnapshot_strokeLimit_hasStrokeLimitReason() {
		PlayerHoleState state = PlayerHoleState.start(PAR_FOUR);
		for (int i = 0; i < PAR_FOUR.strokeLimit(); i++) {
			state = state.recordAcceptedShot();
		}
		HoleStatePayload p = HoleStatePayload.complete(state);
		assertEquals(Phase.COMPLETE, p.phase());
		assertEquals(HoleCompletionReason.STROKE_LIMIT, p.completionReason());
		assertEquals(PAR_FOUR.strokeLimit(), p.strokes());
	}

	@Test
	void roundCompleteSnapshot_isExplicitAndRetainsFinalValues() {
		HoleStatePayload complete = HoleStatePayload.complete(
			PlayerHoleState.start(PAR_FOUR).recordAcceptedShot().pickUp())
			.withCourseTotals(14, 12, 12)
			.withRoundAdvanceAvailable(true);

		HoleStatePayload roundComplete = complete.asRoundComplete();

		assertEquals(Phase.ROUND_COMPLETE, roundComplete.phase());
		assertEquals(14, roundComplete.courseStrokes());
		assertEquals(12, roundComplete.courseParPlayed());
		assertEquals(12, roundComplete.courseTotalPar());
		assertFalse(roundComplete.roundAdvanceAvailable());
		assertEquals(HoleCompletionReason.PICKED_UP, roundComplete.completionReason());
	}

	@Test
	void nonTerminalSnapshot_cannotBecomeRoundComplete() {
		assertThrows(IllegalStateException.class,
			() -> HoleStatePayload.active(PlayerHoleState.start(PAR_FOUR)).asRoundComplete());
	}

	@Test
	void practiceSnapshot_completionReasonAndTermAreNull() {
		HoleStatePayload p = HoleStatePayload.practice(PAR_FOUR);
		assertNull(p.completionReason());
		assertNull(p.scoreTerm());
	}
}
