package com.prillcode.minecraftgolf.ball;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;

class ShotDistanceTrackerTest {

	@Test
	void accumulatesHorizontalTravelAndIgnoresFlightHeight() {
		ShotDistanceTracker tracker = new ShotDistanceTracker();

		tracker.advance(new Vec3(0.0, 64.0, 0.0), new Vec3(3.0, 80.0, 4.0));
		tracker.advance(new Vec3(3.0, 80.0, 4.0), new Vec3(6.0, 64.0, 8.0));

		assertEquals(10.0, tracker.blocks(), 1e-9);
		assertEquals(10, tracker.roundedBlocks());
	}

	@Test
	void resetStartsTheNextShotAtZero() {
		ShotDistanceTracker tracker = new ShotDistanceTracker();
		tracker.advance(Vec3.ZERO, new Vec3(4.0, 0.0, 0.0));
		tracker.reset();

		assertEquals(0.0, tracker.blocks(), 1e-9);
		assertEquals(0, tracker.roundedBlocks());
	}
}
