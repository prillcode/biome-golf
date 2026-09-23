package com.prillcode.minecraftgolf.hole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClientLightHudTextTest {

	@Test
	void activeLineIncludesHoleParStrokesAndDistances() {
		assertEquals("[golf] Hole 1 Par 3 | strokes 2/8 | cup 42 blocks | shot 17 blocks",
			ClientLightHudText.active(1, 3, 2, 8, 42, 17));
	}

	@Test
	void activeLineOmitsUnavailableDistances() {
		assertEquals("[golf] Hole 2 Par 4 | strokes 0/10",
			ClientLightHudText.active(2, 4, 0, 10, -1, -1));
	}

	@Test
	void completeLineAddsTermAndCourseTotalsWhenPresent() {
		assertEquals("[golf] Hole 1 Par 3 | 4 strokes (+1) BOGEY",
			ClientLightHudText.complete(1, 3, 4, 1, "BOGEY", 4, 3, 0));
		assertEquals("[golf] Hole 3 Par 4 | 3 strokes (-1) BIRDIE | course 10/12 (-1)",
			ClientLightHudText.complete(3, 4, 3, -1, "BIRDIE", 10, 11, 12));
	}

	@Test
	void practiceLineNamesTheHoleWhenConfigured() {
		assertEquals("[golf] Practice — Hole 2 Par 4", ClientLightHudText.practice(2, 4, -1));
		assertEquals("[golf] Practice | shot 18 blocks", ClientLightHudText.practice(0, 0, 18));
	}

	@Test
	void practiceLinePointsAtTheCourseBrowserWhenNothingIsConfigured() {
		assertTrue(ClientLightHudText.practice(0, 0, -1).contains("/golf browse"));
	}

	@Test
	void missingBallLineIsActionable() {
		assertTrue(ClientLightHudText.missingBall().contains("/golf hole restart"));
	}

	@Test
	void toParMatchesModdedHudShortForm() {
		assertEquals("E", ClientLightHudText.toPar(0));
		assertEquals("+2", ClientLightHudText.toPar(2));
		assertEquals("-3", ClientLightHudText.toPar(-3));
	}
}
