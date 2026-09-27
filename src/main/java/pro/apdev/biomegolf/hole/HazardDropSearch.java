package pro.apdev.biomegolf.hole;

import java.util.Optional;

import pro.apdev.biomegolf.golf.Vec3;

/**
 * Pure search for a legal drop position near a water/lava entry point.
 *
 * <p><b>Phase 1 — back on the line.</b> The ball is walked backwards along the
 * incoming shot heading to find the nearest dry land. The exact line is scanned
 * first and fully, so a call that would land on a bunker keeps walking back on
 * the line instead of stepping sideways; a widening cone is used only after the
 * line itself has no valid target. This is the closest practical match to real
 * "back on the line" relief and recovers shots that landed short and rolled
 * into a hazard.</p>
 *
 * <p><b>Phase 2 — bounded radial fallback.</b> When the line and cone find
 * nothing (open water straight back, or the near shore lies outside the hole),
 * the search sweeps every direction around the entry point, but only within a
 * short radius so it cannot reach the far bank across a wide hazard.</p>
 *
 * <p>All phases honour the hole boundary and the caller's resting-site
 * resolver. The caller falls back to the previous-shot position when all
 * return empty, so this class never has to invent a target.</p>
 */
public final class HazardDropSearch {

	/** Default farthest the drop search walks back along the shot line, in blocks. */
	public static final double MAX_BACKTRACK_BLOCKS = 24.0;

	/** The all-directions fallback stays near the entry so it cannot reach the far bank. */
	private static final double RADIAL_MAX_BLOCKS = 24.0;

	private static final double STEP_BLOCKS = 0.5;
	private static final double[] LINE_ANGLES_DEGREES = { 0.0 };
	private static final double[] CONE_ANGLES_DEGREES = { 20.0, -20.0, 45.0, -45.0 };
	private static final double[] RADIAL_ANGLES_DEGREES = {
		0.0, 22.5, -22.5, 45.0, -45.0, 67.5, -67.5, 90.0, -90.0,
		112.5, -112.5, 135.0, -135.0, 157.5, -157.5, 180.0
	};

	private HazardDropSearch() {
	}

	@FunctionalInterface
	public interface RestSiteResolver {
		/** Resolves a safe resting ball-center position at the given horizontal column, or empty. */
		Optional<Vec3> safeRestAt(double x, double z);
	}

	/** Finds a drop with the default {@link #MAX_BACKTRACK_BLOCKS} bound. */
	public static Optional<Vec3> findDrop(Vec3 entryPoint, Vec3 incomingHeading,
			HoleBoundary boundary, RestSiteResolver resolver) {
		return findDrop(entryPoint, incomingHeading, boundary, MAX_BACKTRACK_BLOCKS, resolver);
	}

	/**
	 * Finds a drop position for the given entry point.
	 *
	 * @param entryPoint        first point where the ball touched hazard fluid
	 * @param incomingHeading   the ball's heading at contact (any magnitude; only
	 *                          the horizontal direction is used)
	 * @param boundary          playable hole region the drop must stay inside
	 * @param maxBacktrackBlocks farthest distance to walk back from the entry point
	 * @param resolver          terrain adapter that returns a safe rest site per column
	 */
	public static Optional<Vec3> findDrop(Vec3 entryPoint, Vec3 incomingHeading,
			HoleBoundary boundary, double maxBacktrackBlocks, RestSiteResolver resolver) {
		Vec3 heading = horizontalUnit(incomingHeading);
		if (heading == null) {
			return Optional.empty();
		}
		Optional<Vec3> line = scan(entryPoint, heading, boundary, maxBacktrackBlocks,
			LINE_ANGLES_DEGREES, resolver);
		if (line.isPresent()) {
			return line;
		}
		Optional<Vec3> cone = scan(entryPoint, heading, boundary, maxBacktrackBlocks,
			CONE_ANGLES_DEGREES, resolver);
		if (cone.isPresent()) {
			return cone;
		}
		return scan(entryPoint, heading, boundary,
			Math.min(maxBacktrackBlocks, RADIAL_MAX_BLOCKS), RADIAL_ANGLES_DEGREES, resolver);
	}

	private static Optional<Vec3> scan(Vec3 entryPoint, Vec3 heading, HoleBoundary boundary,
			double maxBacktrackBlocks, double[] angles, RestSiteResolver resolver) {
		for (double distance = STEP_BLOCKS; distance <= maxBacktrackBlocks + 1.0E-9;
				distance += STEP_BLOCKS) {
			for (double angle : angles) {
				Vec3 back = rotate(heading, angle).scale(-distance);
				Optional<Vec3> rest = resolver.safeRestAt(
					entryPoint.x() + back.x(), entryPoint.z() + back.z());
				if (rest.isPresent() && boundary.contains(rest.orElseThrow())) {
					return rest;
				}
			}
		}
		return Optional.empty();
	}

	private static Vec3 horizontalUnit(Vec3 vector) {
		double length = vector.horizontalLength();
		if (length < 1.0E-9) {
			return null;
		}
		return new Vec3(vector.x() / length, 0.0, vector.z() / length);
	}

	private static Vec3 rotate(Vec3 unit, double degrees) {
		if (degrees == 0.0) {
			return unit;
		}
		double radians = Math.toRadians(degrees);
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		return new Vec3(unit.x() * cos - unit.z() * sin, 0.0, unit.x() * sin + unit.z() * cos);
	}
}
