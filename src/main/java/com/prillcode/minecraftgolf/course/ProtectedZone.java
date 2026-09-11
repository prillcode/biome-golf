package com.prillcode.minecraftgolf.course;

import java.util.Objects;

import com.prillcode.minecraftgolf.golf.Vec3;

/**
 * A Minecraft-free cylindrical protection volume around a course point.
 *
 * <p>M7 S1 keys course protection on tee/cup vicinity (recorded decision
 * 2026-09-10): each zone is a vertical cylinder — a radius in the horizontal
 * (x/z) plane plus a vertical half-height around the zone center. Containment
 * uses exact block coordinates, so the guard can test a {@code BlockPos}
 * directly without converting through Minecraft geometry classes.</p>
 *
 * @param kind             descriptive label, e.g. {@code "tee"} or {@code "cup"}
 * @param holeNumber       the hole this zone protects (for diagnostics)
 * @param center           zone center in blocks
 * @param radius           horizontal vicinity radius in blocks
 * @param verticalHalfHeight half the vertical extent of the cylinder in blocks
 */
public record ProtectedZone(
	String kind,
	int holeNumber,
	Vec3 center,
	double radius,
	double verticalHalfHeight
) {
	public ProtectedZone {
		requireNonBlank(kind, "kind");
		if (holeNumber <= 0) {
			throw new IllegalArgumentException("holeNumber must be positive: " + holeNumber);
		}
		Objects.requireNonNull(center, "center");
		requireFinite(center, "center");
		requirePositiveFinite(radius, "radius");
		requirePositiveFinite(verticalHalfHeight, "verticalHalfHeight");
	}

	/** Returns whether the exact block coordinate lies inside this cylinder. */
	public boolean contains(int x, int y, int z) {
		double dx = x - center.x();
		double dz = z - center.z();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		return horizontal <= radius && Math.abs(y - center.y()) <= verticalHalfHeight;
	}

	/** Returns whether the precise position lies inside this cylinder. */
	public boolean contains(Vec3 position) {
		Objects.requireNonNull(position, "position");
		requireFinite(position, "position");
		double dx = position.x() - center.x();
		double dz = position.z() - center.z();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		return horizontal <= radius && Math.abs(position.y() - center.y()) <= verticalHalfHeight;
	}

	private static void requireNonBlank(String value, String name) {
		Objects.requireNonNull(value, name);
		if (value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
	}

	private static void requirePositiveFinite(double value, String name) {
		if (!(value > 0.0) || !Double.isFinite(value)) {
			throw new IllegalArgumentException(name + " must be finite and > 0: " + value);
		}
	}

	private static void requireFinite(Vec3 value, String name) {
		if (!Double.isFinite(value.x()) || !Double.isFinite(value.y()) || !Double.isFinite(value.z())) {
			throw new IllegalArgumentException(name + " coordinates must be finite");
		}
	}
}