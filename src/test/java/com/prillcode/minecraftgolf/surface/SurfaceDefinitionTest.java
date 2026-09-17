package com.prillcode.minecraftgolf.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SurfaceDefinition surface coefficients")
class SurfaceDefinitionTest {

	private static final double TOL = 1.0E-9;

	@Test
	void normalSurfaceIsBaseline() {
		assertEquals(0.95, SurfaceDefinition.NORMAL.rollingFriction(), TOL);
		assertEquals(1.0, SurfaceDefinition.NORMAL.bounceMultiplier(), TOL);
		assertEquals(1.0, SurfaceDefinition.NORMAL.landingHorizontalRetention(), TOL);
		assertEquals(1.0, SurfaceDefinition.NORMAL.shotPowerMultiplier(), TOL);
		assertEquals("normal", SurfaceDefinition.NORMAL.id());
	}

	@Test
	void iceRollsAndBouncesHigh() {
		assertTrue(SurfaceDefinition.ICE.rollingFriction() > SurfaceDefinition.NORMAL.rollingFriction());
		assertEquals(0.995, SurfaceDefinition.ICE.rollingFriction(), TOL);
		assertEquals(0.95, SurfaceDefinition.ICE.bounceMultiplier(), TOL);
	}

	@Test
	void sandAndHoneyAreHazardsWithLowRollBounce() {
		assertEquals(0.60, SurfaceDefinition.SAND.rollingFriction(), TOL);
		assertEquals(0.30, SurfaceDefinition.SAND.bounceMultiplier(), TOL);
		assertEquals(0.03, SurfaceDefinition.SAND.landingHorizontalRetention(), TOL);
		assertEquals(0.75, SurfaceDefinition.SAND.shotPowerMultiplier(), TOL);
		assertTrue(SurfaceDefinition.SAND.hazard());

		assertEquals(0.55, SurfaceDefinition.HONEY.rollingFriction(), TOL);
		assertEquals(0.05, SurfaceDefinition.HONEY.bounceMultiplier(), TOL);
		assertTrue(SurfaceDefinition.HONEY.hazard());
	}

	@Test
	void ordinarySurfacesAreNotHazards() {
		assertFalse(SurfaceDefinition.NORMAL.hazard());
		assertFalse(SurfaceDefinition.ICE.hazard());
		assertFalse(SurfaceDefinition.SLIME.hazard());
		assertFalse(SurfaceDefinition.GENERIC.hazard());
	}

	@Test
	void slimeBouncesUnusuallyHigh() {
		assertTrue(SurfaceDefinition.SLIME.bounceMultiplier() > 1.0);
		assertEquals(1.60, SurfaceDefinition.SLIME.bounceMultiplier(), TOL);
	}

	@Test
	void surfaceBounceRankingMatchesPhysicsDesign() {
		double slime = SurfaceDefinition.SLIME.bounceMultiplier();
		double normal = SurfaceDefinition.NORMAL.bounceMultiplier();
		double ice = SurfaceDefinition.ICE.bounceMultiplier();
		double generic = SurfaceDefinition.GENERIC.bounceMultiplier();
		double sand = SurfaceDefinition.SAND.bounceMultiplier();
		double honey = SurfaceDefinition.HONEY.bounceMultiplier();

		assertTrue(slime > normal);
		assertTrue(normal > ice);
		assertTrue(ice > generic);
		assertTrue(generic > sand);
		assertTrue(sand > honey);
	}

	@Test
	void rollingFrictionMustBeInZeroOneRange() {
		assertThrows(IllegalArgumentException.class,
				() -> new SurfaceDefinition("bad", -0.1, 1.0, 1.0, 1.0, false));
		assertThrows(IllegalArgumentException.class,
				() -> new SurfaceDefinition("bad", 1.1, 1.0, 1.0, 1.0, false));
	}

	@Test
	void bounceMultiplierMustBeNonNegative() {
		assertThrows(IllegalArgumentException.class,
				() -> new SurfaceDefinition("bad", 0.5, -0.1, 1.0, 1.0, false));
	}

	@Test
	void landingRetentionAndShotPowerMustBeNonNegativeAndAtMostOne() {
		assertThrows(IllegalArgumentException.class,
				() -> new SurfaceDefinition("bad", 0.5, 1.0, -0.1, 1.0, false));
		assertThrows(IllegalArgumentException.class,
				() -> new SurfaceDefinition("bad", 0.5, 1.0, 1.1, 1.0, false));
		assertThrows(IllegalArgumentException.class,
				() -> new SurfaceDefinition("bad", 0.5, 1.0, 1.0, -0.1, false));
		assertThrows(IllegalArgumentException.class,
				() -> new SurfaceDefinition("bad", 0.5, 1.0, 1.0, 1.1, false));
	}

	@Test
	void idMustNotBeNull() {
		assertThrows(NullPointerException.class,
				() -> new SurfaceDefinition(null, 0.5, 1.0, 1.0, 1.0, false));
	}
}
