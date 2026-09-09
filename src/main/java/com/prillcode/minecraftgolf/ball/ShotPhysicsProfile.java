package com.prillcode.minecraftgolf.ball;

/**
 * Per-shot landing behavior carried by the authoritative ball alongside velocity.
 * This keeps normal surface friction intact for putting while allowing lofted
 * clubs to lose realistic energy when they first contact the ground.
 */
public record ShotPhysicsProfile(
	double landingHorizontalRetention,
	double rollingFrictionMultiplier
) {

	public static final ShotPhysicsProfile STANDARD = new ShotPhysicsProfile(1.0, 1.0);
	public static final ShotPhysicsProfile LOFTED_CLUB = new ShotPhysicsProfile(0.25, 0.82);

	public ShotPhysicsProfile {
		if (landingHorizontalRetention < 0.0 || landingHorizontalRetention > 1.0) {
			throw new IllegalArgumentException("landingHorizontalRetention must be within [0,1]");
		}
		if (rollingFrictionMultiplier <= 0.0 || rollingFrictionMultiplier > 1.0) {
			throw new IllegalArgumentException("rollingFrictionMultiplier must be within (0,1]");
		}
	}
}
