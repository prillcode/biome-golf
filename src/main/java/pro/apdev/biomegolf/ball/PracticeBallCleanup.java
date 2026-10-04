package pro.apdev.biomegolf.ball;

import java.util.Set;
import java.util.UUID;

/** Pure ownership and assignment rules for the player- and operator-scoped practice-ball clear actions. */
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

	/**
	 * Operator scoped: removes an explicit practice ball regardless of owner, while always
	 * preserving a ball that is currently assigned to an active round session.
	 */
	public static boolean shouldClearAll(
			boolean practiceBall,
			Set<UUID> inPlayBallIds,
			UUID candidateBallId) {
		return practiceBall
			&& (inPlayBallIds == null || !inPlayBallIds.contains(candidateBallId));
	}
}
