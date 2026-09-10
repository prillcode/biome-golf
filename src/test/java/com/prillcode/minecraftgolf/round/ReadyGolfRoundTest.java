package com.prillcode.minecraftgolf.round;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.GeneratedLayoutIdentity;
import com.prillcode.minecraftgolf.course.HoleTransition;
import com.prillcode.minecraftgolf.course.PlayerCourseState;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleDefinition;
import com.prillcode.minecraftgolf.hole.PenaltyType;
import com.prillcode.minecraftgolf.hole.PlayerHoleState;

class ReadyGolfRoundTest {
	private static final UUID ROUND_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
	private static final UUID PLAYER_ONE = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID PLAYER_TWO = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID PLAYER_THREE = UUID.fromString("00000000-0000-0000-0000-000000000003");
	private static final UUID PLAYER_FOUR = UUID.fromString("00000000-0000-0000-0000-000000000004");
	private static final UUID PLAYER_FIVE = UUID.fromString("00000000-0000-0000-0000-000000000005");

	@Test
	void twoPlayersStartOnHoleOneWithIndependentStateInJoinOrder() {
		ReadyGolfRound round = playingRound();

		assertEquals(RoundPhase.PLAYING, round.phase());
		assertEquals(List.of(PLAYER_ONE, PLAYER_TWO),
			round.participants().stream().map(ReadyGolfParticipant::playerId).toList());
		assertEquals(1, state(round, PLAYER_ONE).currentHole().hole().number());
		assertEquals(1, state(round, PLAYER_TWO).currentHole().hole().number());
		assertNotSame(state(round, PLAYER_ONE), state(round, PLAYER_TWO));
	}

	@Test
	void playerUpdatesAndRestartNeverAlterPeerState() {
		assertPeerUnchanged(round -> update(round, PLAYER_ONE,
			state(round, PLAYER_ONE).currentHole().recordAcceptedShot()));
		assertPeerUnchanged(round -> update(round, PLAYER_ONE,
			state(round, PLAYER_ONE).currentHole().applyPenalty(PenaltyType.WATER)));
		assertPeerUnchanged(round -> round.restartCurrentHole(PLAYER_ONE));
		assertPeerUnchanged(round -> update(round, PLAYER_ONE,
			state(round, PLAYER_ONE).currentHole().pickUp()));
		assertPeerUnchanged(round -> {
			PlayerHoleState holedOut = state(round, PLAYER_ONE).currentHole()
				.recordAcceptedShot().holeOut();
			return update(round, PLAYER_ONE, holedOut);
		});
	}

	@Test
	void oneTerminalPlayerCannotAdvanceUntilAllActivePlayersAreTerminal() {
		ReadyGolfRound round = playingRound();
		round = holeOut(round, PLAYER_ONE);

		assertFalse(round.allActiveTerminal());
		assertEquals(2, round.activeParticipantCount());
		assertEquals(1, round.terminalActiveParticipantCount());
		ReadyGolfRound blocked = round;
		assertThrows(IllegalStateException.class, () -> blocked.advanceNextHole(0));
		assertEquals(0, blocked.currentHoleIndex());
		assertEquals(1, state(blocked, PLAYER_TWO).currentHole().hole().number());
	}

	@Test
	void allTerminalPlayersAdvanceTogetherExactlyOnce() {
		ReadyGolfRound terminal = holeOut(playingRound(), PLAYER_ONE);
		terminal = update(terminal, PLAYER_TWO, state(terminal, PLAYER_TWO).currentHole().pickUp());

		ReadyGolfRound advanced = terminal.advanceNextHole(0);

		assertEquals(1, advanced.currentHoleIndex());
		assertEquals(2, state(advanced, PLAYER_ONE).currentHole().hole().number());
		assertEquals(2, state(advanced, PLAYER_TWO).currentHole().hole().number());
		assertEquals(1, state(advanced, PLAYER_ONE).completedStrokes());
		assertEquals(10, state(advanced, PLAYER_TWO).completedStrokes());
		assertThrows(IllegalStateException.class, () -> advanced.advanceNextHole(0));
		assertEquals(1, advanced.currentHoleIndex());
	}

	@Test
	void lastFinalHoleTerminalUpdateCompletesRoundAndPreservesEveryScorecard() {
		ReadyGolfRound round = playingRound();
		for (int holeIndex = 0; holeIndex < 2; holeIndex++) {
			round = holeOut(round, PLAYER_ONE);
			round = update(round, PLAYER_TWO, state(round, PLAYER_TWO).currentHole().pickUp());
			round = round.advanceNextHole(holeIndex);
		}

		round = holeOut(round, PLAYER_ONE);
		assertEquals(RoundPhase.PLAYING, round.phase());
		round = update(round, PLAYER_TWO, state(round, PLAYER_TWO).currentHole().pickUp());

		assertEquals(RoundPhase.COMPLETE, round.phase());
		assertEquals(3, round.currentHoleIndex());
		assertEquals(3, state(round, PLAYER_ONE).finalScorecard().totalStrokes());
		assertEquals(30, state(round, PLAYER_TWO).finalScorecard().totalStrokes());
		assertEquals(List.of(PLAYER_ONE, PLAYER_TWO),
			round.participants().stream().map(ReadyGolfParticipant::playerId).toList());
		ReadyGolfRound completed = round;
		assertThrows(IllegalStateException.class, () -> completed.advanceNextHole(2));
	}

	@Test
	void reconnectBeforeAdvanceRestoresTheSameAuthoritativeState() {
		ReadyGolfRound round = playingRound();
		round = update(round, PLAYER_TWO,
			state(round, PLAYER_TWO).currentHole().recordAcceptedShot());
		PlayerCourseState beforeDisconnect = state(round, PLAYER_TWO);

		round = round.disconnect(PLAYER_TWO);
		assertEquals(ParticipantStatus.SUSPENDED, participant(round, PLAYER_TWO).status());
		assertEquals(beforeDisconnect, state(round, PLAYER_TWO));

		round = round.reconnect(PLAYER_TWO);
		assertEquals(ParticipantStatus.ACTIVE, participant(round, PLAYER_TWO).status());
		assertEquals(beforeDisconnect, state(round, PLAYER_TWO));
	}

	@Test
	void advancingWithoutSuspendedPlayerWithdrawsTheirStaleState() {
		ReadyGolfRound round = playingRound().disconnect(PLAYER_TWO);
		round = holeOut(round, PLAYER_ONE);

		assertTrue(round.allActiveTerminal());
		round = round.advanceNextHole(0);

		assertEquals(ParticipantStatus.WITHDRAWN, participant(round, PLAYER_TWO).status());
		assertEquals(0, state(round, PLAYER_TWO).currentHoleIndex());
		assertEquals(1, state(round, PLAYER_ONE).currentHoleIndex());
		ReadyGolfRound advanced = round;
		assertThrows(IllegalStateException.class, () -> advanced.reconnect(PLAYER_TWO));
	}

	@Test
	void finalHoleDisconnectCompletesRoundWhenRemainingGolferIsTerminal() {
		ReadyGolfRound round = playingRound();
		for (int holeIndex = 0; holeIndex < 2; holeIndex++) {
			round = holeOut(round, PLAYER_ONE);
			round = update(round, PLAYER_TWO, state(round, PLAYER_TWO).currentHole().pickUp());
			round = round.advanceNextHole(holeIndex);
		}
		round = holeOut(round, PLAYER_ONE);

		round = round.disconnect(PLAYER_TWO);

		assertEquals(RoundPhase.COMPLETE, round.phase());
		assertEquals(ParticipantStatus.ACTIVE, participant(round, PLAYER_ONE).status());
		assertEquals(ParticipantStatus.WITHDRAWN, participant(round, PLAYER_TWO).status());
		assertTrue(state(round, PLAYER_ONE).isComplete());
		assertEquals(3, state(round, PLAYER_ONE).finalScorecard().totalStrokes());
	}

	@Test
	void withdrawalReevaluatesBarrierWithoutChangingRemainingScore() {
		ReadyGolfRound round = holeOut(playingRound(), PLAYER_ONE);
		PlayerCourseState completed = state(round, PLAYER_ONE);

		round = round.withdraw(PLAYER_TWO);

		assertTrue(round.allActiveTerminal());
		assertEquals(1, round.activeParticipantCount());
		assertEquals(1, round.terminalActiveParticipantCount());
		assertEquals(completed, state(round, PLAYER_ONE));
		assertEquals(ParticipantStatus.WITHDRAWN, participant(round, PLAYER_TWO).status());
	}

	@Test
	void allDisconnectedRoundRemainsSuspendedWithoutOpeningBarrier() {
		ReadyGolfRound round = playingRound()
			.disconnect(PLAYER_ONE)
			.disconnect(PLAYER_TWO);

		assertEquals(RoundPhase.PLAYING, round.phase());
		assertFalse(round.allActiveTerminal());
		ReadyGolfRound suspended = round;
		assertThrows(IllegalStateException.class, () -> suspended.advanceNextHole(0));

		round = round.reconnect(PLAYER_ONE);
		assertEquals(ParticipantStatus.ACTIVE, participant(round, PLAYER_ONE).status());
		assertEquals(ParticipantStatus.SUSPENDED, participant(round, PLAYER_TWO).status());
	}

	@Test
	void lastParticipantWithdrawalEndsRoundWithoutInventingAScorecard() {
		ReadyGolfRound round = ReadyGolfRound.create(ROUND_ID, course(), PLAYER_ONE)
			.start(PLAYER_ONE)
			.withdraw(PLAYER_ONE);

		assertEquals(RoundPhase.COMPLETE, round.phase());
		assertEquals(ParticipantStatus.WITHDRAWN, participant(round, PLAYER_ONE).status());
		assertFalse(state(round, PLAYER_ONE).isComplete());
	}

	@Test
	void completedSoloRoundCanReplayButCompletedMultiplayerRoundCannot() {
		ReadyGolfRound solo = ReadyGolfRound.create(ROUND_ID, course(), PLAYER_ONE)
			.start(PLAYER_ONE);
		for (int holeIndex = 0; holeIndex < 2; holeIndex++) {
			solo = holeOut(solo, PLAYER_ONE).advanceNextHole(holeIndex);
		}
		solo = holeOut(solo, PLAYER_ONE);

		ReadyGolfRound replay = solo.replaySolo(PLAYER_ONE);
		assertEquals(RoundPhase.PLAYING, replay.phase());
		assertEquals(0, replay.currentHoleIndex());
		assertEquals(0, state(replay, PLAYER_ONE).currentHole().strokes());
		assertTrue(state(replay, PLAYER_ONE).completedHoles().isEmpty());

		ReadyGolfRound multiplayer = completeRound(playingRound());
		assertThrows(IllegalStateException.class, () -> multiplayer.replaySolo(PLAYER_ONE));
	}

	@Test
	void lobbyCoordinatorTransfersInStableOrderAndEmptyLobbyCloses() {
		ReadyGolfRound round = ReadyGolfRound.create(ROUND_ID, course(), PLAYER_ONE)
			.join(PLAYER_TWO)
			.join(PLAYER_THREE);

		round = round.disconnect(PLAYER_ONE);
		assertEquals(PLAYER_TWO, round.coordinatorId().orElseThrow());
		assertEquals(List.of(PLAYER_TWO, PLAYER_THREE),
			round.participants().stream().map(ReadyGolfParticipant::playerId).toList());
		ReadyGolfRound transferred = round;
		assertThrows(IllegalStateException.class, () -> transferred.start(PLAYER_THREE));

		round = round.withdraw(PLAYER_TWO).withdraw(PLAYER_THREE);
		assertEquals(RoundPhase.COMPLETE, round.phase());
		assertTrue(round.participants().isEmpty());
	}

	@Test
	void duplicateFullAndLateJoinsFailWithoutMutatingRound() {
		ReadyGolfRound lobby = ReadyGolfRound.create(ROUND_ID, course(), PLAYER_ONE);
		assertThrows(IllegalStateException.class, () -> lobby.join(PLAYER_ONE));
		assertEquals(List.of(PLAYER_ONE),
			lobby.participants().stream().map(ReadyGolfParticipant::playerId).toList());

		ReadyGolfRound full = lobby.join(PLAYER_TWO).join(PLAYER_THREE).join(PLAYER_FOUR);
		assertThrows(IllegalStateException.class, () -> full.join(PLAYER_FIVE));
		assertEquals(4, full.participants().size());

		ReadyGolfRound playing = full.start(PLAYER_ONE);
		assertThrows(IllegalStateException.class, () -> playing.join(PLAYER_FIVE));
		assertEquals(4, playing.participants().size());
	}

	@Test
	void invalidPlayerAndHoleUpdatesFailWithoutMutation() {
		ReadyGolfRound round = playingRound();
		assertThrows(IllegalArgumentException.class,
			() -> round.updateCurrentHole(PLAYER_FIVE, state(round, PLAYER_ONE).currentHole()));
		assertThrows(IllegalArgumentException.class,
			() -> round.updateCurrentHole(PLAYER_ONE, PlayerHoleState.start(course().hole(2))));
		assertEquals(0, state(round, PLAYER_ONE).currentHole().strokes());
		assertEquals(0, state(round, PLAYER_TWO).currentHole().strokes());
	}

	private static void assertPeerUnchanged(UnaryOperator<ReadyGolfRound> playerOneAction) {
		ReadyGolfRound before = playingRound();
		PlayerCourseState peerBefore = state(before, PLAYER_TWO);

		ReadyGolfRound after = playerOneAction.apply(before);

		assertEquals(peerBefore, state(after, PLAYER_TWO));
	}

	private static ReadyGolfRound playingRound() {
		return ReadyGolfRound.create(ROUND_ID, course(), PLAYER_ONE)
			.join(PLAYER_TWO)
			.start(PLAYER_ONE);
	}

	private static ReadyGolfRound holeOut(ReadyGolfRound round, UUID playerId) {
		PlayerHoleState completed = state(round, playerId).currentHole()
			.recordAcceptedShot()
			.holeOut();
		return update(round, playerId, completed);
	}

	private static ReadyGolfRound completeRound(ReadyGolfRound round) {
		for (int holeIndex = round.currentHoleIndex(); holeIndex < 2; holeIndex++) {
			round = holeOut(round, PLAYER_ONE);
			round = holeOut(round, PLAYER_TWO);
			round = round.advanceNextHole(holeIndex);
		}
		round = holeOut(round, PLAYER_ONE);
		return holeOut(round, PLAYER_TWO);
	}

	private static ReadyGolfRound update(ReadyGolfRound round, UUID playerId,
			PlayerHoleState updatedHole) {
		return round.updateCurrentHole(playerId, updatedHole);
	}

	private static ReadyGolfParticipant participant(ReadyGolfRound round, UUID playerId) {
		return round.findParticipant(playerId).orElseThrow();
	}

	private static PlayerCourseState state(ReadyGolfRound round, UUID playerId) {
		return participant(round, playerId).courseState();
	}

	private static CourseDefinition course() {
		return new CourseDefinition("test", "Test Course", "minecraft:overworld",
			new GeneratedLayoutIdentity("test-course", 1),
			List.of(hole(1, 4), hole(2, 3), hole(3, 5)));
	}

	private static HoleDefinition hole(int number, int par) {
		Vec3 tee = new Vec3(number * 20.0, 64.25, 0.0);
		Vec3 cup = new Vec3(number * 20.0 + 10.0, 64.25, 0.0);
		return new HoleDefinition("test:" + number, number, "minecraft:overworld", tee, cup, par,
			new HoleBoundary(new Vec3(0.0, 0.0, -10.0), new Vec3(100.0, 100.0, 10.0)),
			new GeneratedLayoutIdentity("test-hole-" + number, 1), HoleTransition.at(tee));
	}
}
