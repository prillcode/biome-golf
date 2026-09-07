package com.prillcode.minecraftgolf.club;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The initial MVP club catalog (ARCHITECTURE.md §12, PRD §8).
 *
 * <p>Seven reusable {@link ClubDefinition}s matching the PRD initial set. Values
 * are deliberate first-pass tuning targets, not the result of playtests; they
 * should be adjusted in M7 hardening from observed behavior, not by editing
 * call sites or item registrations. Speeds are chosen comfortably below the M1
 * {@code PhysicsConfig.DEFAULT.maxLaunchSpeed} (4.0 blocks/tick) so a full-power
 * drive never trips the anti-tunneling safety ceiling or the launch clamp.</p>
 *
 * <p>The catalog is immutable and exposed by {@code id}. Adding a club later is
 * a one-line change here plus an item registration.</p>
 */
public final class GolfClubs {

	/** Driver: longest club, low-loft/fast full-power carry. */
	public static final ClubDefinition DRIVER = club("driver", "Driver", 82.0, 1.95, 12.0, 0.80, false);
	/** Fairway Wood: long but a touch higher/softer than the driver. */
	public static final ClubDefinition FAIRWAY_WOOD = club("fairway_wood", "Fairway Wood", 66.0, 1.72, 14.0, 0.82, false);
	/** Long Iron. */
	public static final ClubDefinition LONG_IRON = club("long_iron", "Long Iron", 46.0, 1.52, 16.0, 0.86, false);
	/** Mid Iron. */
	public static final ClubDefinition MID_IRON = club("mid_iron", "Mid Iron", 33.0, 1.36, 20.0, 0.90, false);
	/** Short Iron: higher loft, shorter carry. */
	public static final ClubDefinition SHORT_IRON = club("short_iron", "Short Iron", 22.0, 1.22, 26.0, 0.94, false);
	/** Wedge: high loft, short low-trajectory high-spin feel. */
	public static final ClubDefinition WEDGE = club("wedge", "Wedge", 13.0, 1.12, 34.0, 0.96, false);
	/** Putter: roll-dominant, near-zero launch, shortest range. */
	public static final ClubDefinition PUTTER = club("putter", "Putter", 7.0, 1.05, 0.5, 1.00, true);

	/** Ordered MVP club set (roughly longest first for inventory/HUD ordering). */
	public static final List<ClubDefinition> ALL = List.of(
			DRIVER, FAIRWAY_WOOD, LONG_IRON, MID_IRON, SHORT_IRON, WEDGE, PUTTER);

	private static final Map<String, ClubDefinition> BY_ID =
			Collections.unmodifiableMap(ALL.stream()
					.collect(Collectors.toMap(ClubDefinition::id, Function.identity())));

	private GolfClubs() {
	}

	private static ClubDefinition club(String id, String name, double carry, double speed,
			double angle, double sensitivity, boolean putting) {
		return new ClubDefinition(id, name, carry, speed, angle, sensitivity, putting);
	}

	/** Look up a club by its stable {@code id}. */
	public static Optional<ClubDefinition> byId(String id) {
		return Optional.ofNullable(id == null ? null : BY_ID.get(id));
	}

	/** All club ids in the catalog, in presentation order. */
	public static List<String> ids() {
		List<String> out = new ArrayList<>(ALL.size());
		for (ClubDefinition c : ALL) {
			out.add(c.id());
		}
		return out;
	}
}
