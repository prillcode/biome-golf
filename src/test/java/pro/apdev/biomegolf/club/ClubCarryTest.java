package pro.apdev.biomegolf.club;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.ball.BallCollisionWorld;
import pro.apdev.biomegolf.ball.BallPhysics;
import pro.apdev.biomegolf.ball.BallState;
import pro.apdev.biomegolf.ball.CollisionResult;
import pro.apdev.biomegolf.ball.PhysicsConfig;
import pro.apdev.biomegolf.ball.ShotPhysicsProfile;
import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.surface.SurfaceDefinition;

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
		private Double firstLanding;

		FlatNormal(SurfaceDefinition surface) {
			this.surface = surface;
		}

		@Override
		public CollisionResult move(Vec3 from, Vec3 delta) {
			Vec3 end = from.add(delta);
			if (end.y() < 0.0) {
				if (firstLanding == null) {
					firstLanding = end.z();
				}
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
	record Drive(Vec3 velocity, double carry, double totalDistance, double peak, int ticks) {
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
			ShotPhysicsProfile profile = club.putting()
				? ShotPhysicsProfile.STANDARD
				: ShotPhysicsProfile.LOFTED_CLUB;
			BallState next = BallPhysics.step(state, CFG, world, profile);
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
		double carry = world.firstLanding == null ? state.position().z() : world.firstLanding;
		return new Drive(v, Math.max(0.0, carry), Math.max(0.0, state.position().z()),
			peak - startY, ticks);
	}

	// Feel invariants that correspond to the manual-test feedback.

	@Test
	void catalogCarryMatchesFlatNormalRangeTargets() {
		for (ClubDefinition club : GolfClubs.ALL) {
			Drive shot = drive(club);
			double measured = club.putting() ? shot.totalDistance() : shot.carry();
			assertEquals(club.nominalCarry(), measured, 1.0,
					club.id() + " flat-range distance " + measured);
		}
	}

	@Test
	void raisedArcProgressionMatchesTuningTargets() {
		assertEquals(14.0, drive(GolfClubs.DRIVER).peak(), 0.3);
		assertEquals(16.0, drive(GolfClubs.FAIRWAY_WOOD).peak(), 0.3);
		assertEquals(18.0, drive(GolfClubs.LONG_IRON).peak(), 0.3);
		assertEquals(20.0, drive(GolfClubs.MID_IRON).peak(), 0.3);
		assertEquals(22.0, drive(GolfClubs.SHORT_IRON).peak(), 0.3);
		assertEquals(24.0, drive(GolfClubs.WEDGE).peak(), 0.3);
		assertTrue(Math.abs(drive(GolfClubs.WEDGE).peak()
				- drive(GolfClubs.SHORT_IRON).peak()) <= 2.5,
				"short iron and wedge should remain in the same high-flight family");
	}

	@Test
	void loftedClubsHaveSharplyLimitedRollout() {
		for (ClubDefinition club : GolfClubs.ALL) {
			if (club.putting()) {
				continue;
			}
			Drive shot = drive(club);
			double rollout = shot.totalDistance() - shot.carry();
			assertTrue(rollout <= club.nominalCarry() * 0.15,
				club.id() + " rollout should stay under 15% of carry: " + rollout);
		}
	}

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
		assertTrue(putt.totalDistance() < drive(GolfClubs.SHORT_IRON).carry(),
				"putter distance " + putt.totalDistance() + " should be less than the shortest iron");
		// Feedback: putter full-power felt too hard; it must stay clearly under the
		// wedge's energy so a right-click putt is not a rocket.
		assertTrue(putt.totalDistance() * 1.6 < drive(GolfClubs.WEDGE).carry(),
				"putter distance " + putt.totalDistance() + " too close to wedge energy " + drive(GolfClubs.WEDGE).carry());
	}

	@Test
	void wedgeLoftsHigherThanDriver() {
		double wedgePeak = drive(GolfClubs.WEDGE).peak();
		double driverPeak = drive(GolfClubs.DRIVER).peak();
		assertTrue(wedgePeak > driverPeak,
				"wedge peak " + wedgePeak + " should exceed driver peak " + driverPeak);
	}
}
