package pro.apdev.biomegolf.hole;

import java.util.Objects;

import pro.apdev.biomegolf.golf.Vec3;

/**
 * Inclusive axis-aligned playable region for one hole.
 *
 * <p>This pure domain value deliberately has no dependency on Minecraft's
 * {@code BoundingBox}; adapters convert world coordinates at the boundary.</p>
 */
public record HoleBoundary(Vec3 min, Vec3 max) {

	/** A finite sentinel boundary that leaves all valid Minecraft positions in bounds. */
	public static HoleBoundary unbounded() {
		return new HoleBoundary(
			new Vec3(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE),
			new Vec3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));
	}

	public HoleBoundary {
		Objects.requireNonNull(min, "min");
		Objects.requireNonNull(max, "max");
		requireFinite(min, "min");
		requireFinite(max, "max");
		if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
			throw new IllegalArgumentException("boundary min must not exceed max on any axis");
		}
	}

	/** Returns whether the position lies on or inside every boundary face. */
	public boolean contains(Vec3 position) {
		Objects.requireNonNull(position, "position");
		requireFinite(position, "position");
		return position.x() >= min.x() && position.x() <= max.x()
			&& position.y() >= min.y() && position.y() <= max.y()
			&& position.z() >= min.z() && position.z() <= max.z();
	}

	private static void requireFinite(Vec3 value, String name) {
		if (!Double.isFinite(value.x()) || !Double.isFinite(value.y()) || !Double.isFinite(value.z())) {
			throw new IllegalArgumentException(name + " coordinates must be finite");
		}
	}
}
