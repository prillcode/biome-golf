package com.prillcode.minecraftgolf.course;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;

class ProtectedZoneTest {

	@Test
	void containsPositionsWithinRadiusAndVerticalExtent() {
		ProtectedZone zone = new ProtectedZone("cup", 1, new Vec3(0.0, 64.0, 0.0), 5.0, 3.0);

		// Center, edge, and a point inside the 3D cylinder.
		assertTrue(zone.contains(0, 64, 0));
		assertTrue(zone.contains(5, 64, 0));
		assertTrue(zone.contains(3, 67, 4));
		// Just outside the horizontal radius or the vertical extent.
		assertFalse(zone.contains(5, 64, 1));
		assertFalse(zone.contains(0, 68, 1));
		assertFalse(zone.contains(0, 60, 0));
	}

	@Test
	void containsPrecisePositions() {
		ProtectedZone zone = new ProtectedZone("tee", 3, new Vec3(-365.5, 70.25, 490.5), 12.0, 8.0);

		assertTrue(zone.contains(new Vec3(-365.5, 70.25, 490.5)));
		assertTrue(zone.contains(new Vec3(-363.0, 69.0, 486.0)));
		assertFalse(zone.contains(new Vec3(-350.0, 69.0, 486.0)));
	}

	@Test
	void rejectsInvalidZones() {
		assertThrows(IllegalArgumentException.class,
			() -> new ProtectedZone("cup", 1, Vec3.ZERO, 0.0, 3.0));
		assertThrows(IllegalArgumentException.class,
			() -> new ProtectedZone("cup", 1, Vec3.ZERO, 5.0, 0.0));
		assertThrows(IllegalArgumentException.class,
			() -> new ProtectedZone("cup", 1, Vec3.ZERO, Double.NaN, 3.0));
		assertThrows(IllegalArgumentException.class,
			() -> new ProtectedZone("cup", 1, new Vec3(Double.POSITIVE_INFINITY, 0.0, 0.0), 5.0, 3.0));
		assertThrows(IllegalArgumentException.class,
			() -> new ProtectedZone("cup", 0, Vec3.ZERO, 5.0, 3.0));
		assertThrows(IllegalArgumentException.class,
			() -> new ProtectedZone("", 1, Vec3.ZERO, 5.0, 3.0));
		assertThrows(NullPointerException.class,
			() -> new ProtectedZone("cup", 1, null, 5.0, 3.0));
	}

	@Test
	void rejectsNonFiniteContainmentQueries() {
		ProtectedZone zone = new ProtectedZone("cup", 1, Vec3.ZERO, 5.0, 3.0);
		assertThrows(IllegalArgumentException.class,
			() -> zone.contains(new Vec3(1.0, Double.NaN, 0.0)));
	}
}