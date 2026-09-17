package com.prillcode.minecraftgolf.round;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.GeneratedLayoutIdentity;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleDefinition;
import com.prillcode.minecraftgolf.course.HoleTransition;

class ReadyGolfRoundRegistryTest {
	private static final UUID ROUND_ONE = id(10);
	private static final UUID ROUND_TWO = id(11);
	private static final UUID ALICE = id(1);
	private static final UUID BOB = id(2);
	private static final UUID CAROL = id(3);
	private static final UUID DAVE = id(4);
	private static final UUID ERIN = id(5);

	@Test
	void supportsDifferentAndSameCourseRoundsWithPerRoundCapacity() {
		ReadyGolfRoundRegistry registry = new ReadyGolfRoundRegistry();
		ReadyGolfRound first = registry.create(ROUND_ONE, course("one"), ALICE);
		ReadyGolfRound second = registry.create(ROUND_TWO, course("two"), BOB);
		assertEquals(2, registry.listOpen().size());

		registry.create(id(12), course("one"), CAROL);
		assertEquals(first.course(), registry.find(ROUND_ONE).orElseThrow().course());

		registry.join(ROUND_ONE, DAVE);
		registry.join(ROUND_ONE, id(6));
		registry.join(ROUND_ONE, id(7));
		assertEquals(4, registry.find(ROUND_ONE).orElseThrow().participants().size());
		registry.join(ROUND_TWO, ERIN);
		assertThrows(IllegalStateException.class, () -> registry.join(ROUND_ONE, id(7)));
	}

	@Test
	void onePlayerCannotCreateOrJoinASecondRoundAndListingIsStable() {
		ReadyGolfRoundRegistry registry = new ReadyGolfRoundRegistry();
		registry.create(ROUND_ONE, course("same"), ALICE);
		registry.create(ROUND_TWO, course("same"), BOB);
		assertEquals(List.of(ROUND_ONE, ROUND_TWO), registry.listOpen().stream()
			.map(ReadyGolfRound::roundId).toList());
		assertThrows(IllegalStateException.class, () -> registry.join(ROUND_TWO, ALICE));
		assertThrows(IllegalStateException.class, () -> registry.create(id(13), course("other"), ALICE));
	}

	@Test
	void startingOneLobbyDoesNotChangeAnotherLobby() {
		ReadyGolfRoundRegistry registry = new ReadyGolfRoundRegistry();
		registry.create(ROUND_ONE, course("same"), ALICE);
		registry.create(ROUND_TWO, course("same"), BOB);
		registry.update(ROUND_ONE, registry.find(ROUND_ONE).orElseThrow(), round -> round.start(ALICE));
		assertEquals(RoundPhase.PLAYING, registry.find(ROUND_ONE).orElseThrow().phase());
		assertEquals(List.of(ROUND_TWO), registry.listOpen().stream().map(ReadyGolfRound::roundId).toList());
		assertEquals(RoundPhase.LOBBY, registry.find(ROUND_TWO).orElseThrow().phase());
	}

	@Test
	void staleSnapshotAndDuplicateIdFailWithoutMutation() {
		ReadyGolfRoundRegistry registry = new ReadyGolfRoundRegistry();
		ReadyGolfRound original = registry.create(ROUND_ONE, course("same"), ALICE);
		assertThrows(IllegalStateException.class, () -> registry.create(ROUND_ONE, course("other"), BOB));
		registry.join(ROUND_ONE, BOB);
		assertThrows(IllegalStateException.class,
			() -> registry.update(ROUND_ONE, original, round -> round.start(ALICE)));
		assertThrows(IllegalStateException.class,
			() -> registry.update(ROUND_ONE, 0L, round -> round.start(ALICE)));
		assertEquals(2, registry.find(ROUND_ONE).orElseThrow().participants().size());
		assertEquals(1L, registry.version(ROUND_ONE));
	}

	@Test
	void disconnectReconnectLeaveAndEmptyCleanupOnlyAffectTargetRound() {
		ReadyGolfRoundRegistry registry = new ReadyGolfRoundRegistry();
		registry.create(ROUND_ONE, course("same"), ALICE);
		registry.create(ROUND_TWO, course("same"), BOB);
		registry.update(ROUND_ONE, registry.find(ROUND_ONE).orElseThrow(), round -> round.start(ALICE));
		registry.disconnect(ROUND_ONE, ALICE);
		assertEquals(ParticipantStatus.SUSPENDED,
			registry.find(ROUND_ONE).orElseThrow().findParticipant(ALICE).orElseThrow().status());
		assertEquals(ROUND_ONE, registry.findByPlayer(ALICE).orElseThrow().roundId());
		registry.reconnect(ROUND_ONE, ALICE);
		registry.leave(ROUND_ONE, ALICE);
		assertTrue(registry.findByPlayer(ALICE).isEmpty());
		assertTrue(registry.find(ROUND_ONE).isEmpty());
		assertTrue(registry.find(ROUND_TWO).isPresent());
	}

	@Test
	void withdrawnParticipantsDoNotRemainIndexedAndCompletedDisconnectIsLeave() {
		ReadyGolfRoundRegistry registry = new ReadyGolfRoundRegistry();
		registry.create(ROUND_ONE, oneHoleCourse(), ALICE);
		registry.update(ROUND_ONE, registry.find(ROUND_ONE).orElseThrow(), round -> round.start(ALICE));
		registry.withdraw(ROUND_ONE, ALICE);
		assertTrue(registry.findByPlayer(ALICE).isEmpty());
		assertTrue(registry.find(ROUND_ONE).isEmpty());

		registry.create(ROUND_TWO, oneHoleCourse(), ALICE);
		ReadyGolfRound playing = registry.update(ROUND_TWO, registry.find(ROUND_TWO).orElseThrow(), round -> round.start(ALICE));
		ReadyGolfRound complete = playing.updateCurrentHole(ALICE,
			playing.findParticipant(ALICE).orElseThrow().courseState().currentHole().pickUp());
		registry.update(ROUND_TWO, playing, complete);
		registry.disconnect(ROUND_TWO, ALICE);
		assertTrue(registry.findByPlayer(ALICE).isEmpty());
		assertTrue(registry.find(ROUND_TWO).isEmpty());
	}

	private static UUID id(int value) {
		return new UUID(0, value);
	}

	private static CourseDefinition course(String id) {
		return new CourseDefinition(id, id, "minecraft:overworld", new GeneratedLayoutIdentity(id, 1),
			List.of(hole(1, 4), hole(2, 3)));
	}

	private static CourseDefinition oneHoleCourse() {
		return new CourseDefinition("one", "one", "minecraft:overworld", new GeneratedLayoutIdentity("one", 1),
			List.of(hole(1, 4)));
	}

	private static HoleDefinition hole(int number, int par) {
		Vec3 tee = new Vec3(0, 64, 0);
		return new HoleDefinition("hole:" + number, number, "minecraft:overworld", tee,
			new Vec3(10, 64, 0), par, new HoleBoundary(new Vec3(-10, 0, -10), new Vec3(20, 100, 10)),
			new GeneratedLayoutIdentity("hole:" + number, 1), HoleTransition.at(tee));
	}

}
