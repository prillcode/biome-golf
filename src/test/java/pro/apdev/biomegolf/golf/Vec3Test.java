package pro.apdev.biomegolf.golf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Vec3 domain math")
class Vec3Test {

	private static final double TOL = 1.0E-9;

	@Test
	void zeroConstantIsOrigin() {
		assertEquals(0.0, Vec3.ZERO.x(), TOL);
		assertEquals(0.0, Vec3.ZERO.y(), TOL);
		assertEquals(0.0, Vec3.ZERO.z(), TOL);
	}

	@Test
	void ofCreatesVector() {
		Vec3 v = Vec3.of(1.0, 2.0, 3.0);
		assertEquals(1.0, v.x(), TOL);
		assertEquals(2.0, v.y(), TOL);
		assertEquals(3.0, v.z(), TOL);
	}

	@Test
	void addAddsComponentWise() {
		Vec3 a = Vec3.of(1.0, 2.0, 3.0);
		Vec3 b = Vec3.of(4.0, 5.0, 6.0);
		Vec3 sum = a.add(b);
		assertEquals(Vec3.of(5.0, 7.0, 9.0), sum);
	}

	@Test
	void subtractSubtractsComponentWise() {
		Vec3 a = Vec3.of(5.0, 7.0, 9.0);
		Vec3 b = Vec3.of(1.0, 2.0, 3.0);
		assertEquals(Vec3.of(4.0, 5.0, 6.0), a.subtract(b));
	}

	@Test
	void scaleMultipliesAllComponents() {
		assertEquals(Vec3.of(2.0, 4.0, 6.0), Vec3.of(1.0, 2.0, 3.0).scale(2.0));
	}

	@Test
	void scaleHorizontalLeavesYAlone() {
		assertEquals(Vec3.of(2.0, 5.0, 6.0), Vec3.of(1.0, 5.0, 3.0).scaleHorizontal(2.0));
	}

	@Test
	void dotIsComponentProductSum() {
		Vec3 a = Vec3.of(1.0, 2.0, 3.0);
		Vec3 b = Vec3.of(4.0, 5.0, 6.0);
		assertEquals(32.0, a.dot(b), TOL);
	}

	@Test
	void alongReturnsProjectionOntoUnitNormal() {
		Vec3 v = Vec3.of(3.0, 4.0, 0.0);
		Vec3 n = Vec3.of(0.0, 1.0, 0.0);
		assertEquals(4.0, v.along(n), TOL);
	}

	@Test
	void lengthAndHorizontalLength() {
		Vec3 v = Vec3.of(3.0, 4.0, 0.0);
		assertEquals(5.0, v.length(), TOL);
		assertEquals(3.0, v.horizontalLength(), TOL);
		assertEquals(25.0, v.lengthSquared(), TOL);
	}

	@Test
	void normalizeProducesUnitVector() {
		Vec3 v = Vec3.of(3.0, 4.0, 0.0).normalize();
		assertEquals(1.0, v.length(), TOL);
		assertEquals(0.6, v.x(), TOL);
		assertEquals(0.8, v.y(), TOL);
	}

	@Test
	void normalizeOfZeroReturnsZero() {
		assertSame(Vec3.ZERO, Vec3.ZERO.normalize());
	}

	@Test
	void tangentStripsNormalComponent() {
		Vec3 v = Vec3.of(3.0, 4.0, 0.0);
		Vec3 n = Vec3.of(0.0, 1.0, 0.0);
		assertEquals(Vec3.of(3.0, 0.0, 0.0), v.tangent(n));
	}

	@Test
	void reflectMirrorsAcrossNormal() {
		Vec3 v = Vec3.of(1.0, -1.0, 0.0);
		Vec3 n = Vec3.of(0.0, 1.0, 0.0);
		assertEquals(Vec3.of(1.0, 1.0, 0.0), v.reflect(n));
	}

	@Test
	void anyNaNDetectsBadComponents() {
		assertTrue(new Vec3(Double.NaN, 0.0, 0.0).anyNaN());
		assertTrue(new Vec3(0.0, Double.NaN, 0.0).anyNaN());
		assertTrue(new Vec3(0.0, 0.0, Double.NaN).anyNaN());
		assertFalse(new Vec3(0.0, 0.0, 0.0).anyNaN());
	}

	@Test
	void toStringIsReadable() {
		String s = Vec3.of(1.0, 2.0, 3.0).toString();
		assertTrue(s.contains("1.0000"));
		assertTrue(s.contains("2.0000"));
		assertTrue(s.contains("3.0000"));
	}
}
