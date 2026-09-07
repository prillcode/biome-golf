package com.prillcode.minecraftgolf.club;

import java.util.Objects;

/**
 * Immutable, reusable data describing one golf club (ARCHITECTURE.md §12,
 * PRD §8). Minecraft item registration wraps a {@code ClubDefinition} instance;
 * multiple items are not needed per club for MVP.
 *
 * <p>This type is Minecraft-free so club data and shot resolution stay testable
 * on the plain JVM. The {@code id} is the stable key used on items, in save
 * data, and any future networking; it must be a lowercase identifier-safe
 * string ({@code driver}, {@code wedge}, ...).</p>
 *
 * <p>Units follow the physics core: distance/speed in blocks and blocks/tick
 * (20 ticks/sec run by the M1 {@code BallPhysics}); {@code launchAngleDegrees}
 * is the full-power vertical launch angle above horizontal.</p>
 *
 * @param id                   stable lowercase key, e.g. {@code "driver"}
 * @param displayName          user-facing label e.g. {@code "Driver"}
 * @param nominalCarry         intended full-power carry on flat normal ground, blocks
 * @param fullPowerSpeed       horizontal launch speed at full power, blocks/tick
 * @param launchAngleDegrees   full-power launch angle above horizontal
 * @param accuracySensitivity  how strongly a unit of aim/power error shifts aim; higher = tighter (M3+)
 * @param putting              true for the putter (roll-dominant trajectory, no loft)
 */
public record ClubDefinition(
		String id,
		String displayName,
		double nominalCarry,
		double fullPowerSpeed,
		double launchAngleDegrees,
		double accuracySensitivity,
		boolean putting) {

	public ClubDefinition {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(displayName, "displayName");
		if (id.isEmpty() || id.isBlank()) {
			throw new IllegalArgumentException("club id must not be blank");
		}
		if (displayName.isEmpty() || displayName.isBlank()) {
			throw new IllegalArgumentException("club displayName must not be blank");
		}
		if (fullPowerSpeed <= 0.0) {
			throw new IllegalArgumentException("fullPowerSpeed must be > 0: " + fullPowerSpeed);
		}
		if (nominalCarry < 0.0) {
			throw new IllegalArgumentException("nominalCarry must be >= 0: " + nominalCarry);
		}
		if (launchAngleDegrees < 0.0 || launchAngleDegrees > 90.0) {
			throw new IllegalArgumentException("launchAngleDegrees must be within [0,90]: " + launchAngleDegrees);
		}
		if (accuracySensitivity < 0.0) {
			throw new IllegalArgumentException("accuracySensitivity must be >= 0: " + accuracySensitivity);
		}
	}

	/** A club whose full-power speed would exceed the physics launch ceiling is a configuration bug. */
	public boolean exceedsMaxSpeed(double maxLaunchSpeed) {
		return fullPowerSpeed > maxLaunchSpeed;
	}

	@Override
	public String toString() {
		return "ClubDefinition[%s (%s), carry=%s".formatted(id, displayName, nominalCarry);
	}
}
