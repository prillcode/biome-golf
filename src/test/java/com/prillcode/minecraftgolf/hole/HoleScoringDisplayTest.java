package com.prillcode.minecraftgolf.hole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.GeneratedLayoutIdentity;
import com.prillcode.minecraftgolf.course.HoleTransition;
import com.prillcode.minecraftgolf.course.PlayerCourseState;
import com.prillcode.minecraftgolf.golf.Vec3;

class HoleScoringDisplayTest {
	@Test
	void activeShotAndPenaltyKeepAttemptsSeparateAndHideProvisionalScore() {
		PlayerCourseState courseState = PlayerCourseState.start(course());
		PlayerHoleState current = courseState.currentHole().recordAcceptedShot()
			.applyPenalty(PenaltyType.WATER);
		courseState = courseState.updateCurrentHole(current);

		HoleScoringDisplay display = HoleScoringDisplay.from(current, courseState);

		assertEquals(1, display.acceptedShots());
		assertEquals(2, display.strokes());
		assertEquals(1, display.penalties());
		assertEquals("Shots attempted: 1", display.holeText());
		assertFalse(display.actionBarText().contains("-2"));
		assertFalse(display.hasCourseScore());
	}

	@Test
	void terminalCurrentHoleIsIncludedExactlyOnceInCourseScore() {
		PlayerCourseState state = PlayerCourseState.start(course());
		PlayerHoleState first = complete(state.currentHole(), 4);
		state = state.updateCurrentHole(first);
		HoleScoringDisplay firstDisplay = HoleScoringDisplay.from(first, state);
		assertTrue(firstDisplay.hasCourseScore());
		assertEquals(4, firstDisplay.courseStrokes());
		assertEquals(4, firstDisplay.coursePar());

		state = state.advance();
		PlayerHoleState second = complete(state.currentHole(), 2);
		state = state.updateCurrentHole(second);
		HoleScoringDisplay secondDisplay = HoleScoringDisplay.from(second, state);
		assertEquals(6, secondDisplay.courseStrokes());
		assertEquals(7, secondDisplay.coursePar());
		assertEquals(" | course 6 strokes (-1)", secondDisplay.courseText());

		state = state.advance();
		PlayerHoleState third = complete(state.currentHole(), 5);
		state = state.updateCurrentHole(third).advance();
		HoleScoringDisplay completeDisplay = HoleScoringDisplay.from(third, state);
		assertEquals(11, completeDisplay.courseStrokes());
		assertEquals(12, completeDisplay.coursePar());
	}

	@Test
	void pickupPreservesActualAttemptsWhileShowingTerminalResult() {
		PlayerHoleState pickedUp = PlayerHoleState.start(course().hole(1))
			.recordAcceptedShot().recordAcceptedShot().pickUp();
		HoleScoringDisplay display = HoleScoringDisplay.from(pickedUp, null);

		assertEquals(2, display.acceptedShots());
		assertEquals(10, display.strokes());
		assertEquals("+6 OVER_PAR", display.holeText());
	}

	private static PlayerHoleState complete(PlayerHoleState state, int strokes) {
		for (int i = 0; i < strokes; i++) state = state.recordAcceptedShot();
		return state.holeOut();
	}

	private static CourseDefinition course() {
		return new CourseDefinition("test", "Test", "minecraft:overworld",
			new GeneratedLayoutIdentity("test", 1), List.of(hole(1, 4), hole(2, 3), hole(3, 5)));
	}

	private static HoleDefinition hole(int number, int par) {
		Vec3 tee = new Vec3(number * 20.0, 64.0, 0.0);
		return new HoleDefinition("test:" + number, number, "minecraft:overworld", tee,
			tee.add(new Vec3(10.0, 0.0, 0.0)), par,
			new HoleBoundary(new Vec3(0.0, 0.0, -10.0), new Vec3(100.0, 100.0, 10.0)),
			new GeneratedLayoutIdentity("hole-" + number, 1), HoleTransition.at(tee));
	}
}
