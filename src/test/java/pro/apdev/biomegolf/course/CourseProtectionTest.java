package pro.apdev.biomegolf.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.hole.HoleBoundary;
import pro.apdev.biomegolf.hole.HoleDefinition;

class CourseProtectionTest {

	@Test
	void buildsTeeAndCupZonesForEveryHole() {
		List<ProtectedZone> zones =
			CourseProtection.zonesFor(fixtureCourse(), CourseProtectionConfig.DEFAULT);

		assertEquals(6, zones.size());
		for (int holeNumber = 1; holeNumber <= 3; holeNumber++) {
			int hole = holeNumber;
			assertEquals(1, zones.stream().filter(z -> z.holeNumber() == hole && z.kind().equals("tee")).count());
			assertEquals(1, zones.stream().filter(z -> z.holeNumber() == hole && z.kind().equals("cup")).count());
		}
	}

	@Test
	void protectsEveryTeeCupAndGreenUnderCupVicinity() {
		CourseDefinition course = fixtureCourse();
		List<ProtectedZone> zones =
			CourseProtection.zonesFor(course, CourseProtectionConfig.DEFAULT);

		// Every authored tee and cup block position.
		for (int hole = 1; hole <= 3; hole++) {
			assertTrue(CourseProtection.isProtected(zones, course.hole(hole).tee()));
			assertTrue(CourseProtection.isProtected(zones, course.hole(hole).cup()));
		}
		// Greens are protected via cup vicinity (recorded S1 decision): a block near
		// the far side of Hole 1's cup and a tee-box pad block near its tee.
		assertTrue(CourseProtection.isProtected(zones, 34, 64, 0));
		assertTrue(CourseProtection.isProtected(zones, 7, 64, 2));
	}

	@Test
	void leavesOrdinaryTerrainBreakable() {
		List<ProtectedZone> zones =
			CourseProtection.zonesFor(fixtureCourse(), CourseProtectionConfig.DEFAULT);

		// Ordinary terrain far from a course, and ground between a hole's tee and
		// cup that sits outside the zone cylinders, are outside every zone.
		assertFalse(CourseProtection.isProtected(zones, -400, 64, -400));
		assertFalse(CourseProtection.isProtected(zones, 300, 63, 300));
		assertFalse(CourseProtection.isProtected(zones, 14, 64, 14));
	}

	@Test
	void singleHoleZonesProtectOnlyThatHole() {
		List<ProtectedZone> zones =
			CourseProtection.zonesFor(fixtureCourse().hole(2), CourseProtectionConfig.DEFAULT);

		assertEquals(2, zones.size());
		assertTrue(CourseProtection.isProtected(zones, 75, 64, 0));
		assertTrue(CourseProtection.isProtected(zones, 55, 64, 0));
		assertFalse(CourseProtection.isProtected(zones, 25, 64, 0));
	}

	@Test
	void rejectsInvalidConfigValues() {
		assertThrows(IllegalArgumentException.class, () -> new CourseProtectionConfig(0.0, 8.0, 4.0));
		assertThrows(IllegalArgumentException.class, () -> new CourseProtectionConfig(12.0, 0.0, 4.0));
		assertThrows(IllegalArgumentException.class, () -> new CourseProtectionConfig(Double.NaN, 8.0, 4.0));
		// M8.10 S3 blast margin must be finite and non-negative (0 is allowed).
		assertThrows(IllegalArgumentException.class, () -> new CourseProtectionConfig(12.0, 8.0, -1.0));
		assertThrows(IllegalArgumentException.class, () -> new CourseProtectionConfig(12.0, 8.0, Double.NaN));
		assertEquals(0.0, new CourseProtectionConfig(12.0, 8.0, 0.0).tntBlastSafetyMargin());
	}

	@Test
	void breakPolicyProtectsZonesAndCupButExemptsDevOperators() {
		List<ProtectedZone> zones = List.of(
			new ProtectedZone("tee", 1, new pro.apdev.biomegolf.golf.Vec3(0, 64, 0), 5, 3));

		assertFalse(CourseProtection.mayBreak(zones, 0, 64, 0, false, false));
		assertFalse(CourseProtection.mayBreak(zones, 100, 64, 100, true, false));
		assertTrue(CourseProtection.mayBreak(zones, 100, 64, 100, false, false));
		assertTrue(CourseProtection.mayBreak(zones, 0, 64, 0, false, true));
		assertTrue(CourseProtection.mayBreak(zones, 100, 64, 100, true, true));
	}

	// ------------------------------------------------------------------
	// M8.10 S0: landscape perimeter verdicts
	// ------------------------------------------------------------------

	@Test
	void resolveAllowsPositionsOutsideEveryRegion() {
		assertSame(ProtectionVerdict.ALLOW, CourseProtection.resolve(List.of(), List.of(), 100, 64, 100));
		assertSame(ProtectionVerdict.ALLOW,
			CourseProtection.resolve(teeZones(), List.of(landscape(false)), 999, 64, 999));
	}

	@Test
	void resolveTreatsTeeCupCylindersAsNonOperatorOnly() {
		assertSame(ProtectionVerdict.DENY_NON_OP,
			CourseProtection.resolve(teeZones(), List.of(), 0, 64, 0));
	}

	@Test
	void resolveHonorsLandscapeLockState() {
		assertSame(ProtectionVerdict.DENY_NON_OP,
			CourseProtection.resolve(List.of(), List.of(landscape(false)), 0, 64, 0));
		assertSame(ProtectionVerdict.DENY_ALL,
			CourseProtection.resolve(List.of(), List.of(landscape(true)), 0, 64, 0));
	}

	@Test
	void lockedLandscapeOverridesCylinderAtSamePosition() {
		// Inside both a tee/cup cylinder (DENY_NON_OP) and a locked box (DENY_ALL).
		assertSame(ProtectionVerdict.DENY_ALL,
			CourseProtection.resolve(teeZones(), List.of(landscape(true)), 0, 64, 0));
		assertSame(ProtectionVerdict.DENY_ALL,
			CourseProtection.resolve(teeZones(), List.of(landscape(false), landscape(true)), 0, 64, 0));
	}

	@Test
	void fullBreakPolicyCombinesZonesLandscapeLockAndOperatorPermission() {
		List<ProtectedZone> zones = teeZones();
		List<CourseLandscape> unlocked = List.of(landscape(false));
		List<CourseLandscape> locked = List.of(landscape(true));

		// Outside everything: everyone may break.
		assertTrue(CourseProtection.mayBreak(zones, unlocked, 999, 64, 999, false, false));
		// Cylinder vicinity: non-operators denied, operators exempt.
		assertFalse(CourseProtection.mayBreak(zones, unlocked, 0, 64, 0, false, false));
		assertTrue(CourseProtection.mayBreak(zones, unlocked, 0, 64, 0, false, true));
		// Unlocked perimeter far from the cylinder: non-operator denied, operator exempt.
		assertFalse(CourseProtection.mayBreak(List.of(), unlocked, 40, 64, 0, false, false));
		assertTrue(CourseProtection.mayBreak(List.of(), unlocked, 40, 64, 0, false, true));
		// Locked perimeter: nobody may break, operators included.
		assertFalse(CourseProtection.mayBreak(List.of(), locked, 40, 64, 0, false, false));
		assertFalse(CourseProtection.mayBreak(List.of(), locked, 40, 64, 0, false, true));
		// The cup block is non-operator protected even on an otherwise unlocked course.
		assertFalse(CourseProtection.mayBreak(List.of(), unlocked, 40, 64, 0, true, false));
		assertTrue(CourseProtection.mayBreak(List.of(), unlocked, 40, 64, 0, true, true));
	}

	@Test
	void tntVerdictExpandsLandscapePerimetersByTheBlastMargin() {
		List<CourseLandscape> unlocked = List.of(landscape(false));
		List<CourseLandscape> locked = List.of(landscape(true));

		// (53, 64, 0) is 3 blocks outside the x=50 face: outside for exact checks,
		// inside once the 4-block TNT blast margin is applied.
		assertSame(ProtectionVerdict.ALLOW, CourseProtection.resolve(List.of(), unlocked, 53, 64, 0));
		assertSame(ProtectionVerdict.DENY_NON_OP,
			CourseProtection.resolveForTnt(List.of(), unlocked, 4.0, 53, 64, 0));
		assertSame(ProtectionVerdict.DENY_ALL,
			CourseProtection.resolveForTnt(List.of(), locked, 4.0, 53, 64, 0));

		// Beyond the margin and far away stay allowed.
		assertSame(ProtectionVerdict.ALLOW,
			CourseProtection.resolveForTnt(List.of(), unlocked, 4.0, 55, 64, 0));
		assertSame(ProtectionVerdict.ALLOW,
			CourseProtection.resolveForTnt(List.of(), locked, 4.0, 500, 64, 500));

		// A zero margin leaves TNT verdicts identical to exact verdicts.
		assertSame(ProtectionVerdict.ALLOW,
			CourseProtection.resolveForTnt(List.of(), unlocked, 0.0, 53, 64, 0));

		// Tee/cup cylinders are combined exactly, and a locked perimeter still wins.
		assertSame(ProtectionVerdict.DENY_NON_OP,
			CourseProtection.resolveForTnt(teeZones(), List.of(), 4.0, 0, 64, 0));
		assertSame(ProtectionVerdict.DENY_ALL,
			CourseProtection.resolveForTnt(teeZones(), locked, 4.0, 0, 64, 0));

		assertThrows(IllegalArgumentException.class,
			() -> CourseProtection.resolveForTnt(List.of(), unlocked, -1.0, 0, 64, 0));
	}

	private static CourseDefinition fixtureCourse() {
		return new CourseDefinition(
			"minecraft_golf:fixture_course",
			"Fixture Course",
			"minecraft:overworld",
			new GeneratedLayoutIdentity("minecraft_golf:fixture_layout", 1),
			List.of(
				hole("minecraft_golf:fixture_hole_1", 1, 4, 5.5, 25.5),
				hole("minecraft_golf:fixture_hole_2", 2, 3, 55.5, 75.5),
				hole("minecraft_golf:fixture_hole_3", 3, 5, 105.5, 125.5)));
	}

	/** One straight hole along +X on a shared line; the cup sits 20 blocks short of the next tee. */
	private static HoleDefinition hole(String id, int number, int par, double teeX, double cupX) {
		return new HoleDefinition(id, number, "minecraft:overworld",
			new Vec3(teeX, 64.25, 0.5), new Vec3(cupX, 64.25, 0.5), par,
			volume((int) teeX - 20, 48, -16, (int) cupX + 20, 96, 16));
	}

	private static HoleBoundary volume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return new HoleBoundary(new Vec3(minX, minY, minZ), new Vec3(maxX, maxY, maxZ));
	}

	private static List<ProtectedZone> teeZones() {
		return List.of(new ProtectedZone("tee", 1,
			new pro.apdev.biomegolf.golf.Vec3(0, 64, 0), 5, 3));
	}

	private static CourseLandscape landscape(boolean locked) {
		return new CourseLandscape("pine-hills", "minecraft:overworld",
			new HoleBoundary(new pro.apdev.biomegolf.golf.Vec3(-50, -64, -50),
				new pro.apdev.biomegolf.golf.Vec3(50, 320, 50)),
			locked);
	}
}
