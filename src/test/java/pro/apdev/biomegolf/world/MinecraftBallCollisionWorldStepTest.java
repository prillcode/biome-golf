package pro.apdev.biomegolf.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class MinecraftBallCollisionWorldStepTest {

	private static final double TOL = 1.0E-9;
	private static final AABB BALL_BOX = new AABB(-0.25, 0.0, -0.25, 0.25, 0.5, 0.25);

	@Test
	void supportedRollingBallTakesOneBlockAlternatePath() {
		Vec3 requested = new Vec3(0.3, 0.0, 0.0);
		Vec3 stepped = MinecraftBallCollisionWorld.tryStepMove(
				BALL_BOX, requested, Vec3.ZERO,
				(box, move) -> {
					if (move.y < 0.0) {
						return Vec3.ZERO; // support below, then top of the ledge
					}
					return move; // full rise and horizontal clearance
				});

		assertEquals(0.3, stepped.x, TOL);
		assertEquals(1.0, stepped.y, TOL);
		assertEquals(0.0, stepped.z, TOL);
	}

	@Test
	void halfBlockTopSettlesAtItsActualHeight() {
		Vec3 stepped = MinecraftBallCollisionWorld.tryStepMove(
				BALL_BOX, new Vec3(0.3, 0.0, 0.0), Vec3.ZERO,
				(box, move) -> {
					if (move.y < 0.0 && move.y > -0.75) {
						return Vec3.ZERO; // near-ground support probe
					}
					if (move.y < 0.0) {
						return new Vec3(0.0, -0.5, 0.0);
					}
					return move;
				});

		assertEquals(0.5, stepped.y, TOL);
	}

	@Test
	void unsupportedAirborneBallDoesNotStep() {
		Vec3 requested = new Vec3(0.3, 0.0, 0.0);
		Vec3 stepped = MinecraftBallCollisionWorld.tryStepMove(
				BALL_BOX, requested, Vec3.ZERO,
				(box, move) -> move); // downward probe is unobstructed

		assertNull(stepped);
	}

	@Test
	void unsupportedRisingBallKeepsNormalWallCollision() {
		AtomicInteger sweeps = new AtomicInteger();
		Vec3 stepped = MinecraftBallCollisionWorld.tryStepMove(
				BALL_BOX, new Vec3(0.3, 0.05, 0.0), Vec3.ZERO,
				(box, move) -> {
					sweeps.incrementAndGet();
					return move;
				});

		assertNull(stepped);
		assertEquals(1, sweeps.get());
	}

	@Test
	void nearGroundRisingAfterBounceCanTakeStepPath() {
		Vec3 stepped = MinecraftBallCollisionWorld.tryStepMove(
				BALL_BOX, new Vec3(0.3, 0.05, 0.0), Vec3.ZERO,
				(box, move) -> {
					if (move.y == -1.0) {
						return new Vec3(0.0, -0.3, 0.0); // lower ground nearby
					}
					if (move.y < 0.0) {
						return Vec3.ZERO; // ledge top
					}
					return move;
				});

		assertEquals(0.3, stepped.x, TOL);
		assertEquals(1.0, stepped.y, TOL);
	}

	@Test
	void nearGroundDescendingBallCanTakeStepPath() {
		Vec3 stepped = MinecraftBallCollisionWorld.tryStepMove(
				BALL_BOX, new Vec3(0.3, -0.1, 0.0), Vec3.ZERO,
				(box, move) -> {
					if (move.y == -1.0) {
						return new Vec3(0.0, -0.2, 0.0); // lower ground is nearby
					}
					if (move.y < -1.0) {
						return new Vec3(0.0, -0.25, 0.0); // ledge top
					}
					return move;
				});

		assertEquals(0.3, stepped.x, TOL);
		assertEquals(0.75, stepped.y, TOL);
	}

	@Test
	void tallerSheerFaceStillBlocksHorizontalProgress() {
		Vec3 stepped = MinecraftBallCollisionWorld.tryStepMove(
				BALL_BOX, new Vec3(0.3, 0.0, 0.0), Vec3.ZERO,
				(box, move) -> {
					if (move.y < 0.0) {
						return Vec3.ZERO;
					}
					if (move.y > 0.0) {
						return move;
					}
					return Vec3.ZERO; // still blocked after the maximum rise
				});

		assertNull(stepped);
	}
}
