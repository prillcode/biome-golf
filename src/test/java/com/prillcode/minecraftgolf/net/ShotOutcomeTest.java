package com.prillcode.minecraftgolf.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M10.1: the vanilla-compatible shot path reports the same feedback as the payload path. */
class ShotOutcomeTest {

	@Test
	void successAndUnknownAreSilent() {
		assertTrue(ShotOutcome.SUCCESS.description().isEmpty());
		assertTrue(ShotOutcome.UNKNOWN.description().isEmpty());
	}

	@Test
	void everyActionableOutcomeCarriesPlayerFacingFeedback() {
		for (ShotOutcome outcome : ShotOutcome.values()) {
			if (outcome == ShotOutcome.SUCCESS || outcome == ShotOutcome.UNKNOWN) {
				continue;
			}
			String message = outcome.description();
			assertFalse(message.isBlank(), outcome + " must describe itself");
			assertTrue(message.startsWith("[golf] "), outcome + " feedback should be tagged");
		}
	}

	@Test
	void tooFarAndShotTypeFeedbackAreSpecific() {
		assertTrue(ShotOutcome.BALL_TOO_FAR.description().contains("closer"));
		assertTrue(ShotOutcome.INVALID_SHOT_TYPE.description().contains("shot type"));
		assertTrue(ShotOutcome.DRIVER_NOT_ALLOWED_ON_SAND.description().contains("sand"));
	}
}
