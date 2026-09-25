package pro.apdev.biomegolf.hole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.golf.Vec3;

class PlayerHoleSessionTest {

	private static final HoleDefinition HOLE = new HoleDefinition(
		"test:1", 1, "minecraft:overworld",
		new Vec3(1.0, 1.0, 1.0), new Vec3(9.0, 1.0, 9.0), 4,
		new HoleBoundary(Vec3.ZERO, new Vec3(10.0, 10.0, 10.0)));

	@Test
	void acceptedShotOriginBecomesPenaltyRecoveryPosition() {
		Vec3 shotOrigin = new Vec3(4.0, 1.0, 3.0);
		PlayerHoleSession session = PlayerHoleSession.start(HOLE, UUID.randomUUID())
			.recordAcceptedShot(shotOrigin)
			.applyPenalty(PenaltyType.WATER);

		assertEquals(shotOrigin, session.lastSafePosition());
		assertEquals(2, session.state().strokes());
		assertEquals(1, session.state().penaltyStrokes());
	}

	@Test
	void penaltyDoesNotMoveInitialTeeRecoveryPoint() {
		PlayerHoleSession session = PlayerHoleSession.start(HOLE, UUID.randomUUID())
			.applyPenalty(PenaltyType.OUT_OF_BOUNDS);

		assertEquals(HOLE.tee(), session.lastSafePosition());
	}

	@Test
	void rejectsRecoveryPositionOutsideBoundary() {
		PlayerHoleSession session = PlayerHoleSession.start(HOLE, UUID.randomUUID());
		assertThrows(IllegalArgumentException.class, () -> new PlayerHoleSession(
			PlayerHoleState.start(HOLE), UUID.randomUUID(), new Vec3(11.0, 1.0, 1.0)));
		assertThrows(IllegalArgumentException.class,
			() -> session.recordAcceptedShot(new Vec3(11.0, 1.0, 1.0)));
	}
}
