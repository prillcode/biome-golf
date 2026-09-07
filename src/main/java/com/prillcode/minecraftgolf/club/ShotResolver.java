package com.prillcode.minecraftgolf.club;

import com.prillcode.minecraftgolf.golf.Vec3;

/**
 * Resolves a shot's initial velocity from a club and an aim direction
 * (ARCHITECTURE.md §8, §12).
 *
 * <p>This is a pure function: identical input produces an identical result for
 * a given club and aim, so it is unit-testable on the plain JVM and suitable as
 * the server-authoritative resolution point. Dispersion/accuracy modelling
 * (M3+ metering) will be layered here without changing the call contract.</p>
 *
 * <p>Aim is expressed in Minecraft's own rotation convention so the server can
 * feed a player's {@code getYRot()}/{@code getXRot()} straight in without a
 * second angle conversion:
 * <ul>
 *   <li>{@code aimYawDegrees} — Minecraft yaw; 0 faces +Z (south), increases
 *       clockwise when viewed from above (east = -90).</li>
 *   <li>{@code aimPitchDegrees} — Minecraft pitch; 0 is level, positive looks
 *       down.</li>
 * </ul>
 * For a putt or low-loft shot the club's {@code launchAngleDegrees} still
 * governs the vertical component (the putter uses ~0), so pitch is only used to
 * suppress clicks aimed far off the ground.</p>
 */
public final class ShotResolver {

	private ShotResolver() {
	}

	/**
	 * Computes the full-power initial velocity vector (blocks/tick) for a club
	 * launched along the given aim.
	 *
	 * <p>Horizontal speed is the club's {@code fullPowerSpeed}; the launch angle
	 * is taken from the club (each club has its own loft). The returned vector's
	 * total magnitude is then capped by {@code maxSpeed} (the physics launch
	 * ceiling) so resolution can never ask the M1 physics to exceed it.</p>
	 *
	 * @param club            club being used
	 * @param aimYawDegrees   Minecraft yaw of the aim direction
	 * @param aimPitchDegrees Minecraft pitch of the aim direction
	 * @param maxSpeed        physics ceiling (blocks/tick) to clamp against
	 * @return the initial velocity, or {@code null} if the aim is too steeply
	 *         down/up to be a legal shot (e.g. pointing at the feet)
	 */
	public static Vec3 initialVelocity(
			ClubDefinition club, double aimYawDegrees, double aimPitchDegrees, double maxSpeed) {
		double speed = club.fullPowerSpeed();
		if (speed > maxSpeed) {
			speed = maxSpeed;
		}

		// Total speed is decomposed by the club's loft into horizontal and vertical
		// so that the resulting vector magnitude equals the club's fullPowerSpeed
		// (and stays within maxSpeed).
		double launchRad = Math.toRadians(club.launchAngleDegrees());
		double horizontal = speed * Math.cos(launchRad);
		double up = speed * Math.sin(launchRad);

		// Minecraft yaw -> direction (+Z south when yaw 0; -X east when yaw -90).
		double yaw = Math.toRadians(aimYawDegrees);
		double dirX = -Math.sin(yaw);
		double dirZ = Math.cos(yaw);

		// Steeply-down click (aiming at the ground near the feet) is not a legal shot.
		if (aimPitchDegrees > 45.0) {
			return null;
		}

		Vec3 v = Vec3.of(dirX * horizontal, up, dirZ * horizontal);
		return v;
	}
}
