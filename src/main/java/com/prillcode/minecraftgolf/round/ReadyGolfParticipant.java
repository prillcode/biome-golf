package com.prillcode.minecraftgolf.round;

import java.util.Objects;
import java.util.UUID;

import com.prillcode.minecraftgolf.course.PlayerCourseState;

/** One golfer in stable join order, with independently owned course progress. */
public record ReadyGolfParticipant(
	UUID playerId,
	ParticipantStatus status,
	PlayerCourseState courseState
) {
	public ReadyGolfParticipant {
		Objects.requireNonNull(playerId, "playerId");
		Objects.requireNonNull(status, "status");
	}

	static ReadyGolfParticipant lobby(UUID playerId) {
		return new ReadyGolfParticipant(playerId, ParticipantStatus.ACTIVE, null);
	}

	ReadyGolfParticipant start(PlayerCourseState state) {
		return new ReadyGolfParticipant(playerId, status, Objects.requireNonNull(state, "state"));
	}

	ReadyGolfParticipant withStatus(ParticipantStatus updatedStatus) {
		return new ReadyGolfParticipant(playerId, updatedStatus, courseState);
	}

	ReadyGolfParticipant withCourseState(PlayerCourseState updatedState) {
		return new ReadyGolfParticipant(playerId, status, Objects.requireNonNull(updatedState, "updatedState"));
	}

	public boolean isParticipating() {
		return status != ParticipantStatus.WITHDRAWN;
	}
}
