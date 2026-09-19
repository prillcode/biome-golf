package com.prillcode.minecraftgolf.ball;

/**
 * Per-shot landing behavior carried by the authoritative ball alongside velocity.
 * This keeps normal surface friction intact for putting while allowing lofted
 * clubs to lose realistic energy when they first contact the ground.
 */
public record ShotPhysicsProfile(
	double launchHorizontalMultiplier,
	double launchVerticalMultiplier,
	double landingHorizontalRetention,
	double rollingFrictionMultiplier
) {

	public static final ShotPhysicsProfile STANDARD = new ShotPhysicsProfile(1.0, 1.0, 1.0, 1.0);
	public static final ShotPhysicsProfile LOFTED_CLUB = new ShotPhysicsProfile(1.0, 1.0, 0.25, 0.82);
	public static final ShotPhysicsProfile CHIP = new ShotPhysicsProfile(1.05, 0.62, 0.72, 0.72);
	public static final ShotPhysicsProfile STINGER = new ShotPhysicsProfile(1.08, 0.42, 0.60, 0.65);
	public static final ShotPhysicsProfile FLOP = new ShotPhysicsProfile(0.62, 1.55, 0.12, 0.50);

	/** Compatibility constructor for callers that only specify landing behavior. */
	public ShotPhysicsProfile(double landingHorizontalRetention, double rollingFrictionMultiplier) {
		this(1.0, 1.0, landingHorizontalRetention, rollingFrictionMultiplier);
	}

	public ShotPhysicsProfile {
		if (launchHorizontalMultiplier <= 0.0 || launchVerticalMultiplier <= 0.0) {
			throw new IllegalArgumentException("launch multipliers must be > 0");
		}
		if (landingHorizontalRetention < 0.0 || landingHorizontalRetention > 1.0) {
			throw new IllegalArgumentException("landingHorizontalRetention must be within [0,1]");
		}
		if (rollingFrictionMultiplier <= 0.0 || rollingFrictionMultiplier > 1.0) {
			throw new IllegalArgumentException("rollingFrictionMultiplier must be within (0,1]");
		}
	}
}
