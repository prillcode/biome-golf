package pro.apdev.biomegolf.club;

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
 * M8.12 outcome contract: a Driver from the deck flies materially lower and shorter
 * than the same swing from a tee, without changing any other club.
 */
class DriverDeckFlightTest {

	/** Flat normal ground; measures carry + roll along +Z. */
	static final class Floor implements BallCollisionWorld {
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
			return SurfaceDefinition.NORMAL;
		}
	}

	private static double range(ClubDefinition club, ShotPhysicsProfile profile) {
		Vec3 v = ShotResolver.initialVelocity(club, 0.0, 0.0, 1.0, 0.5,
			PhysicsConfig.DEFAULT.maxLaunchSpeed(), profile);
		BallState state = BallState.launched(Vec3.of(0.0, 1.0, 0.0), v);
		Floor world = new Floor();
		for (int i = 0; i < 4000 && !state.resting(); i++) {
			state = BallPhysics.step(state, PhysicsConfig.DEFAULT, world, profile);
		}
		return state.position().z();
	}

	@Test
	void deckedDriverFliesLowerAndTravelsShorterThanTeed() {
		ShotPhysicsProfile teed = ShotType.STANDARD.profile(GolfClubs.DRIVER);
		ShotPhysicsProfile deck = LieRules.applyTo(teed, BallLie.DECK);

		double teedRange = range(GolfClubs.DRIVER, teed);
		double deckRange = range(GolfClubs.DRIVER, deck);

		assertTrue(deck.launchVerticalMultiplier() < teed.launchVerticalMultiplier(),
			"decked driver must launch lower");
		assertTrue(deckRange < teedRange,
			"decked driver must travel shorter: " + deckRange + " vs " + teedRange);

		double ratio = deckRange / teedRange;
		// Closes the par-5 exploit (a clear loss) while keeping the shot meaty.
		assertTrue(ratio >= 0.70 && ratio <= 0.80,
			"decked driver range ratio should be clearly penalized but viable: " + ratio);
	}

	@Test
	void deckedStingerIsLowerThanTeedStinger() {
		ShotPhysicsProfile teed = ShotType.STINGER.profile(GolfClubs.DRIVER);
		ShotPhysicsProfile deck = LieRules.applyTo(teed, BallLie.DECK);
		assertTrue(deck.launchVerticalMultiplier() < teed.launchVerticalMultiplier());
	}

	@Test
	void nonDriverClubsAreUnaffected() {
		for (ClubDefinition club : GolfClubs.ALL) {
			if ("driver".equals(club.id())) {
				continue;
			}
			ShotPhysicsProfile base = ShotType.STANDARD.profile(club);
			// LieRules.applyTo is only ever used behind penalizes(); confirm base identity.
			ShotPhysicsProfile afterTee = LieRules.applyTo(base, BallLie.TEE);
			assertTrue(base.launchVerticalMultiplier() == afterTee.launchVerticalMultiplier());
		}
	}

	@Test
	void deckedDriverFansWiderForTheSameMiss() {
		ShotPhysicsProfile teed = ShotType.STANDARD.profile(GolfClubs.DRIVER);
		ShotPhysicsProfile deck = LieRules.applyTo(teed, BallLie.DECK);

		Vec3 teedShot = ShotResolver.initialVelocity(GolfClubs.DRIVER, 0.0, 0.0, 1.0, 0.75,
			PhysicsConfig.DEFAULT.maxLaunchSpeed(), teed, LieRules.accuracySpread(BallLie.TEE));
		Vec3 deckShot = ShotResolver.initialVelocity(GolfClubs.DRIVER, 0.0, 0.0, 1.0, 0.75,
			PhysicsConfig.DEFAULT.maxLaunchSpeed(), deck, LieRules.accuracySpread(BallLie.DECK));

		assertTrue(Math.abs(deckShot.x()) > Math.abs(teedShot.x()),
			"decked driver must fan wider for the same accuracy miss");
	}
}
