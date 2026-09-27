package pro.apdev.biomegolf.hole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.golf.Vec3;

class HazardDropSearchTest {

	private static final Vec3 ENTRY = new Vec3(5.0, 64.0, 0.0);
	private static final Vec3 HEADING = new Vec3(1.0, 0.0, 0.0);
	private static final HoleBoundary BOUNDARY =
		new HoleBoundary(new Vec3(-10.0, 0.0, -10.0), new Vec3(20.0, 80.0, 10.0));

	@Test
	void dropsOnNearestLandBehindTheEntryPoint() {
		// Land exists only at x <= 2, i.e. behind the ball's +x heading.
		var drop = HazardDropSearch.findDrop(ENTRY, HEADING, BOUNDARY,
			(x, z) -> x <= 2.0 ? Optional.of(new Vec3(x, 64.0, z)) : Optional.empty());

		assertTrue(drop.isPresent());
		assertEquals(2.0, drop.orElseThrow().x(), 1.0E-9);
	}

	@Test
	void widensTheConeWhenTheDirectLineStaysWet() {
		// Land exists only to the side; the on-line column is always water.
		var drop = HazardDropSearch.findDrop(ENTRY, HEADING, BOUNDARY,
			(x, z) -> Math.abs(z) >= 1.0 ? Optional.of(new Vec3(x, 64.0, z)) : Optional.empty());

		assertTrue(drop.isPresent());
		assertTrue(Math.abs(drop.orElseThrow().z()) >= 1.0);
	}

	@Test
	void keepsWalkingBackOnTheLineInsteadOfSteppingSideways() {
		// A lateral site is always available, but the on-line land at x <= 2 must win,
		// so a bunker at the water edge keeps the drop on the shot line.
		var drop = HazardDropSearch.findDrop(ENTRY, HEADING, BOUNDARY,
			(x, z) -> z == 0.0
				? (x <= 2.0 ? Optional.of(new Vec3(x, 64.0, z)) : Optional.empty())
				: Optional.of(new Vec3(x, 64.0, z)));

		assertTrue(drop.isPresent());
		assertEquals(0.0, drop.orElseThrow().z(), 1.0E-9);
		assertEquals(2.0, drop.orElseThrow().x(), 1.0E-9);
	}

	@Test
	void reachesFarBackLandWhenTheCapAllows() {
		// Entry at x=100 heading +x (back is -x); land exists only at x <= 0.
		Vec3 entry = new Vec3(100.0, 64.0, 0.0);
		HoleBoundary wide = new HoleBoundary(new Vec3(-50.0, 0.0, -10.0), new Vec3(120.0, 80.0, 10.0));

		var drop = HazardDropSearch.findDrop(entry, new Vec3(1.0, 0.0, 0.0), wide, 105.0,
			(x, z) -> x <= 0.0 ? Optional.of(new Vec3(x, 64.0, z)) : Optional.empty());

		assertTrue(drop.isPresent());
		assertTrue(drop.orElseThrow().x() <= 1.0);
	}

	@Test
	void radialFallbackStaysNearTheEntryPoint() {
		// Land exists only far ahead (+x); the bounded radial sweep must not reach it.
		Vec3 entry = new Vec3(0.0, 64.0, 0.0);
		HoleBoundary wide = new HoleBoundary(new Vec3(-10.0, 0.0, -60.0), new Vec3(60.0, 80.0, 60.0));

		var drop = HazardDropSearch.findDrop(entry, new Vec3(1.0, 0.0, 0.0), wide, 100.0,
			(x, z) -> x >= 40.0 ? Optional.of(new Vec3(x, 64.0, z)) : Optional.empty());

		assertTrue(drop.isEmpty());
	}

	@Test
	void fallsBackToBoundedRadialSearchWhenTheLineNeverFindsLand() {
		// Land exists only in front of the ball (the far bank), never behind it.
		var drop = HazardDropSearch.findDrop(ENTRY, HEADING, BOUNDARY,
			(x, z) -> x > 5.0 ? Optional.of(new Vec3(x, 64.0, z)) : Optional.empty());

		assertTrue(drop.isPresent());
		assertTrue(drop.orElseThrow().x() > 5.0);
	}

	@Test
	void rejectsRestSitesOutsideTheHoleBoundary() {
		HoleBoundary tight = new HoleBoundary(new Vec3(0.0, 0.0, -1.0), new Vec3(4.0, 80.0, 1.0));
		var drop = HazardDropSearch.findDrop(ENTRY, HEADING, tight,
			(x, z) -> Optional.of(new Vec3(-5.0, 64.0, 0.0)));

		assertTrue(drop.isEmpty());
	}

	@Test
	void returnsEmptyWhenNoRestSiteExistsWithinTheBound() {
		assertTrue(HazardDropSearch.findDrop(ENTRY, HEADING, BOUNDARY, 3.0,
			(x, z) -> Optional.empty()).isEmpty());
	}

	@Test
	void returnsEmptyForAZeroHorizontalHeading() {
		assertTrue(HazardDropSearch.findDrop(ENTRY, new Vec3(0.0, -1.0, 0.0), BOUNDARY,
			(x, z) -> Optional.of(ENTRY)).isEmpty());
	}
}
