package pro.apdev.biomegolf.hole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.golf.Vec3;

class HazardFluidCrossingTest {

	@Test
	void findsTheFluidMarginAlongTheSegment() {
		// Fluid occupies x >= 5; the ball travels +x through it at y=64.
		var contact = HazardFluidCrossing.firstContact(
			new Vec3(0.0, 64.0, 0.0), new Vec3(10.0, 64.0, 0.0), 0.25,
			(x, y, z) -> x >= 5.0);

		assertTrue(contact.isPresent());
		assertEquals(5.0, contact.orElseThrow().x(), 0.05);
	}

	@Test
	void detectsContactViaTheUndersideOfTheBall() {
		// Only the block below the ball center is fluid (a shallow water film).
		var contact = HazardFluidCrossing.firstContact(
			new Vec3(0.0, 64.0, 0.0), new Vec3(4.0, 64.0, 0.0), 0.25,
			(x, y, z) -> x >= 2.0 && y < 64.0);

		assertTrue(contact.isPresent());
		assertEquals(2.0, contact.orElseThrow().x(), 0.05);
	}

	@Test
	void returnsEmptyWhenTheWholeSegmentIsDry() {
		assertTrue(HazardFluidCrossing.firstContact(
			new Vec3(0.0, 64.0, 0.0), new Vec3(4.0, 64.0, 0.0), 0.25,
			(x, y, z) -> false).isEmpty());
	}
}
