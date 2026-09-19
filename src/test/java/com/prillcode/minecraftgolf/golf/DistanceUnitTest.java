package com.prillcode.minecraftgolf.golf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DistanceUnitTest {

	@Test
	void yardsUseTheDisplayScaleAndWholeUnitRounding() {
		assertEquals(263, DistanceUnit.YARDS.rounded(150.0));
		assertEquals(219, DistanceUnit.YARDS.rounded(125.0));
		assertEquals(175, DistanceUnit.YARDS.rounded(100.0));
		assertEquals(149, DistanceUnit.YARDS.rounded(85.0));
		assertEquals(105, DistanceUnit.YARDS.rounded(60.0));
		assertEquals(74, DistanceUnit.YARDS.rounded(42.0));
		assertEquals(39, DistanceUnit.YARDS.rounded(22.0));
	}

	@Test
	void blocksRemainUnchangedAndLabelsAreExplicit() {
		assertEquals(50, DistanceUnit.BLOCKS.rounded(50.0));
		assertEquals("88 yards", DistanceUnit.YARDS.format(50.0));
		assertEquals("50 blocks", DistanceUnit.BLOCKS.format(50.0));
	}

	@Test
	void unitsToggleBetweenYardsAndBlocks() {
		assertEquals(DistanceUnit.BLOCKS, DistanceUnit.YARDS.toggled());
		assertEquals(DistanceUnit.YARDS, DistanceUnit.BLOCKS.toggled());
	}
}
