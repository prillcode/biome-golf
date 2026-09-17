package com.prillcode.minecraftgolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.course.CourseScorecard;
import com.prillcode.minecraftgolf.course.GeneratedLayoutIdentity;
import com.prillcode.minecraftgolf.course.HoleScore;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleCompletionReason;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

class RoundScorecardPayloadTest {
	@Test
	void displaySlotsAlwaysCoverEighteenHolesAndMarkUnplayedHoles() {
		HoleDefinition hole = new HoleDefinition("test:1", 1, "minecraft:overworld",
			Vec3.ZERO, new Vec3(10, 64, 10), 4,
			new HoleBoundary(new Vec3(-10, 0, -10), new Vec3(20, 100, 20)));
		CourseScorecard scorecard = new CourseScorecard("test", List.of(
			new HoleScore("test:1", 1, 4, 5, 0, HoleCompletionReason.HOLED_OUT)));

		RoundScorecardPayload payload = new RoundScorecardPayload("test",
			RoundScorecardPayload.pars(new com.prillcode.minecraftgolf.course.CourseDefinition(
				"test", "Test", "minecraft:overworld", new GeneratedLayoutIdentity("test", 1), List.of(hole))),
			List.of(RoundScorecardPayload.player("Aaron", scorecard)));

		assertEquals(18, payload.pars().size());
		assertEquals(18, payload.players().getFirst().strokes().size());
		assertEquals(4, payload.pars().getFirst());
		assertEquals(5, payload.players().getFirst().strokes().getFirst());
		assertEquals(-1, payload.players().getFirst().strokes().get(1));
		assertEquals(0, payload.pars().get(1));
	}

	@Test
	void positionsPartialScorecardByAuthoredHoleNumber() {
		CourseScorecard scorecard = new CourseScorecard("test", List.of(
			new HoleScore("test:10", 10, 4, 5, 0, HoleCompletionReason.HOLED_OUT),
			new HoleScore("test:11", 11, 4, 4, 0, HoleCompletionReason.HOLED_OUT)));

		RoundScorecardPayload.PlayerRow row = RoundScorecardPayload.player("Aaron", scorecard);

		assertEquals(-1, row.strokes().get(0));
		assertEquals(-1, row.strokes().get(8));
		assertEquals(5, row.strokes().get(9));
		assertEquals(4, row.strokes().get(10));
	}
}
