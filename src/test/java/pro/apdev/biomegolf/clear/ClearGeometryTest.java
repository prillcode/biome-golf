package pro.apdev.biomegolf.clear;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.clear.ClearGeometry.Axis;
import pro.apdev.biomegolf.clear.ClearGeometry.ClearBox;

class ClearGeometryTest {

	@Test
	void axisForCardinalYaws() {
		assertEquals(Axis.POS_Z, ClearGeometry.axisForYaw(0.0));
		assertEquals(Axis.NEG_X, ClearGeometry.axisForYaw(90.0));
		assertEquals(Axis.NEG_Z, ClearGeometry.axisForYaw(180.0));
		assertEquals(Axis.POS_X, ClearGeometry.axisForYaw(270.0));
	}

	@Test
	void axisWrapsAroundFullRotationsAndNegatives() {
		assertEquals(Axis.POS_Z, ClearGeometry.axisForYaw(360.0));
		assertEquals(Axis.NEG_X, ClearGeometry.axisForYaw(450.0));
		assertEquals(Axis.POS_X, ClearGeometry.axisForYaw(-90.0));
		assertEquals(Axis.POS_Z, ClearGeometry.axisForYaw(-360.0));
	}

	@Test
	void diagonalYawsRoundToNearestAxis() {
		// 45 is the +Z/-X boundary (resolves -X); 135 the -X/-Z boundary (resolves -Z).
		assertEquals(Axis.NEG_X, ClearGeometry.axisForYaw(45.0));
		assertEquals(Axis.NEG_Z, ClearGeometry.axisForYaw(135.0));
		assertEquals(Axis.POS_Z, ClearGeometry.axisForYaw(15.0));
		assertEquals(Axis.POS_X, ClearGeometry.axisForYaw(255.0));
	}

	@Test
	void oddWidthRoundsEvenUp() {
		assertEquals(9, ClearGeometry.oddWidth(8));
		assertEquals(7, ClearGeometry.oddWidth(7));
		assertEquals(1, ClearGeometry.oddWidth(1));
		assertEquals(21, ClearGeometry.oddWidth(20));
		assertEquals(1, ClearGeometry.oddWidth(0));
		assertEquals(1, ClearGeometry.oddWidth(-4));
	}

	@Test
	void boxClearsDepthAheadOnEveryAxis() {
		// Facing +X: one block ahead starts at x+1, width 9 spans z-4..z+4, height +32.
		ClearBox east = ClearGeometry.box(10, 64, 20, Axis.POS_X, 10, 8, 32);
		assertEquals(11, east.minX());
		assertEquals(20, east.maxX());
		assertEquals(16, east.minZ());
		assertEquals(24, east.maxZ());
		assertEquals(64, east.minY());
		assertEquals(95, east.maxY());

		ClearBox west = ClearGeometry.box(10, 64, 20, Axis.NEG_X, 10, 9, 32);
		assertEquals(0, west.minX());
		assertEquals(9, west.maxX());

		ClearBox south = ClearGeometry.box(10, 64, 20, Axis.POS_Z, 10, 9, 32);
		assertEquals(21, south.minZ());
		assertEquals(30, south.maxZ());
		assertEquals(6, south.minX());
		assertEquals(14, south.maxX());

		ClearBox north = ClearGeometry.box(10, 64, 20, Axis.NEG_Z, 10, 9, 32);
		assertEquals(10, north.minZ());
		assertEquals(19, north.maxZ());
	}

	@Test
	void positiveHeightClearsUpwardFromFeetLevel() {
		ClearBox box = ClearGeometry.box(0, 70, 0, Axis.POS_X, 5, 1, 10);
		assertEquals(70, box.minY());
		assertEquals(79, box.maxY());
	}

	@Test
	void negativeHeightClearsDownwardIncludingFeetLevel() {
		ClearBox box = ClearGeometry.box(0, 70, 0, Axis.POS_X, 5, 1, -10);
		assertEquals(60, box.minY());
		assertEquals(70, box.maxY());
		// The foot-level block (70) is included so digging leaves no rim, and the
		// column is 11 blocks 60..70 (depth still starts ahead of the player).
	}

	@Test
	void volumeCountsInclusiveBlocks() {
		ClearBox box = ClearGeometry.box(0, 64, 0, Axis.POS_X, 10, 9, 32);
		assertEquals(10 * 32 * 9, box.volume());
	}
}