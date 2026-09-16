package com.prillcode.minecraftgolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RoundActionPayloadTest {
	@Test
	void completedRoundActionsUseReplayOrLeave() {
		assertEquals(2, RoundActionPayload.Action.values().length);
		assertEquals(RoundActionPayload.Action.REPLAY,
			RoundActionPayload.Action.valueOf("REPLAY"));
		assertEquals(RoundActionPayload.Action.LEAVE,
			RoundActionPayload.Action.valueOf("LEAVE"));
		assertThrows(IllegalArgumentException.class,
			() -> RoundActionPayload.Action.valueOf("DONE"));
	}
}
