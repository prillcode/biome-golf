package com.prillcode.minecraftgolf.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.GeneratedLayoutIdentity;
import com.prillcode.minecraftgolf.course.HoleTransition;
import com.prillcode.minecraftgolf.course.PlayerCourseState;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleDefinition;
import com.prillcode.minecraftgolf.hole.PlayerHoleState;

	class ActiveHoleServiceStatusTest {
	@Test
	void terminalCurrentHoleIsIncludedBeforeAdvance() {
		PlayerCourseState state = PlayerCourseState.start(course());
		PlayerHoleState first = completeHole(state.currentHole(), 3);
		state = state.updateCurrentHole(first);

		assertEquals(" | course 3 strokes (-1)", ActiveHoleService.cumulativeStatus(state, first));
	}

	@Test
	void completedCourseStatusDoesNotCountRetainedFinalHoleTwice() {
		PlayerCourseState state = PlayerCourseState.start(course());
		state = completeWithStrokes(state, 4).advance();
		state = completeWithStrokes(state, 3).advance();
		PlayerHoleState finalHole = completeHole(state.currentHole(), 3);
		state = state.updateCurrentHole(finalHole).advance();

		assertEquals(" | course 10 strokes (-2)",
			ActiveHoleService.cumulativeStatus(state, finalHole));
	}

	private static PlayerCourseState completeWithStrokes(PlayerCourseState state, int strokes) {
		return state.updateCurrentHole(completeHole(state.currentHole(), strokes));
	}

	private static PlayerHoleState completeHole(PlayerHoleState state, int strokes) {
		for (int stroke = 0; stroke < strokes; stroke++) {
			state = state.recordAcceptedShot();
		}
		return state.holeOut();
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
