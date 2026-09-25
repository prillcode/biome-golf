package pro.apdev.biomegolf.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.hole.HoleBoundary;

class AuthoredHoleBoundsTest {

	@Test
	void normalizesCornerOrderingPerAxis() {
		HoleBoundary boundary = AuthoredHoleBounds.fromCorners(
			new Vec3(100, 70, -50), new Vec3(20, 64, 30), -64, 320);
		assertEquals(new Vec3(20, -64, -50), boundary.min());
		assertEquals(new Vec3(100, 320, 30), boundary.max());
	}

	@Test
	void expandsYToWorldBuildHeightRegardlessOfCornerY() {
		HoleBoundary boundary = AuthoredHoleBounds.fromCorners(
			new Vec3(0, 200, 0), new Vec3(10, -30, 10), -64, 320);
		assertEquals(-64, boundary.min().y());
		assertEquals(320, boundary.max().y());
	}

	@Test
	void identicalCornersProduceDegenerateBox() {
		HoleBoundary boundary = AuthoredHoleBounds.fromCorners(
			new Vec3(5, 64, 5), new Vec3(5, 64, 5), -64, 320);
		assertEquals(new Vec3(5, -64, 5), boundary.min());
		assertEquals(new Vec3(5, 320, 5), boundary.max());
	}

	@Test
	void rejectsInvertedWorldHeightRange() {
		assertThrows(IllegalArgumentException.class, () ->
			AuthoredHoleBounds.fromCorners(new Vec3(0, 0, 0), new Vec3(1, 1, 1), 320, -64));
	}
}
