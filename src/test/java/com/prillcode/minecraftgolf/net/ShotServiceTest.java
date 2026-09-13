package com.prillcode.minecraftgolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.club.GolfClubs;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.surface.SurfaceDefinition;

class ShotServiceTest {

	private static final double TOL = 1.0E-9;

	@Test
	void sandReducesEveryLaunchVelocityComponent() {
		Vec3 launch = Vec3.of(2.0, 1.0, -3.0);

		Vec3 reduced = ShotService.applySurfaceShotPower(launch, SurfaceDefinition.SAND);

		assertEquals(1.0, reduced.x(), TOL);
		assertEquals(0.5, reduced.y(), TOL);
		assertEquals(-1.5, reduced.z(), TOL);
	}

	@Test
	void normalSurfaceLeavesLaunchVelocityUnchanged() {
		Vec3 launch = Vec3.of(2.0, 1.0, -3.0);

		assertEquals(launch, ShotService.applySurfaceShotPower(launch, SurfaceDefinition.NORMAL));
	}

	@Test
	void driverIsIllegalOnlyOnSand() {
		assertTrue(ShotService.isDriver(GolfClubs.DRIVER));
		assertFalse(ShotService.isDriver(GolfClubs.PUTTER));
		assertTrue(ShotService.isSand(SurfaceDefinition.SAND));
		assertFalse(ShotService.isSand(SurfaceDefinition.NORMAL));
	}
}
