package pro.apdev.biomegolf.ball;

import java.util.UUID;

/** Pure ownership and assignment rules for the player-scoped practice-ball clear action. */
public final class PracticeBallCleanup {

	private PracticeBallCleanup() {
	}

	public static boolean shouldClear(
			UUID playerId,
			UUID ownerId,
			boolean practiceBall,
			UUID assignedBallId,
			UUID candidateBallId) {
		return playerId != null
			&& playerId.equals(ownerId)
			&& practiceBall
			&& (assignedBallId == null || !assignedBallId.equals(candidateBallId));
	}
}
