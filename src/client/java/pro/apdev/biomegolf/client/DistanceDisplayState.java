package pro.apdev.biomegolf.client;

import pro.apdev.biomegolf.golf.DistanceUnit;

/** Client-local distance presentation preference; it is never gameplay state. */
public final class DistanceDisplayState {
	private static DistanceUnit unit = DistanceUnit.YARDS;

	private DistanceDisplayState() {
	}

	public static DistanceUnit unit() {
		return unit;
	}

	public static DistanceUnit toggle() {
		unit = unit.toggled();
		return unit;
	}

	public static String format(double blocks) {
		return unit.format(blocks);
	}
}
