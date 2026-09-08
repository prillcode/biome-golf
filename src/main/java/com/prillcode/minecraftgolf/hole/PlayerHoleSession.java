package com.prillcode.minecraftgolf.hole;

import java.util.Objects;
import java.util.UUID;

import com.prillcode.minecraftgolf.golf.Vec3;

/** Immutable link between scoring, the assigned ball, and recovery position. */
public record PlayerHoleSession(
	PlayerHoleState state,
	UUID ballUuid,
	Vec3 lastSafePosition
) {

	public PlayerHoleSession {
		Objects.requireNonNull(state, "state");
		Objects.requireNonNull(ballUuid, "ballUuid");
		Objects.requireNonNull(lastSafePosition, "lastSafePosition");
		if (!state.hole().boundary().contains(lastSafePosition)) {
			throw new IllegalArgumentException("last safe position must be inside the hole boundary");
		}
	}

	public static PlayerHoleSession start(HoleDefinition hole, UUID ballUuid) {
		return new PlayerHoleSession(PlayerHoleState.start(hole), ballUuid, hole.tee());
	}

	/** A legal launch advances the score and makes its origin the recovery point. */
	public PlayerHoleSession recordAcceptedShot(Vec3 shotOrigin) {
		return new PlayerHoleSession(state.recordAcceptedShot(), ballUuid, shotOrigin);
	}

	/** A hazard adds its stroke without changing the previous-shot recovery point. */
	public PlayerHoleSession applyPenalty(PenaltyType penalty) {
		return new PlayerHoleSession(state.applyPenalty(penalty), ballUuid, lastSafePosition);
	}

	public PlayerHoleSession holeOut() {
		return new PlayerHoleSession(state.holeOut(), ballUuid, lastSafePosition);
	}

	public PlayerHoleSession pickUp() {
		return new PlayerHoleSession(state.pickUp(), ballUuid, lastSafePosition);
	}
}
