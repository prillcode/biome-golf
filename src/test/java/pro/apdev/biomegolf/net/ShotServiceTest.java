package pro.apdev.biomegolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.club.GolfClubs;
import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.surface.SurfaceDefinition;

class ShotServiceTest {

	private static final double TOL = 1.0E-9;

	@Test
	void sandReducesEveryLaunchVelocityComponent() {
		Vec3 launch = Vec3.of(2.0, 1.0, -3.0);

		Vec3 reduced = ShotService.applySurfaceShotPower(launch, SurfaceDefinition.SAND);

		assertEquals(1.5, reduced.x(), TOL);
		assertEquals(0.75, reduced.y(), TOL);
		assertEquals(-2.25, reduced.z(), TOL);
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
