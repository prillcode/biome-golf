package com.prillcode.minecraftgolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.CourseScorecard;
import com.prillcode.minecraftgolf.course.GeneratedLayoutIdentity;
import com.prillcode.minecraftgolf.course.HoleScore;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleCompletionReason;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

class RoundScorecardPayloadTest {
	@Test
	void variableAuthoredHolesAndPartialScoresRetainTheirNumbers() {
		CourseDefinition course = course(20);
		CourseScorecard scorecard = new CourseScorecard("test", List.of(
			new HoleScore("test:10", 10, 4, 5, 0, HoleCompletionReason.HOLED_OUT),
			new HoleScore("test:11", 11, 4, 4, 0, HoleCompletionReason.HOLED_OUT)));
		List<RoundScorecardPayload.HoleColumn> holes = RoundScorecardPayload.holes(course);
		RoundScorecardPayload payload = new RoundScorecardPayload("test", holes,
			List.of(RoundScorecardPayload.player("Aaron", scorecard, holes)));

		assertEquals("minecraft_golf:round_scorecard_v2", payload.type().id().toString());
		assertEquals(20, payload.holes().size());
		assertEquals(20, payload.holes().getLast().number());
		assertEquals(-1, payload.players().getFirst().strokes().get(8));
		assertEquals(5, payload.players().getFirst().strokes().get(9));
		assertEquals(4, payload.players().getFirst().strokes().get(10));
	}

	private static CourseDefinition course(int count) {
		List<HoleDefinition> holes = java.util.stream.IntStream.rangeClosed(1, count).mapToObj(number ->
			new HoleDefinition("test:" + number, number, "minecraft:overworld", Vec3.ZERO,
				new Vec3(number * 10.0, 64, 10), 4,
				new HoleBoundary(new Vec3(-10, 0, -10), new Vec3(count * 10.0 + 20, 100, 20)))).toList();
		return new CourseDefinition("test", "Test", "minecraft:overworld",
			new GeneratedLayoutIdentity("test", 1), holes);
	}
}
