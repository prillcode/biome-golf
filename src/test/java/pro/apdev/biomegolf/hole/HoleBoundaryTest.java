package pro.apdev.biomegolf.hole;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.golf.Vec3;

class HoleBoundaryTest {

	@Test
	void containsPositionsOnAndInsideInclusiveBounds() {
		HoleBoundary boundary = new HoleBoundary(
			new Vec3(-10.0, 20.0, -30.0),
			new Vec3(40.0, 90.0, 50.0)
		);

		assertTrue(boundary.contains(new Vec3(-10.0, 20.0, -30.0)));
		assertTrue(boundary.contains(new Vec3(0.0, 64.0, 0.0)));
		assertTrue(boundary.contains(new Vec3(40.0, 90.0, 50.0)));
		assertFalse(boundary.contains(new Vec3(40.0001, 64.0, 0.0)));
		assertFalse(boundary.contains(new Vec3(0.0, 19.9999, 0.0)));
	}

	@Test
	void rejectsReversedAndNonFiniteBounds() {
		assertThrows(IllegalArgumentException.class, () -> new HoleBoundary(
			new Vec3(1.0, 0.0, 0.0),
			new Vec3(0.0, 1.0, 1.0)
		));
		assertThrows(IllegalArgumentException.class, () -> new HoleBoundary(
			new Vec3(0.0, Double.NaN, 0.0),
			new Vec3(1.0, 1.0, 1.0)
		));
		assertThrows(NullPointerException.class, () -> new HoleBoundary(null, Vec3.ZERO));
	}

	@Test
	void rejectsNonFiniteContainmentQueries() {
		HoleBoundary boundary = new HoleBoundary(Vec3.ZERO, new Vec3(10.0, 10.0, 10.0));
		assertThrows(IllegalArgumentException.class,
			() -> boundary.contains(new Vec3(Double.POSITIVE_INFINITY, 1.0, 1.0)));
	}
}
