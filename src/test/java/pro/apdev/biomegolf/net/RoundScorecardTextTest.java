package pro.apdev.biomegolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class RoundScorecardTextTest {

	@Test
	void rendersEveryPlayedHoleAndATotalPerPlayer() {
		RoundScorecardPayload payload = new RoundScorecardPayload("test",
			List.of(new RoundScorecardPayload.HoleColumn(1, 3),
				new RoundScorecardPayload.HoleColumn(2, 4)),
			List.of(new RoundScorecardPayload.PlayerRow("Aaron", List.of(2, 5))));

		assertEquals(List.of(
			"[golf] FINAL SCORECARD — test",
			"Aaron:",
			"  Hole 1 — 2 strokes on Par 3 (-1)",
			"  Hole 2 — 5 strokes on Par 4 (+1)",
			"  Total — 7 strokes on Par 7 (E)"),
			RoundScorecardText.lines(payload));
	}

	@Test
	void skipsHolesThatWereNeverPlayed() {
		RoundScorecardPayload payload = new RoundScorecardPayload("test",
			List.of(new RoundScorecardPayload.HoleColumn(1, 3),
				new RoundScorecardPayload.HoleColumn(2, 4)),
			List.of(new RoundScorecardPayload.PlayerRow("Bea", List.of(3, -1))));

		assertEquals(List.of(
			"[golf] FINAL SCORECARD — test",
			"Bea:",
			"  Hole 1 — 3 strokes on Par 3 (E)",
			"  Total — 3 strokes on Par 3 (E)"),
			RoundScorecardText.lines(payload));
	}
}
