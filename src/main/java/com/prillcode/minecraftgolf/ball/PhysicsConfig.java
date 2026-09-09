package com.prillcode.minecraftgolf.ball;

/**
 * Central tunable physics coefficients (ARCHITECTURE.md §10, MILESTONES.md M1).
 *
 * <p>All values likely to change during playtesting live here instead of being
 * scattered through game logic. Units: blocks and ticks (20 ticks/second).</p>
 *
 * @param gravity           downward acceleration per tick squared (blocks/tick²)
 * @param airDrag           per-tick multiplicative drag while airborne (1.0 = none)
 * @param restitution       base bounce energy retention on the normal axis (0–1)
 * @param bounceFloorSpeed  vertical impact speed below which a contact rolls instead of bouncing (blocks/tick)
 * @param stopSpeed         grounded speed below which the ball comes to rest (blocks/tick)
 * @param maxLaunchSpeed    sanity clamp applied by shot/dev-launch input (blocks/tick)
 * @param maxStepDistance   maximum collision-sweep distance per sub-step; prevents high-speed tunneling (blocks)
 * @param maxSubsteps       hard cap on sub-steps per tick (performance guard)
 */
public record PhysicsConfig(
		double gravity,
		double airDrag,
		double restitution,
		double bounceFloorSpeed,
		double stopSpeed,
		double maxLaunchSpeed,
		double maxStepDistance,
		int maxSubsteps
) {

	public PhysicsConfig {
		if (gravity < 0.0) {
			throw new IllegalArgumentException("gravity must be >= 0: " + gravity);
		}
		if (airDrag <= 0.0 || airDrag > 1.0) {
			throw new IllegalArgumentException("airDrag must be within (0,1]: " + airDrag);
		}
		if (restitution < 0.0 || restitution > 1.0) {
			throw new IllegalArgumentException("restitution must be within [0,1]: " + restitution);
		}
		if (bounceFloorSpeed < 0.0) {
			throw new IllegalArgumentException("bounceFloorSpeed must be >= 0: " + bounceFloorSpeed);
		}
		if (stopSpeed < 0.0) {
			throw new IllegalArgumentException("stopSpeed must be >= 0: " + stopSpeed);
		}
		if (maxLaunchSpeed <= 0.0) {
			throw new IllegalArgumentException("maxLaunchSpeed must be > 0: " + maxLaunchSpeed);
		}
		if (maxStepDistance <= 0.0) {
			throw new IllegalArgumentException("maxStepDistance must be > 0: " + maxStepDistance);
		}
		if (maxSubsteps < 1) {
			throw new IllegalArgumentException("maxSubsteps must be >= 1: " + maxSubsteps);
		}
	}

	/** Starting point for M1 tuning; adjust from playtests, not by editing call sites. */
	public static final PhysicsConfig DEFAULT = new PhysicsConfig(
			0.06,   // gravity: snappier fall than vanilla items (~0.04), floatier than stone
			0.99,   // airDrag: ~1% velocity loss per tick while airborne
			0.60,   // restitution: lively golf-ball bounce on normal ground
			0.08,   // bounceFloorSpeed: below this vertical impact speed the ball rolls
			0.03,   // stopSpeed: 0.6 blocks/second threshold to count as stopped
			5.0,    // maxLaunchSpeed: headroom for the raised 150-block Driver flight
			0.5,    // maxStepDistance: half-block sweep granularity
			16      // maxSubsteps: 4 blocks/tick / 0.5 = 8 typical worst case; cap with headroom
	);
}
