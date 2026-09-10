package com.prillcode.minecraftgolf.course;

/**
 * Server gameplay configuration for the M7 S1 course block-break guard
 * (ARCHITECTURE.md §33 "server gameplay configuration").
 *
 * <p>Deliberately a small code-level record with a sane default, matching the
 * existing {@code PhysicsConfig} pattern rather than introducing a JSON
 * server-config file system for one value (M7 plan: "no full world-guard
 * framework").</p>
 *
 * @param vicinityRadius          horizontal vicinity radius around each hole's tee and cup (blocks)
 * @param vicinityVerticalHalfHeight half the vertical extent of each protection cylinder (blocks)
 */
public record CourseProtectionConfig(
	double vicinityRadius,
	double vicinityVerticalHalfHeight
) {
	public CourseProtectionConfig {
		requirePositiveFinite(vicinityRadius, "vicinityRadius");
		requirePositiveFinite(vicinityVerticalHalfHeight, "vicinityVerticalHalfHeight");
	}

	/**
	 * Default protection radius. 12 blocks fully covers the largest M5 green
	 * (Hole 3's far green block is ~9.8 blocks from its cup) and every M5 tee
	 * box pad with headroom, while keeping most fairway/terrain outside the
	 * zones breakable. Greens are protected through cup vicinity and tee boxes
	 * through tee vicinity per the recorded M7 S1 decision.
	 */
	public static final CourseProtectionConfig DEFAULT =
		new CourseProtectionConfig(12.0, 8.0);

	private static void requirePositiveFinite(double value, String name) {
		if (!(value > 0.0) || !Double.isFinite(value)) {
			throw new IllegalArgumentException(name + " must be finite and > 0: " + value);
		}
	}
}