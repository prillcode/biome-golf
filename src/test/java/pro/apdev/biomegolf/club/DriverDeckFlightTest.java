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
 * M8.12/M8.14 outcome contract: a deck Driver flies lower and shorter than a tee shot,
 * while the Standard deck profile adds rollout without overtaking a Fairway Wood.
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
		ShotPhysicsProfile deck = LieRules.applyTo(teed, BallLie.DECK, ShotType.STANDARD);

		double teedRange = range(GolfClubs.DRIVER, teed);
		double deckRange = range(GolfClubs.DRIVER, deck);
		ShotPhysicsProfile launchOnly = teed.withLaunchScaled(
			LieRules.DECK_LAUNCH_HORIZONTAL, LieRules.DECK_LAUNCH_VERTICAL);
		double launchOnlyRange = range(GolfClubs.DRIVER, launchOnly);
		double fairwayWoodRange = range(GolfClubs.FAIRWAY_WOOD,
			ShotType.STANDARD.profile(GolfClubs.FAIRWAY_WOOD));

		assertTrue(deck.launchVerticalMultiplier() < teed.launchVerticalMultiplier(),
			"decked driver must launch lower");
		assertTrue(deckRange < teedRange,
			"decked driver must travel shorter: " + deckRange + " vs " + teedRange);
		assertTrue(deckRange > launchOnlyRange,
			"runner landing must add rollout: " + deckRange + " vs " + launchOnlyRange);
		assertTrue(deckRange < fairwayWoodRange,
			"deck Driver should remain shorter than a standard Fairway Wood: "
				+ deckRange + " vs " + fairwayWoodRange);

		double ratio = deckRange / teedRange;
		// Closes the par-5 exploit (a clear loss) while keeping the shot meaty.
		assertTrue(ratio >= 0.70 && ratio <= 0.80,
			"decked driver range ratio should be clearly penalized but viable: " + ratio);
	}

	@Test
	void deckedStingerIsLowerThanTeedStinger() {
		ShotPhysicsProfile teed = ShotType.STINGER.profile(GolfClubs.DRIVER);
		ShotPhysicsProfile deck = LieRules.applyTo(teed, BallLie.DECK, ShotType.STINGER);
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
			ShotPhysicsProfile afterTee = LieRules.applyTo(base, BallLie.TEE, ShotType.STANDARD);
			assertTrue(base.launchVerticalMultiplier() == afterTee.launchVerticalMultiplier());
		}
	}

	@Test
	void deckedDriverFansWiderForTheSameMiss() {
		ShotPhysicsProfile teed = ShotType.STANDARD.profile(GolfClubs.DRIVER);
		ShotPhysicsProfile deck = LieRules.applyTo(teed, BallLie.DECK, ShotType.STANDARD);

		Vec3 teedShot = ShotResolver.initialVelocity(GolfClubs.DRIVER, 0.0, 0.0, 1.0, 0.75,
			PhysicsConfig.DEFAULT.maxLaunchSpeed(), teed, LieRules.accuracySpread(BallLie.TEE));
		Vec3 deckShot = ShotResolver.initialVelocity(GolfClubs.DRIVER, 0.0, 0.0, 1.0, 0.75,
			PhysicsConfig.DEFAULT.maxLaunchSpeed(), deck, LieRules.accuracySpread(BallLie.DECK));

		assertTrue(Math.abs(deckShot.x()) > Math.abs(teedShot.x()),
			"decked driver must fan wider for the same accuracy miss");
	}
}
