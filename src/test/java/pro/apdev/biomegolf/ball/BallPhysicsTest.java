package pro.apdev.biomegolf.ball;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.surface.SurfaceDefinition;

/**
 * Unit tests for {@link BallPhysics}: gravity, air drag, bounce restitution,
 * per-surface behavior, rolling friction, stop threshold, and the sub-step
 * tunneling guard. All against synthetic worlds — no Minecraft.
 */
class BallPhysicsTest {

	private static final double TOL = 1.0E-9;

	// ------------------------------------------------------------------
	// Synthetic collision worlds
	// ------------------------------------------------------------------

	/** No geometry at all: everything moves freely. */
	static final class EmptyWorld implements BallCollisionWorld {
		@Override
		public CollisionResult move(Vec3 from, Vec3 delta) {
			return CollisionResult.moved(from.add(delta));
		}

		@Override
		public SurfaceDefinition surfaceAt(Vec3 position) {
			return SurfaceDefinition.NORMAL;
		}
	}

	/** Flat floor at y=0, optionally ending at {@code floorMaxX} (gap after). */
	static final class FloorWorld implements BallCollisionWorld {
		private final SurfaceDefinition surface;
		private final double floorMaxX;

		FloorWorld(SurfaceDefinition surface) {
			this(surface, Double.MAX_VALUE);
		}

		FloorWorld(SurfaceDefinition surface, double floorMaxX) {
			this.surface = surface;
			this.floorMaxX = floorMaxX;
		}

		@Override
		public CollisionResult move(Vec3 from, Vec3 delta) {
			Vec3 end = from.add(delta);
			if (end.y() < 0.0 && end.x() <= floorMaxX) {
				return CollisionResult.hit(new Vec3(end.x(), 0.0, end.z()), Vec3.of(0, 1, 0));
			}
			return CollisionResult.moved(end);
		}

		@Override
		public SurfaceDefinition surfaceAt(Vec3 position) {
			return surface;
		}
	}

	/**
	 * Floor at y=0 plus a vertical wall slab spanning x in [wallMinX, wallMaxX].
	 * Movement uses endpoint sampling (deliberately naive) so the sub-step
	 * guard in the engine is what prevents tunneling.
	 */
	static final class WallWorld implements BallCollisionWorld {
		private final double wallMinX;
		private final double wallMaxX;
		private double maxDeltaSeen;

		WallWorld(double wallMinX, double wallMaxX) {
			this.wallMinX = wallMinX;
			this.wallMaxX = wallMaxX;
		}

		@Override
		public CollisionResult move(Vec3 from, Vec3 delta) {
			maxDeltaSeen = Math.max(maxDeltaSeen, delta.length());
			Vec3 end = from.add(delta);
			if (end.y() < 0.0) {
				return CollisionResult.hit(new Vec3(end.x(), 0.0, end.z()), Vec3.of(0, 1, 0));
			}
			boolean insideWall = end.x() >= wallMinX && end.x() <= wallMaxX;
			if (insideWall) {
				boolean fromLeft = from.x() < wallMinX;
				double clampedX = fromLeft ? wallMinX : wallMaxX;
				Vec3 normal = fromLeft ? Vec3.of(-1, 0, 0) : Vec3.of(1, 0, 0);
				return CollisionResult.hit(new Vec3(clampedX, end.y(), end.z()), normal);
			}
			return CollisionResult.moved(end);
		}

		@Override
		public SurfaceDefinition surfaceAt(Vec3 position) {
			return SurfaceDefinition.NORMAL;
		}
	}

	// ------------------------------------------------------------------
	// Config helpers
	// ------------------------------------------------------------------

	private static PhysicsConfig cfg(double gravity, double drag, double restitution,
			double bounceFloor, double stop, double maxStepDistance, int maxSubsteps) {
		return new PhysicsConfig(gravity, drag, restitution, bounceFloor, stop,
				4.0, maxStepDistance, maxSubsteps);
	}

	/** Coarse config: one sub-step per tick, exact hand-computed values. */
	private static PhysicsConfig coarse() {
		return cfg(0.06, 1.0, 0.60, 0.08, 0.03, 100.0, 16);
	}

	// ------------------------------------------------------------------
	// Rest state
	// ------------------------------------------------------------------

	@Test
	void restingBallIsUntouched() {
		BallState state = BallState.atRest(Vec3.of(1, 0, 2));
		assertSame(state, BallPhysics.step(state, coarse(), new EmptyWorld()));
	}

	// ------------------------------------------------------------------
	// Gravity and air drag
	// ------------------------------------------------------------------

	@Test
	void freeFallGainsGravityPerTick() {
		BallState state = new BallState(Vec3.of(0, 10, 0), Vec3.ZERO, false, false);
		BallState next = BallPhysics.step(state, coarse(), new EmptyWorld());
		assertEquals(-0.06, next.velocity().y(), TOL);
		assertEquals(10.0 - 0.06, next.position().y(), TOL);
	}

	@Test
	void airDragScalesVelocityWhileAirborne() {
		PhysicsConfig cfg = cfg(0.0, 0.5, 0.6, 0.08, 0.03, 100.0, 16);
		BallState state = BallState.launched(Vec3.of(0, 5, 0), Vec3.of(2, 0, 0));
		BallState next = BallPhysics.step(state, cfg, new EmptyWorld());
		assertEquals(1.0, next.velocity().x(), TOL);
		assertEquals(0.0, next.velocity().y(), TOL);
	}

	@Test
	void gravityTotalIsIndependentOfSubstepCount() {
		BallState start = new BallState(Vec3.of(0, 10, 0), Vec3.of(0, -2, 0), false, false);
		PhysicsConfig fine = cfg(0.06, 1.0, 0.6, 0.08, 0.03, 0.001, 16);
		BallState coarseEnd = BallPhysics.step(start, coarse(), new EmptyWorld());
		BallState fineEnd = BallPhysics.step(start, fine, new EmptyWorld());
		assertEquals(coarseEnd.velocity().y(), fineEnd.velocity().y(), TOL);
		// Semi-implicit Euler: positions converge but are not identical.
		assertEquals(coarseEnd.position().y(), fineEnd.position().y(), 0.05);
	}

	// ------------------------------------------------------------------
	// Launch clamping
	// ------------------------------------------------------------------

	@Test
	void clampLaunchPreservesDirectionAndCapsSpeed() {
		// (3,4,0) has speed 5; clamped to 4 it becomes (2.4, 3.2, 0).
		Vec3 clamped = BallPhysics.clampLaunch(Vec3.of(3, 4, 0), coarse());
		assertEquals(2.4, clamped.x(), TOL);
		assertEquals(3.2, clamped.y(), TOL);
		assertEquals(4.0, clamped.length(), TOL);
	}

	@Test
	void clampLaunchLeavesSlowLaunchesAlone() {
		Vec3 v = Vec3.of(1, 1, 1);
		assertEquals(v, BallPhysics.clampLaunch(v, coarse()));
	}

	// ------------------------------------------------------------------
	// Bounce and restitution
	// ------------------------------------------------------------------

	@Test
	void hardImpactBouncesWithRestitution() {
		// Impact speed 2.06 (gravity adds 0.06 before contact); restitution 0.6.
		BallState state = new BallState(Vec3.of(0, 0.05, 0), Vec3.of(0, -2, 0), false, false);
		BallState next = BallPhysics.step(state, coarse(), new FloorWorld(SurfaceDefinition.NORMAL));
		assertEquals(2.06 * 0.6, next.velocity().y(), TOL);
		assertEquals(0.0, next.position().y(), TOL);
		assertFalse(next.grounded());
		assertFalse(next.resting());
	}

	@Test
	void gentleImpactRollsInsteadOfBouncing() {
		// Impact speed 0.07 < bounceFloorSpeed 0.08 → transition to roll, then rest.
		BallState state = new BallState(Vec3.of(0, 0.01, 0), Vec3.of(0, -0.01, 0), false, false);
		BallState next = BallPhysics.step(state, coarse(), new FloorWorld(SurfaceDefinition.NORMAL));
		assertTrue(next.resting());
		assertEquals(0.0, next.position().y(), TOL);
	}

	@Test
	void wallImpactReflectsHorizontalComponent() {
		BallState state = BallState.launched(Vec3.of(4.9, 0.5, 0), Vec3.of(2, 0, 0));
		BallState next = BallPhysics.step(state, coarse(), new WallWorld(5.0, 7.0));
		assertEquals(5.0, next.position().x(), TOL);
		assertEquals(-2.0 * 0.6, next.velocity().x(), TOL);
		assertEquals(-0.06, next.velocity().y(), TOL);
		assertFalse(next.grounded());
	}

	// ------------------------------------------------------------------
	// Per-surface bounce modifiers
	// ------------------------------------------------------------------

	private static double bounceOutSpeed(SurfaceDefinition surface) {
		BallState state = new BallState(Vec3.of(0, 0.05, 0), Vec3.of(0, -2, 0), false, false);
		BallState next = BallPhysics.step(state, coarse(), new FloorWorld(surface));
		return next.velocity().y();
	}

	@Test
	void sandNearlyKillsTheBounce() {
		assertEquals(2.06 * 0.6 * 0.30, bounceOutSpeed(SurfaceDefinition.SAND), TOL);
	}

	@Test
	void slimeReturnsNearlyAllImpactEnergy() {
		double out = bounceOutSpeed(SurfaceDefinition.SLIME);
		assertEquals(2.06 * 0.6 * 1.60, out, TOL);
		assertTrue(out > 2.06 * 0.9, "slime should feel unusually lively: " + out);
	}

	@Test
	void honeyAlmostAbsorbsTheBounce() {
		double out = bounceOutSpeed(SurfaceDefinition.HONEY);
		assertEquals(2.06 * 0.6 * 0.05, out, TOL);
		assertTrue(out < 0.1);
	}

	@Test
	void surfaceBounceRankingMatchesDesign() {
		double slime = bounceOutSpeed(SurfaceDefinition.SLIME);
		double normal = bounceOutSpeed(SurfaceDefinition.NORMAL);
		double ice = bounceOutSpeed(SurfaceDefinition.ICE);
		double sand = bounceOutSpeed(SurfaceDefinition.SAND);
		double honey = bounceOutSpeed(SurfaceDefinition.HONEY);
		assertTrue(slime > normal);
		assertTrue(normal > ice);
		assertTrue(ice > sand);
		assertTrue(sand > honey);
	}

	// ------------------------------------------------------------------
	// Rolling friction and stopping
	// ------------------------------------------------------------------

	private static BallState rollingOn(SurfaceDefinition surface, double vx) {
		BallState state = new BallState(Vec3.of(0, 0, 0), Vec3.of(vx, 0, 0), true, false);
		return BallPhysics.step(state, coarse(), new FloorWorld(surface));
	}

	@Test
	void normalGroundFrictionSlowsRoll() {
		assertEquals(2.0 * 0.95, rollingOn(SurfaceDefinition.NORMAL, 2.0).velocity().x(), TOL);
	}

	@Test
	void loftedClubProfileAddsRollResistanceWithoutChangingSurfaceDefinitions() {
		BallState state = new BallState(Vec3.of(0, 0, 0), Vec3.of(2, 0, 0), true, false);
		BallState next = BallPhysics.step(state, coarse(),
			new FloorWorld(SurfaceDefinition.NORMAL), ShotPhysicsProfile.LOFTED_CLUB);

		assertEquals(2.0 * 0.95 * 0.82, next.velocity().x(), TOL);
		assertEquals(0.95, SurfaceDefinition.NORMAL.rollingFriction(), TOL);
	}

	@Test
	void loftedClubProfileRemovesHorizontalEnergyAtLanding() {
		BallState state = new BallState(Vec3.of(0, 0.05, 0), Vec3.of(2, -2, 0), false, false);
		BallState next = BallPhysics.step(state, coarse(),
			new FloorWorld(SurfaceDefinition.NORMAL), ShotPhysicsProfile.LOFTED_CLUB);

		assertEquals(2.0 * 0.25, next.velocity().x(), TOL);
		assertEquals(2.06 * 0.6, next.velocity().y(), TOL);
	}

	@Test
	void loftedSandLandingNearlyStopsHorizontalVelocity() {
		BallState state = new BallState(Vec3.of(0, 0.05, 0), Vec3.of(2, -2, 0), false, false);
		BallState next = BallPhysics.step(state, coarse(),
			new FloorWorld(SurfaceDefinition.SAND), ShotPhysicsProfile.LOFTED_CLUB);

		assertEquals(2.0 * 0.25 * 0.03, next.velocity().x(), TOL);
		assertEquals(2.06 * 0.6 * 0.30, next.velocity().y(), TOL);
	}

	@Test
	void iceRollsFartherThanNormalGround() {
		double ice = rollingOn(SurfaceDefinition.ICE, 2.0).velocity().x();
		double normal = rollingOn(SurfaceDefinition.NORMAL, 2.0).velocity().x();
		assertTrue(ice > normal, "ice must out-roll normal ground: " + ice + " vs " + normal);
		assertTrue(ice > 1.98, "ice should retain ~99.5% per tick: " + ice);
	}

	@Test
	void sandSlowsRollQuickly() {
		assertEquals(2.0 * 0.60, rollingOn(SurfaceDefinition.SAND, 2.0).velocity().x(), TOL);
	}

	@Test
	void puttRollingAcrossSandContinuesWithRollingFriction() {
		BallState next = rollingOn(SurfaceDefinition.SAND, 2.0);

		assertEquals(2.0 * 0.60, next.velocity().x(), TOL);
		assertFalse(next.resting());
	}

	@Test
	void honeySlowsRollFastest() {
		assertTrue(rollingOn(SurfaceDefinition.HONEY, 2.0).velocity().x()
				< rollingOn(SurfaceDefinition.SAND, 2.0).velocity().x());
	}

	@Test
	void slowGroundedBallComesToRest() {
		BallState next = rollingOn(SurfaceDefinition.NORMAL, 0.02);
		assertTrue(next.resting());
		assertEquals(Vec3.ZERO, next.velocity());
	}

	@Test
	void slowAirborneBallDoesNotRest() {
		BallState state = BallState.launched(Vec3.of(0, 5, 0), Vec3.of(0.02, 0.02, 0));
		BallState next = BallPhysics.step(state, coarse(), new EmptyWorld());
		assertFalse(next.resting());
	}

	@Test
	void groundedBallLeavingTheEdgeBecomesAirborne() {
		// Floor exists only for x <= 5; ball rolls past the edge this tick.
		BallState state = new BallState(Vec3.of(4.9, 0, 0), Vec3.of(1, 0, 0), true, false);
		BallState next = BallPhysics.step(state, coarse(), new FloorWorld(SurfaceDefinition.NORMAL, 5.0));
		assertFalse(next.grounded());
		assertTrue(next.position().x() > 5.0);
	}

	// ------------------------------------------------------------------
	// Sub-stepping / tunneling guard
	// ------------------------------------------------------------------

	@Test
	void substepCountScalesWithSpeedAndCaps() {
		assertEquals(1, BallPhysics.substepCount(Vec3.of(0.3, 0, 0), PhysicsConfig.DEFAULT));
		assertEquals(8, BallPhysics.substepCount(Vec3.of(4, 0, 0), PhysicsConfig.DEFAULT));
		assertEquals(16, BallPhysics.substepCount(Vec3.of(100, 0, 0), PhysicsConfig.DEFAULT));
	}

	@Test
	void fastBallDoesNotTunnelThroughThinWallWhenSubstepping() {
		// Wall is 0.4 thick; each sub-step moves at most 0.25 → the wall is hit.
		WallWorld world = new WallWorld(5.0, 5.4);
		PhysicsConfig fine = cfg(0.0, 1.0, 0.6, 0.08, 0.03, 0.25, 16);
		BallState state = BallState.launched(Vec3.of(4.9, 0.5, 0), Vec3.of(2, 0, 0));

		BallState next = BallPhysics.step(state, fine, world);

		assertTrue(next.position().x() < 5.0, "ball must not pass the wall: " + next);
		assertTrue(next.velocity().x() < 0, "ball must bounce back off the wall");
		assertTrue(world.maxDeltaSeen <= 0.25 + TOL,
				"no sweep may exceed maxStepDistance: " + world.maxDeltaSeen);
	}

	@Test
	void sameWallIsTunneledByACoarseSingleStep() {
		// Control for the guard: one huge step sails straight through the
		// naive endpoint-sampled wall. Documents why sub-stepping exists.
		WallWorld world = new WallWorld(5.0, 5.4);
		PhysicsConfig coarseCfg = cfg(0.0, 1.0, 0.6, 0.08, 0.03, 100.0, 16);
		BallState state = BallState.launched(Vec3.of(4.9, 0.5, 0), Vec3.of(2, 0, 0));

		BallState next = BallPhysics.step(state, coarseCfg, world);

		assertTrue(next.position().x() > 5.4, "control: coarse step should tunnel");
	}

	// ------------------------------------------------------------------
	// Safety and end-to-end
	// ------------------------------------------------------------------

	@Test
	void nanVelocityIsSanitizedInsteadOfPropagating() {
		BallState state = new BallState(Vec3.of(0, 5, 0), Vec3.of(Double.NaN, 1, 2), false, false);
		BallState next = BallPhysics.step(state, coarse(), new EmptyWorld());
		assertFalse(next.velocity().anyNaN());
		assertEquals(0.0, next.velocity().x(), TOL);
		assertEquals(0.94, next.velocity().y(), TOL);
	}

	@Test
	void droppedBallEventuallyRestsOnTheFloor() {
		BallState state = BallState.launched(Vec3.of(0, 10, 0), Vec3.of(0.5, 0, 0));
		PhysicsConfig cfg = PhysicsConfig.DEFAULT;
		FloorWorld world = new FloorWorld(SurfaceDefinition.NORMAL);
		for (int tick = 0; tick < 500 && !state.resting(); tick++) {
			state = BallPhysics.step(state, cfg, world);
		}
		assertTrue(state.resting(), "ball should settle within 500 ticks: " + state);
		assertEquals(0.0, state.position().y(), 0.5);
		assertTrue(state.grounded());
	}

	@Test
	void identicalInputsProduceIdenticalOutcomes() {
		BallState run1 = BallState.launched(Vec3.of(0, 10, 0), Vec3.of(0.5, 0, 0));
		BallState run2 = BallState.launched(Vec3.of(0, 10, 0), Vec3.of(0.5, 0, 0));
		PhysicsConfig cfg = PhysicsConfig.DEFAULT;
		for (int tick = 0; tick < 200; tick++) {
			run1 = BallPhysics.step(run1, cfg, new FloorWorld(SurfaceDefinition.NORMAL));
			run2 = BallPhysics.step(run2, cfg, new FloorWorld(SurfaceDefinition.NORMAL));
		}
		assertEquals(run1, run2);
	}
}
