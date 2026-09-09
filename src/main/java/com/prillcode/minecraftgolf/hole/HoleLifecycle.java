package com.prillcode.minecraftgolf.hole;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Minecraft-free, server-owned registry and policy for one player's configured-hole session.
 *
 * <p>The integration layer remains responsible for spawning and locating entities. It reports
 * whether the assigned ball still exists so this policy can distinguish practice, active,
 * completed, and recoverable missing-ball states without depending on Minecraft classes.</p>
 */
public final class HoleLifecycle {

	public enum Status {
		PRACTICE,
		ACTIVE,
		COMPLETE,
		MISSING_BALL
	}

	public enum ShotPermission {
		PRACTICE,
		SCORING,
		WRONG_BALL,
		HOLE_COMPLETE,
		MISSING_BALL
	}

	public enum StartOutcome {
		STARTED,
		ALREADY_ACTIVE
	}

	public enum RestartOutcome {
		RESTARTED,
		NO_ACTIVE_HOLE
	}

	public enum AbandonOutcome {
		ABANDONED,
		NO_ACTIVE_HOLE
	}

	private final Map<UUID, PlayerHoleSession> sessions = new HashMap<>();

	public StartOutcome start(UUID playerId, HoleDefinition hole, UUID ballId) {
		Objects.requireNonNull(playerId, "playerId");
		Objects.requireNonNull(hole, "hole");
		Objects.requireNonNull(ballId, "ballId");
		if (sessions.containsKey(playerId)) {
			return StartOutcome.ALREADY_ACTIVE;
		}
		sessions.put(playerId, PlayerHoleSession.start(hole, ballId));
		return StartOutcome.STARTED;
	}

	public RestartOutcome restart(UUID playerId, HoleDefinition hole, UUID ballId) {
		Objects.requireNonNull(playerId, "playerId");
		Objects.requireNonNull(hole, "hole");
		Objects.requireNonNull(ballId, "ballId");
		if (!sessions.containsKey(playerId)) {
			return RestartOutcome.NO_ACTIVE_HOLE;
		}
		sessions.put(playerId, PlayerHoleSession.start(hole, ballId));
		return RestartOutcome.RESTARTED;
	}

	public AbandonOutcome abandon(UUID playerId) {
		Objects.requireNonNull(playerId, "playerId");
		return sessions.remove(playerId) == null
			? AbandonOutcome.NO_ACTIVE_HOLE
			: AbandonOutcome.ABANDONED;
	}

	public Optional<PlayerHoleSession> session(UUID playerId) {
		return Optional.ofNullable(sessions.get(Objects.requireNonNull(playerId, "playerId")));
	}

	public boolean allowsPracticeBall(UUID playerId) {
		return !sessions.containsKey(Objects.requireNonNull(playerId, "playerId"));
	}

	public void update(UUID playerId, PlayerHoleSession updatedSession) {
		Objects.requireNonNull(playerId, "playerId");
		Objects.requireNonNull(updatedSession, "updatedSession");
		PlayerHoleSession current = sessions.get(playerId);
		if (current == null) {
			throw new IllegalStateException("player has no active hole session");
		}
		if (!current.ballUuid().equals(updatedSession.ballUuid())) {
			throw new IllegalArgumentException("updated session must retain the assigned ball");
		}
		sessions.put(playerId, updatedSession);
	}

	public Status status(UUID playerId, boolean assignedBallPresent) {
		PlayerHoleSession session = sessions.get(Objects.requireNonNull(playerId, "playerId"));
		if (session == null) {
			return Status.PRACTICE;
		}
		if (session.state().isComplete()) {
			return Status.COMPLETE;
		}
		return assignedBallPresent ? Status.ACTIVE : Status.MISSING_BALL;
	}

	public ShotPermission shotPermission(
		UUID playerId,
		UUID candidateBallId,
		boolean assignedBallPresent
	) {
		Objects.requireNonNull(candidateBallId, "candidateBallId");
		PlayerHoleSession session = sessions.get(Objects.requireNonNull(playerId, "playerId"));
		if (session == null) {
			return ShotPermission.PRACTICE;
		}
		if (session.state().isComplete()) {
			return ShotPermission.HOLE_COMPLETE;
		}
		if (!assignedBallPresent) {
			return ShotPermission.MISSING_BALL;
		}
		return session.ballUuid().equals(candidateBallId)
			? ShotPermission.SCORING
			: ShotPermission.WRONG_BALL;
	}

	public void clear() {
		sessions.clear();
	}
}
