package pro.apdev.biomegolf.round;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import pro.apdev.biomegolf.course.CourseDefinition;
import pro.apdev.biomegolf.course.PlayerCourseState;
import pro.apdev.biomegolf.hole.PlayerHoleState;

/**
 * Immutable, Minecraft-free authority for one 1--4 player Ready Golf round.
 *
 * <p>Participant list order is join order and therefore also the deterministic
 * result presentation order. Suspended golfers remain in the round but are
 * excluded from the advancement barrier. They are withdrawn if connected
 * golfers advance while they remain offline.</p>
 */
public record ReadyGolfRound(
	UUID roundId,
	CourseDefinition course,
	RoundPhase phase,
	int currentHoleIndex,
	List<ReadyGolfParticipant> participants
) {
	public static final int MAX_PARTICIPANTS = 4;

	public ReadyGolfRound {
		Objects.requireNonNull(roundId, "roundId");
		Objects.requireNonNull(course, "course");
		Objects.requireNonNull(phase, "phase");
		Objects.requireNonNull(participants, "participants");
		participants = List.copyOf(participants);
		validateParticipants(course, phase, currentHoleIndex, participants);
	}

	public static ReadyGolfRound create(UUID roundId, CourseDefinition course, UUID creatorId) {
		Objects.requireNonNull(creatorId, "creatorId");
		return new ReadyGolfRound(roundId, course, RoundPhase.LOBBY, 0,
			List.of(ReadyGolfParticipant.lobby(creatorId)));
	}

	public ReadyGolfRound join(UUID playerId) {
		requirePhase(RoundPhase.LOBBY);
		Objects.requireNonNull(playerId, "playerId");
		if (findParticipant(playerId).isPresent()) {
			throw new IllegalStateException("player is already in the round");
		}
		if (participants.size() >= MAX_PARTICIPANTS) {
			throw new IllegalStateException("Ready Golf round is full");
		}
		List<ReadyGolfParticipant> updated = new ArrayList<>(participants);
		updated.add(ReadyGolfParticipant.lobby(playerId));
		return with(RoundPhase.LOBBY, currentHoleIndex, updated);
	}

	public ReadyGolfRound start(UUID coordinatorId) {
		requirePhase(RoundPhase.LOBBY);
		Objects.requireNonNull(coordinatorId, "coordinatorId");
		if (!coordinatorId.equals(coordinatorId().orElse(null))) {
			throw new IllegalStateException("only the lobby coordinator can start the round");
		}
		List<ReadyGolfParticipant> started = participants.stream()
			.map(participant -> participant.start(PlayerCourseState.start(course)))
			.toList();
		return with(RoundPhase.PLAYING, 0, started);
	}

	/** Applies one player's authoritative hole update without touching any peer state. */
	public ReadyGolfRound updateCurrentHole(UUID playerId, PlayerHoleState updatedHole) {
		requirePhase(RoundPhase.PLAYING);
		ReadyGolfParticipant participant = requireActiveParticipant(playerId);
		PlayerCourseState updatedCourse = participant.courseState().updateCurrentHole(updatedHole);
		ReadyGolfRound updated = replaceParticipant(participant.withCourseState(updatedCourse));
		return updated.completeFinalHoleIfReady();
	}

	/** Restarts only the caller's current hole and preserves every peer's state. */
	public ReadyGolfRound restartCurrentHole(UUID playerId) {
		requirePhase(RoundPhase.PLAYING);
		ReadyGolfParticipant participant = requireActiveParticipant(playerId);
		return replaceParticipant(participant.withCourseState(
			participant.courseState().restartCurrentHole()));
	}

	/** Preserves the M5 replay fast path without allowing a multiplayer round to replay itself. */
	public ReadyGolfRound replaySolo(UUID playerId) {
		requirePhase(RoundPhase.COMPLETE);
		ReadyGolfParticipant participant = requireActiveParticipant(playerId);
		if (participants.size() != 1 || !participant.courseState().isComplete()) {
			throw new IllegalStateException("only one completed active golfer can use solo replay");
		}
		return new ReadyGolfRound(roundId, course, RoundPhase.PLAYING, 0,
			List.of(participant.withCourseState(participant.courseState().reset())));
	}

	/** Replays Hole 1 for every participant that remained in the completed round. */
	public ReadyGolfRound replayRemaining() {
		requirePhase(RoundPhase.COMPLETE);
		if (participants.stream().noneMatch(ReadyGolfParticipant::isParticipating)) {
			throw new IllegalStateException("no active golfers remain in the completed round");
		}
		List<ReadyGolfParticipant> replayed = participants.stream()
			.map(participant -> participant.isParticipating()
				? participant.withCourseState(participant.courseState().reset())
				: participant)
			.toList();
		return with(RoundPhase.PLAYING, 0, replayed);
	}

	/** Removes one player from a completed round without changing the other players. */
	public ReadyGolfRound leaveCompleted(UUID playerId) {
		requirePhase(RoundPhase.COMPLETE);
		ReadyGolfParticipant participant = requireActiveParticipant(playerId);
		return replaceParticipant(participant.withStatus(ParticipantStatus.WITHDRAWN));
	}

	/**
	 * Disconnecting from a lobby removes that golfer; disconnecting during play
	 * suspends them and excludes them from the barrier.
	 */
	public ReadyGolfRound disconnect(UUID playerId) {
		if (phase == RoundPhase.LOBBY) {
			return removeLobbyParticipant(playerId);
		}
		if (phase == RoundPhase.COMPLETE) {
			return leaveCompleted(playerId);
		}
		requirePhase(RoundPhase.PLAYING);
		ReadyGolfParticipant participant = requireActiveParticipant(playerId);
		return replaceParticipant(participant.withStatus(ParticipantStatus.SUSPENDED))
			.completeFinalHoleIfReady();
	}

	public ReadyGolfRound reconnect(UUID playerId) {
		requirePhase(RoundPhase.PLAYING);
		ReadyGolfParticipant participant = requireParticipant(playerId);
		if (participant.status() != ParticipantStatus.SUSPENDED) {
			throw new IllegalStateException("only a suspended participant can reconnect");
		}
		return replaceParticipant(participant.withStatus(ParticipantStatus.ACTIVE));
	}

	/** Removes a lobby golfer or permanently withdraws an in-progress golfer. */
	public ReadyGolfRound withdraw(UUID playerId) {
		if (phase == RoundPhase.LOBBY) {
			return removeLobbyParticipant(playerId);
		}
		requirePhase(RoundPhase.PLAYING);
		ReadyGolfParticipant participant = requireParticipant(playerId);
		if (!participant.isParticipating()) {
			throw new IllegalStateException("player is already withdrawn");
		}
		ReadyGolfRound updated = replaceParticipant(
			participant.withStatus(ParticipantStatus.WITHDRAWN));
		if (updated.participants.stream().noneMatch(ReadyGolfParticipant::isParticipating)) {
			return updated.with(RoundPhase.COMPLETE, currentHoleIndex, updated.participants);
		}
		return updated.completeFinalHoleIfReady();
	}

	/**
	 * Atomically advances all connected golfers from a terminal Hole 1 or 2.
	 * The expected index makes stale duplicate requests fail without mutation.
	 */
	public ReadyGolfRound advanceNextHole(int expectedCurrentHoleIndex) {
		requirePhase(RoundPhase.PLAYING);
		if (expectedCurrentHoleIndex != currentHoleIndex) {
			throw new IllegalStateException("stale next-hole request");
		}
		if (currentHoleIndex >= course.holes().size() - 1) {
			throw new IllegalStateException("the final hole completes automatically");
		}
		if (!allActiveTerminal()) {
			throw new IllegalStateException("all active participants must be terminal before advancing");
		}
		List<ReadyGolfParticipant> advanced = participants.stream()
			.map(participant -> switch (participant.status()) {
				case ACTIVE -> participant.withCourseState(participant.courseState().advance());
				case SUSPENDED -> participant.withStatus(ParticipantStatus.WITHDRAWN);
				case WITHDRAWN -> participant;
			})
			.toList();
		return with(RoundPhase.PLAYING, currentHoleIndex + 1, advanced);
	}

	/** True only when at least one connected golfer exists and all are terminal. */
	public boolean allActiveTerminal() {
		List<ReadyGolfParticipant> active = participants.stream()
			.filter(participant -> participant.status() == ParticipantStatus.ACTIVE)
			.toList();
		return !active.isEmpty()
			&& active.stream().allMatch(participant -> participant.courseState().currentHole().isComplete());
	}

	public long activeParticipantCount() {
		return participants.stream()
			.filter(participant -> participant.status() == ParticipantStatus.ACTIVE)
			.count();
	}

	/** True when at least one connected golfer still owns this round. */
	public boolean hasActiveParticipants() {
		return activeParticipantCount() > 0;
	}

	/** True while any active or suspended golfer still owns this round. */
	public boolean hasRemainingParticipants() {
		return participants.stream().anyMatch(ReadyGolfParticipant::isParticipating);
	}

	public long terminalActiveParticipantCount() {
		if (phase != RoundPhase.PLAYING) {
			return 0;
		}
		return participants.stream()
			.filter(participant -> participant.status() == ParticipantStatus.ACTIVE)
			.filter(participant -> participant.courseState().currentHole().isComplete())
			.count();
	}

	public Optional<UUID> coordinatorId() {
		if (phase != RoundPhase.LOBBY) {
			return Optional.empty();
		}
		return participants.stream()
			.filter(participant -> participant.status() == ParticipantStatus.ACTIVE)
			.map(ReadyGolfParticipant::playerId)
			.findFirst();
	}

	public Optional<ReadyGolfParticipant> findParticipant(UUID playerId) {
		Objects.requireNonNull(playerId, "playerId");
		return participants.stream()
			.filter(participant -> participant.playerId().equals(playerId))
			.findFirst();
	}

	private ReadyGolfRound completeFinalHoleIfReady() {
		if (phase != RoundPhase.PLAYING
				|| currentHoleIndex != course.holes().size() - 1
				|| !allActiveTerminal()) {
			return this;
		}
		List<ReadyGolfParticipant> completed = participants.stream()
			.map(participant -> switch (participant.status()) {
				case ACTIVE -> participant.withCourseState(participant.courseState().advance());
				case SUSPENDED -> participant.withStatus(ParticipantStatus.WITHDRAWN);
				case WITHDRAWN -> participant;
			})
			.toList();
		return with(RoundPhase.COMPLETE, course.holes().size(), completed);
	}

	private ReadyGolfRound removeLobbyParticipant(UUID playerId) {
		requirePhase(RoundPhase.LOBBY);
		ReadyGolfParticipant participant = requireParticipant(playerId);
		List<ReadyGolfParticipant> remaining = participants.stream()
			.filter(candidate -> !candidate.playerId().equals(participant.playerId()))
			.toList();
		RoundPhase updatedPhase = remaining.isEmpty() ? RoundPhase.COMPLETE : RoundPhase.LOBBY;
		return with(updatedPhase, 0, remaining);
	}

	private ReadyGolfRound replaceParticipant(ReadyGolfParticipant replacement) {
		List<ReadyGolfParticipant> updated = participants.stream()
			.map(participant -> participant.playerId().equals(replacement.playerId())
				? replacement : participant)
			.toList();
		return with(phase, currentHoleIndex, updated);
	}

	private ReadyGolfRound with(RoundPhase updatedPhase, int updatedHoleIndex,
			List<ReadyGolfParticipant> updatedParticipants) {
		return new ReadyGolfRound(roundId, course, updatedPhase, updatedHoleIndex, updatedParticipants);
	}

	private ReadyGolfParticipant requireActiveParticipant(UUID playerId) {
		ReadyGolfParticipant participant = requireParticipant(playerId);
		if (participant.status() != ParticipantStatus.ACTIVE) {
			throw new IllegalStateException("player is not an active participant");
		}
		return participant;
	}

	private ReadyGolfParticipant requireParticipant(UUID playerId) {
		return findParticipant(playerId)
			.orElseThrow(() -> new IllegalArgumentException("player is not in the round"));
	}

	private void requirePhase(RoundPhase required) {
		if (phase != required) {
			throw new IllegalStateException("round must be " + required + " but is " + phase);
		}
	}

	private static void validateParticipants(CourseDefinition course, RoundPhase phase,
			int currentHoleIndex, List<ReadyGolfParticipant> participants) {
		if (participants.size() > MAX_PARTICIPANTS) {
			throw new IllegalArgumentException("Ready Golf round supports at most four participants");
		}
		if (phase != RoundPhase.COMPLETE && participants.isEmpty()) {
			throw new IllegalArgumentException("an active round must contain a participant");
		}
		Set<UUID> playerIds = new HashSet<>();
		for (ReadyGolfParticipant participant : participants) {
			Objects.requireNonNull(participant, "participants must not contain null");
			if (!playerIds.add(participant.playerId())) {
				throw new IllegalArgumentException("participant player ids must be unique");
			}
			validateParticipantState(course, phase, currentHoleIndex, participant);
		}
		if (phase == RoundPhase.LOBBY && currentHoleIndex != 0) {
			throw new IllegalArgumentException("a lobby must be positioned before Hole 1");
		}
		if (phase == RoundPhase.PLAYING
				&& (currentHoleIndex < 0 || currentHoleIndex >= course.holes().size())) {
			throw new IllegalArgumentException("playing hole index is out of range");
		}
		if (phase == RoundPhase.COMPLETE
				&& (currentHoleIndex < 0 || currentHoleIndex > course.holes().size())) {
			throw new IllegalArgumentException("complete round hole index is out of range");
		}
	}

	private static void validateParticipantState(CourseDefinition course, RoundPhase phase,
			int currentHoleIndex, ReadyGolfParticipant participant) {
		PlayerCourseState state = participant.courseState();
		if (phase == RoundPhase.LOBBY) {
			if (participant.status() != ParticipantStatus.ACTIVE || state != null) {
				throw new IllegalArgumentException("lobby participants must be active without course state");
			}
			return;
		}
		if (state == null) {
			throw new IllegalArgumentException("playing and completed participants require course state");
		}
		if (!course.equals(state.course())) {
			throw new IllegalArgumentException("participant state must use the round course");
		}
		if (phase == RoundPhase.PLAYING && participant.isParticipating()
				&& state.currentHoleIndex() != currentHoleIndex) {
			throw new IllegalArgumentException("participating golfers must share the round hole index");
		}
		if (phase == RoundPhase.COMPLETE && participant.status() == ParticipantStatus.SUSPENDED) {
			throw new IllegalArgumentException("a complete round cannot retain suspended golfers");
		}
		if (phase == RoundPhase.COMPLETE && participant.status() == ParticipantStatus.ACTIVE
				&& !state.isComplete()) {
			throw new IllegalArgumentException("active golfers must have final scorecards in a complete round");
		}
	}
}
