package com.prillcode.minecraftgolf.club;

/**
 * Pure timing/power model for the M10.1 held-use shot input, kept free of
 * Minecraft types so it stays unit-testable.
 *
 * <p>A client without the mod (vanilla Java or Bedrock through Geyser) holds
 * use near its own resting ball. The server times the hold; release converts
 * elapsed ticks into a legal power value and funnels it through the unchanged
 * {@code ShotService}. The three-click meter remains the modded-client path.</p>
 */
public final class HeldShotRules {

	/** Ticks of holding required to reach full power (1 second at 20 TPS). */
	public static final int CHARGE_TICKS = 20;

	/** Safety cap: a hold this long auto-fires rather than lingering forever. */
	public static final int MAX_HOLD_TICKS = 200;

	/** A quick tap still produces at least this much power. */
	public static final float MIN_POWER = 0.10f;

	/** Held-use has no accuracy input; a centred lane is the neutral default. */
	public static final float HELD_ACCURACY = 0.5f;

	private HeldShotRules() {
	}

	/** Maps held ticks to a power in {@code [MIN_POWER, 1.0]}. */
	public static float power(int heldTicks) {
		if (heldTicks <= 0) {
			return MIN_POWER;
		}
		float ratio = (float) heldTicks / CHARGE_TICKS;
		return Math.clamp(ratio, MIN_POWER, 1.0f);
	}

	/** True once a hold should fire even if no release arrives. */
	public static boolean shouldAutoFire(int heldTicks) {
		return heldTicks >= MAX_HOLD_TICKS;
	}
}
