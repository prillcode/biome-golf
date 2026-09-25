package pro.apdev.biomegolf.round;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import pro.apdev.biomegolf.course.CourseDefinition;

/**
 * Minecraft-free, server-owned registry for independent Ready Golf rounds.
 *
 * <p>The map and membership index are mutated under the same monitor. A failed
 * candidate update therefore cannot leave either view partially changed.</p>
 */
public final class ReadyGolfRoundRegistry {
	private final Map<UUID, ReadyGolfRound> rounds = new LinkedHashMap<>();
	private final Map<UUID, UUID> membership = new LinkedHashMap<>();
	private final Map<UUID, Long> versions = new LinkedHashMap<>();

	public synchronized ReadyGolfRound create(CourseDefinition course, UUID creatorId) {
		return create(UUID.randomUUID(), course, creatorId);
	}

	public synchronized ReadyGolfRound create(UUID roundId, CourseDefinition course, UUID creatorId) {
		Objects.requireNonNull(roundId, "roundId");
		Objects.requireNonNull(course, "course");
		Objects.requireNonNull(creatorId, "creatorId");
		if (rounds.containsKey(roundId)) {
			throw new IllegalStateException("round id already exists");
		}
		if (membership.containsKey(creatorId)) {
			throw new IllegalStateException("player already belongs to a golf context");
		}
		ReadyGolfRound created = ReadyGolfRound.create(roundId, course, creatorId);
		commitNew(created);
		return created;
	}

	public synchronized Optional<ReadyGolfRound> find(UUID roundId) {
		return Optional.ofNullable(rounds.get(Objects.requireNonNull(roundId, "roundId")));
	}

	public synchronized Optional<ReadyGolfRound> findByPlayer(UUID playerId) {
		UUID roundId = membership.get(Objects.requireNonNull(playerId, "playerId"));
		return roundId == null ? Optional.empty() : Optional.of(rounds.get(roundId));
	}

	/** Returns open lobbies in stable round-creation order. */
	public synchronized List<ReadyGolfRound> listOpen() {
		return rounds.values().stream()
			.filter(round -> round.phase() == RoundPhase.LOBBY)
			.toList();
	}

	/** Returns all registered rounds in stable round-creation order. */
	public synchronized List<ReadyGolfRound> list() {
		return List.copyOf(rounds.values());
	}

	public synchronized boolean hasActivePlay() {
		return rounds.values().stream().anyMatch(round -> round.phase() != RoundPhase.COMPLETE);
	}

	public synchronized ReadyGolfRound join(UUID roundId, UUID playerId) {
		ReadyGolfRound current = requireRound(roundId);
		if (membership.containsKey(Objects.requireNonNull(playerId, "playerId"))) {
			throw new IllegalStateException("player already belongs to a golf context");
		}
		return replace(current, current.join(playerId));
	}

	/** Applies an existing ReadyGolf transformation only if its input is current. */
	public synchronized ReadyGolfRound update(UUID roundId, ReadyGolfRound expected,
			ReadyGolfRound candidate) {
		Objects.requireNonNull(expected, "expected");
		ReadyGolfRound current = requireRound(roundId);
		if (!current.equals(expected)) {
			throw new IllegalStateException("stale round snapshot");
		}
		return replace(current, candidate);
	}

	/** Applies an existing ReadyGolf transformation only if its input is current. */
	public synchronized ReadyGolfRound update(UUID roundId, ReadyGolfRound expected,
			UnaryOperator<ReadyGolfRound> transformation) {
		Objects.requireNonNull(expected, "expected");
		Objects.requireNonNull(transformation, "transformation");
		ReadyGolfRound current = requireRound(roundId);
		if (!current.equals(expected)) {
			throw new IllegalStateException("stale round snapshot");
		}
		return replace(current, transformation.apply(current));
	}

	/** Version-based equivalent for callers that retain a version rather than a snapshot. */
	public synchronized ReadyGolfRound update(UUID roundId, long expectedVersion,
			UnaryOperator<ReadyGolfRound> transformation) {
		Objects.requireNonNull(transformation, "transformation");
		ReadyGolfRound current = requireRound(roundId);
		if (versions.get(roundId) != expectedVersion) {
			throw new IllegalStateException("stale round version");
		}
		return replace(current, transformation.apply(current));
	}

	public synchronized long version(UUID roundId) {
		return versions.getOrDefault(Objects.requireNonNull(roundId, "roundId"), -1L);
	}

	public synchronized ReadyGolfRound disconnect(UUID roundId, UUID playerId) {
		ReadyGolfRound current = requireRound(roundId);
		return replace(current, current.disconnect(playerId));
	}

	public synchronized ReadyGolfRound reconnect(UUID roundId, UUID playerId) {
		ReadyGolfRound current = requireRound(roundId);
		return replace(current, current.reconnect(playerId));
	}

	public synchronized ReadyGolfRound leave(UUID roundId, UUID playerId) {
		ReadyGolfRound current = requireRound(roundId);
		ReadyGolfRound candidate = current.phase() == RoundPhase.COMPLETE
			? current.leaveCompleted(playerId)
			: current.withdraw(playerId);
		return replace(current, candidate);
	}

	public synchronized ReadyGolfRound withdraw(UUID roundId, UUID playerId) {
		ReadyGolfRound current = requireRound(roundId);
		return replace(current, current.withdraw(playerId));
	}

	private void commitNew(ReadyGolfRound round) {
		validateMembership(round, null);
		rounds.put(round.roundId(), round);
		versions.put(round.roundId(), 0L);
		index(round);
	}

	private ReadyGolfRound replace(ReadyGolfRound current, ReadyGolfRound candidate) {
		Objects.requireNonNull(candidate, "transformation returned null");
		if (!current.roundId().equals(candidate.roundId())) {
			throw new IllegalArgumentException("candidate round id does not match target");
		}
		validateMembership(candidate, current.roundId());
		deindex(current);
		if (candidate.hasRemainingParticipants()) {
			rounds.put(candidate.roundId(), candidate);
			versions.put(candidate.roundId(), versions.get(candidate.roundId()) + 1L);
			index(candidate);
		} else {
			rounds.remove(candidate.roundId());
			versions.remove(candidate.roundId());
		}
		return candidate;
	}

	private void validateMembership(ReadyGolfRound candidate, UUID replacingRoundId) {
		for (ReadyGolfParticipant participant : candidate.participants()) {
			if (!participant.isParticipating()) {
				continue;
			}
			UUID existing = membership.get(participant.playerId());
			if (existing != null && !existing.equals(replacingRoundId)) {
				throw new IllegalStateException("player already belongs to a golf context");
			}
		}
	}

	private void index(ReadyGolfRound round) {
		for (ReadyGolfParticipant participant : round.participants()) {
			if (participant.isParticipating()) {
				membership.put(participant.playerId(), round.roundId());
			}
		}
	}

	private void deindex(ReadyGolfRound round) {
		for (ReadyGolfParticipant participant : round.participants()) {
			if (membership.get(participant.playerId()) != null
					&& membership.get(participant.playerId()).equals(round.roundId())) {
				membership.remove(participant.playerId());
			}
		}
	}

	private ReadyGolfRound requireRound(UUID roundId) {
		Objects.requireNonNull(roundId, "roundId");
		ReadyGolfRound round = rounds.get(roundId);
		if (round == null) {
			throw new IllegalArgumentException("round does not exist");
		}
		return round;
	}
}
