package pro.apdev.biomegolf.ball;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PhysicsConfig tunable constants")
class PhysicsConfigTest {

	private static final double TOL = 1.0E-9;

	@Test
	void defaultConfigMatchesMilestoneSpec() {
		PhysicsConfig cfg = PhysicsConfig.DEFAULT;
		assertEquals(0.06, cfg.gravity(), TOL);
		assertEquals(0.99, cfg.airDrag(), TOL);
		assertEquals(0.60, cfg.restitution(), TOL);
		assertEquals(0.08, cfg.bounceFloorSpeed(), TOL);
		assertEquals(0.03, cfg.stopSpeed(), TOL);
		assertEquals(5.0, cfg.maxLaunchSpeed(), TOL);
		assertEquals(0.5, cfg.maxStepDistance(), TOL);
		assertEquals(16, cfg.maxSubsteps());
	}

	@Test
	void fieldsAreOrderedAsDocumented() {
		PhysicsConfig cfg = new PhysicsConfig(1.0, 0.9, 0.8, 0.7, 0.6, 5.0, 0.5, 8);
		assertEquals(1.0, cfg.gravity(), TOL);
		assertEquals(0.9, cfg.airDrag(), TOL);
		assertEquals(0.8, cfg.restitution(), TOL);
		assertEquals(0.7, cfg.bounceFloorSpeed(), TOL);
		assertEquals(0.6, cfg.stopSpeed(), TOL);
		assertEquals(5.0, cfg.maxLaunchSpeed(), TOL);
		assertEquals(0.5, cfg.maxStepDistance(), TOL);
		assertEquals(8, cfg.maxSubsteps());
	}

	@Test
	void gravityMustBeNonNegative() {
		assertThrows(IllegalArgumentException.class,
				() -> new PhysicsConfig(-0.01, 1.0, 0.5, 0.1, 0.1, 1.0, 0.5, 1));
	}

	@Test
	void airDragMustBeWithinRange() {
		assertThrows(IllegalArgumentException.class,
				() -> new PhysicsConfig(0.0, 0.0, 0.5, 0.1, 0.1, 1.0, 0.5, 1));
		assertThrows(IllegalArgumentException.class,
				() -> new PhysicsConfig(0.0, 1.1, 0.5, 0.1, 0.1, 1.0, 0.5, 1));
	}

	@Test
	void restitutionMustBeWithinRange() {
		assertThrows(IllegalArgumentException.class,
				() -> new PhysicsConfig(0.0, 1.0, -0.1, 0.1, 0.1, 1.0, 0.5, 1));
		assertThrows(IllegalArgumentException.class,
				() -> new PhysicsConfig(0.0, 1.0, 1.1, 0.1, 0.1, 1.0, 0.5, 1));
	}

	@Test
	void speedThresholdsMustBeNonNegative() {
		assertThrows(IllegalArgumentException.class,
				() -> new PhysicsConfig(0.0, 1.0, 0.5, -0.1, 0.1, 1.0, 0.5, 1));
		assertThrows(IllegalArgumentException.class,
				() -> new PhysicsConfig(0.0, 1.0, 0.5, 0.1, -0.1, 1.0, 0.5, 1));
	}

	@Test
	void launchAndStepLimitsMustBePositive() {
		assertThrows(IllegalArgumentException.class,
				() -> new PhysicsConfig(0.0, 1.0, 0.5, 0.1, 0.1, 0.0, 0.5, 1));
		assertThrows(IllegalArgumentException.class,
				() -> new PhysicsConfig(0.0, 1.0, 0.5, 0.1, 0.1, 1.0, 0.0, 1));
	}

	@Test
	void maxSubstepsMustBeAtLeastOne() {
		assertThrows(IllegalArgumentException.class,
				() -> new PhysicsConfig(0.0, 1.0, 0.5, 0.1, 0.1, 1.0, 0.5, 0));
	}

	@Test
	void defaultConfigSatisfiesSubstepInvariants() {
		assertTrue(PhysicsConfig.DEFAULT.maxSubsteps() >= 1);
		assertTrue(PhysicsConfig.DEFAULT.maxStepDistance() > 0.0);
		assertTrue(PhysicsConfig.DEFAULT.maxLaunchSpeed() > 0.0);
	}
}
