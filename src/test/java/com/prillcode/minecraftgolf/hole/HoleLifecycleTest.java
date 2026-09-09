package com.prillcode.minecraftgolf.hole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;

class HoleLifecycleTest {

	private static final UUID PLAYER_ID = UUID.randomUUID();
	private static final HoleDefinition HOLE = new HoleDefinition(
		"test:1", 1, "minecraft:overworld",
		new Vec3(1.0, 1.0, 1.0), new Vec3(9.0, 1.0, 9.0), 4,
		new HoleBoundary(Vec3.ZERO, new Vec3(10.0, 10.0, 10.0)));

	@Test
	void playerWithoutSessionIsInPracticeMode() {
		HoleLifecycle lifecycle = new HoleLifecycle();

		assertTrue(lifecycle.allowsPracticeBall(PLAYER_ID));
		assertEquals(HoleLifecycle.Status.PRACTICE, lifecycle.status(PLAYER_ID, false));
		assertEquals(
			HoleLifecycle.ShotPermission.PRACTICE,
			lifecycle.shotPermission(PLAYER_ID, UUID.randomUUID(), false));
	}

	@Test
	void practiceBallDropIsBlockedUntilAttemptIsAbandoned() {
		HoleLifecycle lifecycle = new HoleLifecycle();
		lifecycle.start(PLAYER_ID, HOLE, UUID.randomUUID());

		assertFalse(lifecycle.allowsPracticeBall(PLAYER_ID));

		PlayerHoleSession completed = lifecycle.session(PLAYER_ID).orElseThrow().pickUp();
		lifecycle.update(PLAYER_ID, completed);
		assertFalse(lifecycle.allowsPracticeBall(PLAYER_ID));

		lifecycle.abandon(PLAYER_ID);
		assertTrue(lifecycle.allowsPracticeBall(PLAYER_ID));
	}

	@Test
	void activeSessionScoresOnlyItsAssignedBall() {
		HoleLifecycle lifecycle = new HoleLifecycle();
		UUID assignedBall = UUID.randomUUID();
		lifecycle.start(PLAYER_ID, HOLE, assignedBall);

		assertEquals(HoleLifecycle.Status.ACTIVE, lifecycle.status(PLAYER_ID, true));
		assertEquals(
			HoleLifecycle.ShotPermission.SCORING,
			lifecycle.shotPermission(PLAYER_ID, assignedBall, true));
		assertEquals(
			HoleLifecycle.ShotPermission.WRONG_BALL,
			lifecycle.shotPermission(PLAYER_ID, UUID.randomUUID(), true));
	}

	@Test
	void completedSessionRejectsFurtherShotsEvenAfterBallRemoval() {
		HoleLifecycle lifecycle = new HoleLifecycle();
		UUID assignedBall = UUID.randomUUID();
		lifecycle.start(PLAYER_ID, HOLE, assignedBall);
		PlayerHoleSession completed = lifecycle.session(PLAYER_ID).orElseThrow()
			.pickUp();
		lifecycle.update(PLAYER_ID, completed);

		assertEquals(HoleLifecycle.Status.COMPLETE, lifecycle.status(PLAYER_ID, false));
		assertEquals(
			HoleLifecycle.ShotPermission.HOLE_COMPLETE,
			lifecycle.shotPermission(PLAYER_ID, assignedBall, false));
	}

	@Test
	void missingAssignedBallIsAnExplicitRecoverableState() {
		HoleLifecycle lifecycle = new HoleLifecycle();
		UUID assignedBall = UUID.randomUUID();
		lifecycle.start(PLAYER_ID, HOLE, assignedBall);

		assertEquals(HoleLifecycle.Status.MISSING_BALL, lifecycle.status(PLAYER_ID, false));
		assertEquals(
			HoleLifecycle.ShotPermission.MISSING_BALL,
			lifecycle.shotPermission(PLAYER_ID, UUID.randomUUID(), false));
	}

	@Test
	void startDoesNotReplaceAnExistingAttempt() {
		HoleLifecycle lifecycle = new HoleLifecycle();
		UUID originalBall = UUID.randomUUID();
		lifecycle.start(PLAYER_ID, HOLE, originalBall);
		lifecycle.update(
			PLAYER_ID,
			lifecycle.session(PLAYER_ID).orElseThrow().recordAcceptedShot(HOLE.tee()));

		assertEquals(
			HoleLifecycle.StartOutcome.ALREADY_ACTIVE,
			lifecycle.start(PLAYER_ID, HOLE, UUID.randomUUID()));
		assertEquals(originalBall, lifecycle.session(PLAYER_ID).orElseThrow().ballUuid());
		assertEquals(1, lifecycle.session(PLAYER_ID).orElseThrow().state().strokes());
	}

	@Test
	void restartReplacesBallAndResetsScoreWithoutLeakingOldState() {
		HoleLifecycle lifecycle = new HoleLifecycle();
		UUID originalBall = UUID.randomUUID();
		UUID replacementBall = UUID.randomUUID();
		lifecycle.start(PLAYER_ID, HOLE, originalBall);
		lifecycle.update(
			PLAYER_ID,
			lifecycle.session(PLAYER_ID).orElseThrow().recordAcceptedShot(HOLE.tee()));

		assertEquals(
			HoleLifecycle.RestartOutcome.RESTARTED,
			lifecycle.restart(PLAYER_ID, HOLE, replacementBall));

		PlayerHoleSession restarted = lifecycle.session(PLAYER_ID).orElseThrow();
		assertEquals(0, restarted.state().strokes());
		assertEquals(replacementBall, restarted.ballUuid());
		assertNotEquals(originalBall, restarted.ballUuid());
	}

	@Test
	void restartRequiresAnExistingAttempt() {
		HoleLifecycle lifecycle = new HoleLifecycle();

		assertEquals(
			HoleLifecycle.RestartOutcome.NO_ACTIVE_HOLE,
			lifecycle.restart(PLAYER_ID, HOLE, UUID.randomUUID()));
		assertTrue(lifecycle.session(PLAYER_ID).isEmpty());
	}

	@Test
	void abandonRemovesSessionAndReturnsPlayerToPractice() {
		HoleLifecycle lifecycle = new HoleLifecycle();
		lifecycle.start(PLAYER_ID, HOLE, UUID.randomUUID());

		assertEquals(HoleLifecycle.AbandonOutcome.ABANDONED, lifecycle.abandon(PLAYER_ID));
		assertTrue(lifecycle.session(PLAYER_ID).isEmpty());
		assertEquals(HoleLifecycle.Status.PRACTICE, lifecycle.status(PLAYER_ID, false));
		assertEquals(HoleLifecycle.AbandonOutcome.NO_ACTIVE_HOLE, lifecycle.abandon(PLAYER_ID));
	}

	@Test
	void clearDropsAllSessionsForServiceReinitialization() {
		HoleLifecycle lifecycle = new HoleLifecycle();
		lifecycle.start(PLAYER_ID, HOLE, UUID.randomUUID());

		lifecycle.clear();

		assertTrue(lifecycle.session(PLAYER_ID).isEmpty());
	}
}
