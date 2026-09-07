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
 * (20 ticks/sec run by the M1 {@code BallPhysics}). Display loft is catalog
 * metadata; horizontal and upward launch components independently control the
 * actual trajectory so playtest carry and apex can be tuned separately.</p>
 *
 * @param id                   stable lowercase key, e.g. {@code "driver"}
 * @param displayName          user-facing label e.g. {@code "Driver"}
 * @param nominalCarry         intended full-power carry on flat normal ground, blocks
 * @param fullPowerHorizontalSpeed horizontal launch speed at full power, blocks/tick
 * @param fullPowerUpwardSpeed upward launch speed at full power, blocks/tick
 * @param displayLoftDegrees   player-facing club loft metadata; does not drive physics
 * @param accuracySensitivity  how strongly a unit of aim/power error shifts aim; higher = tighter (M3+)
 * @param putting              true for the putter (roll-dominant trajectory, no loft)
 * @param meleeDamage          extra flat attack damage dealt when swung as a weapon (ARCH §12, PRD §9)
 */
public record ClubDefinition(
		String id,
		String displayName,
		double nominalCarry,
		double fullPowerHorizontalSpeed,
		double fullPowerUpwardSpeed,
		double displayLoftDegrees,
		double accuracySensitivity,
		boolean putting,
		double meleeDamage) {

	public ClubDefinition {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(displayName, "displayName");
		if (id.isEmpty() || id.isBlank()) {
			throw new IllegalArgumentException("club id must not be blank");
		}
		if (displayName.isEmpty() || displayName.isBlank()) {
			throw new IllegalArgumentException("club displayName must not be blank");
		}
		if (fullPowerHorizontalSpeed <= 0.0) {
			throw new IllegalArgumentException(
					"fullPowerHorizontalSpeed must be > 0: " + fullPowerHorizontalSpeed);
		}
		if (fullPowerUpwardSpeed < 0.0) {
			throw new IllegalArgumentException(
					"fullPowerUpwardSpeed must be >= 0: " + fullPowerUpwardSpeed);
		}
		if (nominalCarry < 0.0) {
			throw new IllegalArgumentException("nominalCarry must be >= 0: " + nominalCarry);
		}
		if (displayLoftDegrees < 0.0 || displayLoftDegrees > 90.0) {
			throw new IllegalArgumentException(
					"displayLoftDegrees must be within [0,90]: " + displayLoftDegrees);
		}
		if (accuracySensitivity < 0.0) {
			throw new IllegalArgumentException("accuracySensitivity must be >= 0: " + accuracySensitivity);
		}
		if (meleeDamage < 0.0) {
			throw new IllegalArgumentException("meleeDamage must be >= 0: " + meleeDamage);
		}
	}

	/** Magnitude of the full-power launch vector. */
	public double fullPowerSpeed() {
		return Math.hypot(fullPowerHorizontalSpeed, fullPowerUpwardSpeed);
	}

	/** A club whose full-power speed would exceed the physics launch ceiling is a configuration bug. */
	public boolean exceedsMaxSpeed(double maxLaunchSpeed) {
		return fullPowerSpeed() > maxLaunchSpeed;
	}

	@Override
	public String toString() {
		return "ClubDefinition[%s (%s), carry=%s".formatted(id, displayName, nominalCarry);
	}
}
