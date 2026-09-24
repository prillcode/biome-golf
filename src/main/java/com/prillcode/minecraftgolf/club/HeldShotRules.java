package com.prillcode.minecraftgolf.club;

/**
 * Pure timing/power model for the client-light tap-meter shot input, kept free of
 * Minecraft types so it stays unit-testable.
 *
 * <p>A client without the mod (vanilla Java or Bedrock through Geyser) taps use near its own
 * resting ball. Each tap advances a power step; the shot fires once the tap window closes or
 * the meter reaches {@link #MAX_TAPS}. The model deliberately does not depend on a
 * use/release pair: a 2026-09 playtest showed Geyser never delivered a release for a club, so
 * the earlier hold-based model always auto-fired at full power. The three-click meter remains
 * the modded-client path; {@code /golf swing <power>} remains the precise floor.</p>
 */
public final class HeldShotRules {

	/** Maximum taps in one meter; each tap is one power step. */
	public static final int MAX_TAPS = 4;

	/** Ticks after the last tap before a sub-maximum meter fires. */
	public static final int FIRE_DELAY_TICKS = 15;

	/** Repeated use actions closer than this are treated as one tap (hold de-bounce). */
	public static final int MIN_TAP_GAP_TICKS = 3;

	/** A gap at least this long starts a fresh meter instead of advancing the current one. */
	public static final int METER_RESET_TICKS = 20;

	/** A single tap still produces this much power. */
	public static final float MIN_POWER = 0.25f;

	/** Tap input has no accuracy lane; a centred lane is the neutral default. */
	public static final float HELD_ACCURACY = 0.5f;

	private HeldShotRules() {
	}

	/** Maps a tap count in {@code [1, MAX_TAPS]} to a legal power; one tap is {@link #MIN_POWER}. */
	public static float powerForTaps(int taps) {
		int clamped = Math.clamp(taps, 1, MAX_TAPS);
		return clamped / (float) MAX_TAPS;
	}

	/** True once a meter should fire: at the maximum tap count, or after the tap window closes. */
	public static boolean shouldFire(int taps, int ticksSinceLastTap) {
		return taps >= MAX_TAPS || ticksSinceLastTap >= FIRE_DELAY_TICKS;
	}

	/** True when a tap starts a fresh meter rather than advancing the current one. */
	public static boolean isNewMeter(int currentTaps, int ticksSinceLastTap) {
		return currentTaps <= 0 || ticksSinceLastTap >= METER_RESET_TICKS;
	}

	/** True when a repeated use action arrives too fast to be a deliberate tap. */
	public static boolean isRepeatWithinGap(int ticksSinceLastTap) {
		return ticksSinceLastTap < MIN_TAP_GAP_TICKS;
	}
}
