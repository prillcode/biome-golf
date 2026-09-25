package pro.apdev.biomegolf.golf;

/**
 * Domain-internal golf 3D vector.
 *
 * <p>Immutable; kept free of Minecraft classes so physics math stays testable
 * on the plain JVM. Minecraft vectors are converted at the integration
 * boundary only (ARCHITECTURE.md §7, §10).</p>
 *
 * <p>Units: blocks (position) and blocks/tick (velocity). Y is up.</p>
 */
public record Vec3(double x, double y, double z) {

	public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);

	public static Vec3 of(double x, double y, double z) {
		return new Vec3(x, y, z);
	}

	public Vec3 add(Vec3 other) {
		return new Vec3(x + other.x, y + other.y, z + other.z);
	}

	public Vec3 subtract(Vec3 other) {
		return new Vec3(x - other.x, y - other.y, z - other.z);
	}

	public Vec3 scale(double factor) {
		return new Vec3(x * factor, y * factor, z * factor);
	}

	/** Scales only the horizontal (x/z) components; y passes through unchanged. */
	public Vec3 scaleHorizontal(double factor) {
		return new Vec3(x * factor, y, z * factor);
	}

	public double dot(Vec3 other) {
		return x * other.x + y * other.y + z * other.z;
	}

	/** Component of this vector pointing along the given (unit) normal. */
	public double along(Vec3 normal) {
		return dot(normal);
	}

	public double lengthSquared() {
		return x * x + y * y + z * z;
	}

	public double length() {
		return Math.sqrt(lengthSquared());
	}

	/** Horizontal (x/z) length, ignoring the vertical component. */
	public double horizontalLength() {
		return Math.sqrt(x * x + z * z);
	}

	/**
	 * Returns a unit-length vector, or {@link #ZERO} if this vector has no length.
	 * Does not preserve an arbitrary axis for zero vectors.
	 */
	public Vec3 normalize() {
		double len = length();
		if (len < 1.0E-9) {
			return ZERO;
		}
		return scale(1.0 / len);
	}

	/** The part of this vector parallel to the plane perpendicular to the (unit) normal. */
	public Vec3 tangent(Vec3 normal) {
		return subtract(normal.scale(dot(normal)));
	}

	/**
	 * Reflects this velocity vector about a surface normal
	 * (mirror across the plane perpendicular to the normal).
	 */
	public Vec3 reflect(Vec3 normal) {
		return subtract(normal.scale(2.0 * dot(normal)));
	}

	public boolean anyNaN() {
		return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z);
	}

	@Override
	public String toString() {
		return String.format("(% .4f, % .4f, % .4f)", x, y, z);
	}
}
