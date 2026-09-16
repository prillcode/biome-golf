package com.prillcode.minecraftgolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class RoundLobbyPayloadTest {
	@Test
	void courseListContainsOnlyDisplayMetadata() {
		CourseListPayload payload = new CourseListPayload(List.of(
			new CourseListPayload.CourseEntry("pine", "Pine Hills", 3, 12)));
		assertEquals("pine", payload.courses().getFirst().id());
		assertEquals(3, payload.courses().getFirst().holeCount());
		assertEquals(12, payload.courses().getFirst().totalPar());
	}

	@Test
	void lobbyStateCarriesCoordinatorAndParticipationFlags() {
		LobbyStatePayload payload = new LobbyStatePayload(LobbyStatePayload.Phase.LOBBY,
			"pine", "Pine Hills", 2, 4, true, true, true);
		assertEquals(LobbyStatePayload.Phase.LOBBY, payload.phase());
		assertEquals(2, payload.participantCount());
	}

	@Test
	void actionRejectsOversizedCourseIds() {
		assertThrows(IllegalArgumentException.class, () -> new RoundLobbyActionPayload(
			RoundLobbyActionPayload.Action.CREATE, "x".repeat(129)));
	}
}
