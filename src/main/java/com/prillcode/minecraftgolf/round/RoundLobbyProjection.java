package com.prillcode.minecraftgolf.round;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import com.prillcode.minecraftgolf.course.CourseDefinition;

/** Display-safe server projection shared by command/chat and the optional round browser. */
public record RoundLobbyProjection(List<CourseEntry> courses, List<LobbyEntry> lobbies) {
	public record CourseEntry(String id, String displayName, int holeCount, int totalPar) {
		public CourseEntry {
			if (id == null || id.isBlank() || displayName == null || displayName.isBlank()) {
				throw new IllegalArgumentException("course display metadata must not be blank");
			}
			if (holeCount < 1 || totalPar < 1) throw new IllegalArgumentException("invalid course totals");
		}
	}

	public record LobbyEntry(UUID roundId, String courseId, String courseName,
			String coordinatorName, int participantCount, int capacity) {
		public LobbyEntry {
			Objects.requireNonNull(roundId, "roundId");
			if (courseId == null || courseId.isBlank() || courseName == null || courseName.isBlank()
					|| coordinatorName == null || coordinatorName.isBlank()) {
				throw new IllegalArgumentException("lobby display metadata must not be blank");
			}
			if (participantCount < 1 || capacity < participantCount) {
				throw new IllegalArgumentException("invalid lobby capacity");
			}
		}
	}

	public RoundLobbyProjection {
		courses = List.copyOf(Objects.requireNonNull(courses, "courses"));
		lobbies = List.copyOf(Objects.requireNonNull(lobbies, "lobbies"));
	}

	public static RoundLobbyProjection from(List<CourseDefinition> courses,
			List<ReadyGolfRound> openLobbies, Function<UUID, String> playerName) {
		Objects.requireNonNull(playerName, "playerName");
		return new RoundLobbyProjection(courses.stream().map(course -> new CourseEntry(course.id(),
			course.displayName(), course.holes().size(), course.totalPar())).toList(),
			openLobbies.stream().map(round -> new LobbyEntry(round.roundId(), round.course().id(),
				round.course().displayName(), playerName.apply(round.coordinatorId().orElseThrow()),
				(int) round.activeParticipantCount(), ReadyGolfRound.MAX_PARTICIPANTS)).toList());
	}
}
