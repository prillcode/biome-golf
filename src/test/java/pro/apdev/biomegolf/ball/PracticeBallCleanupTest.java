package pro.apdev.biomegolf.ball;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PracticeBallCleanupTest {
	private static final UUID ALICE = id(1);
	private static final UUID BOB = id(2);
	private static final UUID PRACTICE_BALL = id(3);
	private static final UUID ACTIVE_BALL = id(4);

	@Test
	void clearsOwnedPracticeBallWhenItIsNotAssignedToPlay() {
		assertTrue(PracticeBallCleanup.shouldClear(
			ALICE, ALICE, true, ACTIVE_BALL, PRACTICE_BALL));
	}

	@Test
	void preservesAssignedInPlayBallEvenIfItHasPracticeMarker() {
		assertFalse(PracticeBallCleanup.shouldClear(
			ALICE, ALICE, true, ACTIVE_BALL, ACTIVE_BALL));
	}

	@Test
	void preservesAnotherPlayersPracticeBall() {
		assertFalse(PracticeBallCleanup.shouldClear(
			ALICE, BOB, true, null, PRACTICE_BALL));
	}

	@Test
	void preservesOwnedBallThatWasNotExplicitlyMarkedForPractice() {
		assertFalse(PracticeBallCleanup.shouldClear(
			ALICE, ALICE, false, null, PRACTICE_BALL));
	}

	private static UUID id(long value) {
		return new UUID(0L, value);
	}
}
