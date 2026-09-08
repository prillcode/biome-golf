package com.prillcode.minecraftgolf.hole;

import java.util.Objects;

import com.prillcode.minecraftgolf.golf.Vec3;

/** Pure geometry for authoritative cup-entry detection. */
public final class CupDetector {

	public static final double OPENING_RADIUS = 0.34;
	public static final double VERTICAL_HALF_RANGE = 0.45;
	public static final double MAX_ENTRY_SPEED = 0.25;

	private CupDetector() {
	}

	/**
	 * Returns true when a sufficiently slow ball segment intersects the cup's
	 * capture region. Coordinates represent the ball center.
	 */
	public static boolean entered(Vec3 from, Vec3 to, Vec3 cup, double speed) {
		Objects.requireNonNull(from, "from");
		Objects.requireNonNull(to, "to");
		Objects.requireNonNull(cup, "cup");
		if (!Double.isFinite(speed) || speed < 0.0) {
			throw new IllegalArgumentException("speed must be finite and non-negative");
		}
		return speed <= MAX_ENTRY_SPEED && intersects(from, to, cup);
	}

	/** Returns whether a movement segment crosses the geometric capture region. */
	public static boolean intersects(Vec3 from, Vec3 to, Vec3 cup) {
		Objects.requireNonNull(from, "from");
		Objects.requireNonNull(to, "to");
		Objects.requireNonNull(cup, "cup");

		// Scale the vertical axis so closest-point distance tests an ellipsoidal
		// capture volume: narrow enough that merely approaching does not count.
		double ax = (from.x() - cup.x()) / OPENING_RADIUS;
		double ay = (from.y() - cup.y()) / VERTICAL_HALF_RANGE;
		double az = (from.z() - cup.z()) / OPENING_RADIUS;
		double bx = (to.x() - cup.x()) / OPENING_RADIUS;
		double by = (to.y() - cup.y()) / VERTICAL_HALF_RANGE;
		double bz = (to.z() - cup.z()) / OPENING_RADIUS;
		double dx = bx - ax;
		double dy = by - ay;
		double dz = bz - az;
		double lengthSquared = dx * dx + dy * dy + dz * dz;
		double t = lengthSquared == 0.0 ? 0.0
			: Math.clamp(-(ax * dx + ay * dy + az * dz) / lengthSquared, 0.0, 1.0);
		double x = ax + dx * t;
		double y = ay + dy * t;
		double z = az + dz * t;
		return x * x + y * y + z * z <= 1.0;
	}

	/** Returns whether a ball center is currently inside the capture region. */
	public static boolean contains(Vec3 position, Vec3 cup) {
		return intersects(position, position, cup);
	}
}
