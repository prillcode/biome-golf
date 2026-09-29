package pro.apdev.biomegolf.club;

/**
 * Pure triangular meter math shared by the three-click swing controller and HUD
 * (PRD §5; M3, hardened by M8.11).
 *
 * <p>The value sweeps {@code 0 -> 1 -> 0} across {@code 2 * halfSweepTicks} ticks.
 * An <strong>even</strong> half-sweep is required so the centre value
 * {@link #CENTER} is exactly reachable: an odd sweep samples {@code k / halfSweep}
 * and can never hit {@code 0.5}, which was the M8.11 accuracy defect (the best
 * click carried a guaranteed {@code 1/30} miss).</p>
 *
 * <p>Kept Minecraft-free so the client meter, the deterministic resolver, and
 * plain-JVM tests share one contract for what "perfect" means.</p>
 */
public final class SwingMeter {

	/** The perfect/centre meter value. */
	public static final float CENTER = 0.5f;

	/** Even half-sweep so {@link #CENTER} is exactly reachable. */
	public static final int ACCURACY_HALF_SWEEP_TICKS = 16;

	/** Power half-sweep; even so both {@code 0.5} and {@code 1.0} are reachable. */
	public static final int POWER_HALF_SWEEP_TICKS = 24;

	/**
	 * Half-width, in meter value, of the zero-deviation perfect band. The HUD draws
	 * the perfect zone from this same constant so "inside the green" means "straight".
	 */
	public static final double PERFECT_BAND = 0.05;

	/** Float-precision guard so exact band-boundary samples read as perfect. */
	private static final double BAND_EPSILON = 1.0e-6;

	private SwingMeter() {
	}

	/**
	 * Triangular meter value in {@code [0,1]} at {@code ticks}; {@code 0} and
	 * {@code 1} are the sweep endpoints and the centre is sampled exactly when
	 * {@code halfSweepTicks} is even.
	 *
	 * @throws IllegalArgumentException if {@code halfSweepTicks < 1}
	 */
	public static float value(int ticks, int halfSweepTicks) {
		if (halfSweepTicks < 1) {
			throw new IllegalArgumentException("halfSweepTicks must be >= 1: " + halfSweepTicks);
		}
		int period = halfSweepTicks * 2;
		int within = Math.floorMod(ticks, period);
		float rising = (float) within / halfSweepTicks;
		return rising <= 1.0f ? rising : 2.0f - rising;
	}

	/** True when the sweep samples {@link #CENTER} exactly (even half-sweep). */
	public static boolean centerReachable(int halfSweepTicks) {
		return halfSweepTicks >= 1 && halfSweepTicks % 2 == 0;
	}

	/** True when a meter value lies inside the zero-deviation perfect band. */
	public static boolean isPerfect(double value) {
		return Math.abs(value - CENTER) <= PERFECT_BAND + BAND_EPSILON;
	}

	/**
	 * Remapped accuracy miss magnitude in {@code [0,1]}: {@code 0} anywhere inside the
	 * perfect band, {@code 1} for a full miss. The band is subtracted then renormalised
	 * so a full miss still reaches the full fan.
	 */
	public static double missMagnitude(double accuracy) {
		if (Double.isNaN(accuracy)) {
			return 0.0;
		}
		double valueMiss = Math.abs(accuracy - CENTER); // 0 at centre, 0.5 at either edge
		if (valueMiss <= PERFECT_BAND + BAND_EPSILON) {
			return 0.0;
		}
		return (valueMiss - PERFECT_BAND) / (0.5 - PERFECT_BAND);
	}
}
