package com.prillcode.minecraftgolf.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TravelDestinationSearchTest {

	@Test
	void choosesTheFirstSafeNearbyPositionWithoutUsingTheBallBlock() {
		TravelDestinationSearch.Destination expected =
			new TravelDestinationSearch.Destination(12, 63, 20);

		var result = TravelDestinationSearch.find(10, 62, 20, expected::equals);

		assertEquals(expected, result.orElseThrow());
	}

	@Test
	void returnsEmptyWhenNoNearbyPositionIsSafe() {
		assertTrue(TravelDestinationSearch.find(10, 62, 20, candidate -> false).isEmpty());
	}
}
