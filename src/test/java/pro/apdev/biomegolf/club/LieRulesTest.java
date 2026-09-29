package pro.apdev.biomegolf.club;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.ball.ShotPhysicsProfile;
import pro.apdev.biomegolf.golf.Vec3;

/** M8.12: pure lie classification and Driver deck-penalty composition. */
class LieRulesTest {

	private static final Vec3 TEE = new Vec3(0.0, 64.0, 0.0);

	@Test
	void ballOnTeeAnchorIsTeeLie() {
		assertEquals(BallLie.TEE, LieRules.classify(TEE, TEE));
		assertEquals(BallLie.TEE, LieRules.classify(
			new Vec3(LieRules.TEE_LIE_RADIUS, 64.0, 0.0), TEE));
	}

	@Test
	void ballAwayFromEveryAnchorIsDeckLie() {
		assertEquals(BallLie.DECK, LieRules.classify(new Vec3(50.0, 64.0, 50.0), TEE));
		// Outside the vertical tolerance is also a deck lie.
		assertEquals(BallLie.DECK, LieRules.classify(
			new Vec3(0.0, 64.0 + LieRules.TEE_LIE_VERTICAL_TOLERANCE + 1.0, 0.0), TEE));
	}

	@Test
	void unknownTeeReferenceNeverPenalizes() {
		assertEquals(BallLie.TEE, LieRules.classify(new Vec3(50.0, 64.0, 50.0), (Vec3) null));
		assertEquals(BallLie.TEE, LieRules.classify(new Vec3(50.0, 64.0, 50.0), List.of()));
	}

	@Test
	void anyMatchingAnchorAmongManyIsTee() {
		List<Vec3> anchors = List.of(new Vec3(100.0, 64.0, 100.0), TEE);
		assertEquals(BallLie.TEE, LieRules.classify(new Vec3(1.0, 64.0, 1.0), anchors));
	}

	@Test
	void onlyTheDriverIsPenalized() {
		assertTrue(LieRules.penalizes(GolfClubs.DRIVER, BallLie.DECK));
		assertFalse(LieRules.penalizes(GolfClubs.DRIVER, BallLie.TEE));
		assertFalse(LieRules.penalizes(GolfClubs.FAIRWAY_WOOD, BallLie.DECK));
		assertFalse(LieRules.penalizes(GolfClubs.LONG_IRON, BallLie.DECK));
		assertFalse(LieRules.penalizes(null, BallLie.DECK));
	}

	@Test
	void deckCompositionLowersLaunchAndKeepsLandingBehavior() {
		ShotPhysicsProfile base = ShotType.STANDARD.profile(GolfClubs.DRIVER);
		ShotPhysicsProfile deck = LieRules.applyTo(base, BallLie.DECK);

		assertEquals(base.launchHorizontalMultiplier() * LieRules.DECK_LAUNCH_HORIZONTAL,
			deck.launchHorizontalMultiplier(), 1e-9);
		assertEquals(base.launchVerticalMultiplier() * LieRules.DECK_LAUNCH_VERTICAL,
			deck.launchVerticalMultiplier(), 1e-9);
		assertEquals(base.landingHorizontalRetention(), deck.landingHorizontalRetention(), 1e-9);
		assertEquals(base.rollingFrictionMultiplier(), deck.rollingFrictionMultiplier(), 1e-9);
	}

	@Test
	void teeLieLeavesProfileUnchanged() {
		ShotPhysicsProfile base = ShotType.STANDARD.profile(GolfClubs.DRIVER);
		assertEquals(base, LieRules.applyTo(base, BallLie.TEE));
	}

	@Test
	void deckWidensAccuracySpreadAndShrinksDisplayedCarry() {
		assertEquals(LieRules.DECK_ACCURACY_SPREAD, LieRules.accuracySpread(BallLie.DECK), 1e-9);
		assertEquals(1.0, LieRules.accuracySpread(BallLie.TEE), 1e-9);
		assertEquals(LieRules.DECK_DISPLAY_CARRY_FACTOR,
			LieRules.displayCarryFactor(GolfClubs.DRIVER, BallLie.DECK), 1e-9);
		assertEquals(1.0, LieRules.displayCarryFactor(GolfClubs.DRIVER, BallLie.TEE), 1e-9);
		assertEquals(1.0, LieRules.displayCarryFactor(GolfClubs.WEDGE, BallLie.DECK), 1e-9);
	}
}
