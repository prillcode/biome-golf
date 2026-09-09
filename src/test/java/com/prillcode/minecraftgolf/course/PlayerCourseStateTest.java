package com.prillcode.minecraftgolf.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleDefinition;
import com.prillcode.minecraftgolf.hole.PlayerHoleState;

class PlayerCourseStateTest {

	@Test
	void advancesOnlyAfterTerminalHoleAndBuildsFinalScorecard() {
		PlayerCourseState state = PlayerCourseState.start(course());
		assertEquals(1, state.currentHole().hole().number());
		assertThrows(IllegalStateException.class, state::advance);
		assertThrows(IllegalStateException.class, state::finalScorecard);

		for (int expectedHole = 1; expectedHole <= 3; expectedHole++) {
			PlayerHoleState completed = state.currentHole().recordAcceptedShot().holeOut();
			state = state.updateCurrentHole(completed).advance();
			if (expectedHole < 3) {
				assertEquals(expectedHole + 1, state.currentHole().hole().number());
			}
		}

		assertTrue(state.isComplete());
		CourseScorecard scorecard = state.finalScorecard();
		assertEquals(3, scorecard.totalStrokes());
		assertEquals(12, scorecard.totalPar());
		assertEquals(-9, scorecard.scoreToPar());
		assertEquals(List.of(1, 2, 3), scorecard.holes().stream().map(HoleScore::holeNumber).toList());
		assertThrows(IllegalStateException.class, state::advance);
	}

	@Test
	void cumulativeScoreAndResetDoNotLeakPriorRoundState() {
		PlayerCourseState state = PlayerCourseState.start(course());
		state = state.updateCurrentHole(state.currentHole().pickUp()).advance();

		assertEquals(10, state.completedStrokes());
		assertEquals(4, state.completedPar());
		assertEquals(6, state.completedScoreToPar());

		PlayerCourseState reset = state.reset();
		assertFalse(reset.isComplete());
		assertEquals(0, reset.completedStrokes());
		assertEquals(0, reset.completedHoles().size());
		assertEquals(1, reset.currentHole().hole().number());
	}

	@Test
	void restartingCurrentHolePreservesCompletedScoresButClearsCurrentStrokes() {
		PlayerCourseState state = PlayerCourseState.start(course());
		state = state.updateCurrentHole(state.currentHole().recordAcceptedShot().holeOut()).advance();
		state = state.updateCurrentHole(state.currentHole().recordAcceptedShot());

		PlayerCourseState restarted = state.restartCurrentHole();

		assertEquals(1, restarted.completedHoles().size());
		assertEquals(1, restarted.completedStrokes());
		assertEquals(2, restarted.currentHole().hole().number());
		assertEquals(0, restarted.currentHole().strokes());
	}

	@Test
	void identifiesTerminalFinalHoleForAutomaticCourseCompletion() {
		PlayerCourseState state = PlayerCourseState.start(course());
		for (int hole = 1; hole < 3; hole++) {
			state = state.updateCurrentHole(state.currentHole().recordAcceptedShot().holeOut()).advance();
		}
		assertFalse(state.isFinalHoleTerminal());

		state = state.updateCurrentHole(state.currentHole().recordAcceptedShot().holeOut());

		assertTrue(state.isFinalHoleTerminal());
		assertTrue(state.advance().isComplete());
	}

	@Test
	void rejectsWrongHoleAndScoreRegressionUpdates() {
		PlayerCourseState state = PlayerCourseState.start(course());
		PlayerHoleState otherHole = PlayerHoleState.start(course().hole(2));
		assertThrows(IllegalArgumentException.class, () -> state.updateCurrentHole(otherHole));

		PlayerCourseState afterShot = state.updateCurrentHole(state.currentHole().recordAcceptedShot());
		PlayerHoleState zeroStrokes = PlayerHoleState.start(course().hole(1));
		assertThrows(IllegalArgumentException.class, () -> afterShot.updateCurrentHole(zeroStrokes));
	}

	private static CourseDefinition course() {
		return new CourseDefinition("test", "Test Course", "minecraft:overworld",
			new GeneratedLayoutIdentity("test-course", 1),
			List.of(hole(1, 4), hole(2, 3), hole(3, 5)));
	}

	private static HoleDefinition hole(int number, int par) {
		Vec3 tee = new Vec3(number * 20.0, 64.25, 0.0);
		Vec3 cup = new Vec3(number * 20.0 + 10.0, 64.25, 0.0);
		return new HoleDefinition("test:" + number, number, "minecraft:overworld", tee, cup, par,
			new HoleBoundary(new Vec3(0.0, 0.0, -10.0), new Vec3(100.0, 100.0, 10.0)),
			new GeneratedLayoutIdentity("test-hole-" + number, 1), HoleTransition.at(tee));
	}
}
