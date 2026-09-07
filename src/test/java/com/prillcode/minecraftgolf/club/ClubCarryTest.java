package com.prillcode.minecraftgolf.club;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.ball.BallCollisionWorld;
import com.prillcode.minecraftgolf.ball.BallPhysics;
import com.prillcode.minecraftgolf.ball.BallState;
import com.prillcode.minecraftgolf.ball.CollisionResult;
import com.prillcode.minecraftgolf.ball.PhysicsConfig;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.surface.SurfaceDefinition;

/**
 * Headless "flat driving range" that pins club *feel* invariants in measured
 * numbers rather than vibes (ARCH §10/§12). Uses the same flat-floor + pure
 * {@link BallPhysics} abstraction M1 used; real terrain carry differs, but the
 * relative ordering and shot shape (does a wedge fly higher than a driver? is a
 * putt low and short?) are signal we want pinned so tuning cannot regress.)
 */
class ClubCarryTest {

	private static final PhysicsConfig CFG = PhysicsConfig.DEFAULT;

	/** Flat floor at y=0 over the whole plane, chosen surface. */
	static final class FlatNormal implements BallCollisionWorld {
		private final SurfaceDefinition surface;

		FlatNormal(SurfaceDefinition surface) {
			this.surface = surface;
		}

		@Override
		public CollisionResult move(Vec3 from, Vec3 delta) {
			Vec3 end = from.add(delta);
			if (end.y() < 0.0) {
				return CollisionResult.hit(new Vec3(end.x(), 0.0, end.z()), Vec3.of(0, 1, 0));
			}
			return CollisionResult.moved(end);
		}

		@Override
		public SurfaceDefinition surfaceAt(Vec3 position) {
			return surface;
		}
	}

	/** Result of one flat-range drive: initial velocity, horizontal carry, apex height, ticks stepped. */
	record Drive(Vec3 velocity, double carry, double peak, int ticks) {
	}

	private static Drive drive(ClubDefinition club) {
		Vec3 v = ShotResolver.initialVelocity(club, 0.0, 0.0, CFG.maxLaunchSpeed());
		if (v == null) {
			throw new IllegalStateException("level aim must be legal: " + club.id());
		}
		FlatNormal world = new FlatNormal(SurfaceDefinition.NORMAL);
		// Start just off the floor (ball radius ~0.25) so there is bit of drop-in arc.
		BallState state = new BallState(Vec3.of(0.0, 0.25, 0.0), v, false, false);
		double startY = state.position().y();
		double peak = startY;
		int ticks = 0;
		while (ticks < 2400) {
			BallState next = BallPhysics.step(state, CFG, world);
			if (next.equals(state)) {
				break; // no change (e.g. resting)
			}
			state = next;
			peak = Math.max(peak, next.position().y());
			ticks++;
			if (next.resting()) {
				break;
			}
		}
		return new Drive(v, Math.max(0.0, state.position().z()), peak - startY, ticks);
	}

	// Feel invariants that correspond to the manual-test feedback.

	@Test
	void driverCarriesFarthest() {
		double driver = drive(GolfClubs.DRIVER).carry();
		for (ClubDefinition c : GolfClubs.ALL) {
			if (c == GolfClubs.DRIVER) {
				continue;
			}
			assertTrue(drive(c).carry() <= driver + 1e-6,
					"club " + c.id() + " carry exceeds driver " + driver);
		}
	}

	@Test
	void putterIsGentleAndLow() {
		Drive putt = drive(GolfClubs.PUTTER);
		assertTrue(putt.peak() < 0.5, "putter should not fly: peak " + putt.peak());
		assertTrue(putt.carry() < drive(GolfClubs.SHORT_IRON).carry(),
				"putter carry " + putt.carry() + " should be less than the shortest iron");
		// Feedback: putter full-power felt too hard; it must stay clearly under the
		// wedge's energy so a right-click putt is not a rocket.
		assertTrue(putt.carry() * 1.6 < drive(GolfClubs.WEDGE).carry(),
				"putter carry " + putt.carry() + " too close to wedge energy " + drive(GolfClubs.WEDGE).carry());
	}

	@Test
	void wedgeLoftsHigherThanDriver() {
		double wedgePeak = drive(GolfClubs.WEDGE).peak();
		double driverPeak = drive(GolfClubs.DRIVER).peak();
		assertTrue(wedgePeak > driverPeak,
				"wedge peak " + wedgePeak + " should exceed driver peak " + driverPeak);
	}
}
