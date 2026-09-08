package com.prillcode.minecraftgolf.hole;

/** Standard golf terminology for a completed numeric hole score. */
public enum GolfScoreTerm {
	HOLE_IN_ONE,
	ALBATROSS,
	EAGLE,
	BIRDIE,
	PAR,
	BOGEY,
	DOUBLE_BOGEY,
	OVER_PAR;

	public static GolfScoreTerm forScore(int strokes, int par) {
		if (strokes <= 0) {
			throw new IllegalArgumentException("strokes must be positive");
		}
		if (par <= 0) {
			throw new IllegalArgumentException("par must be positive");
		}
		if (strokes == 1) {
			return HOLE_IN_ONE;
		}
		int scoreToPar = strokes - par;
		if (scoreToPar <= -3) {
			return ALBATROSS;
		}
		return switch (scoreToPar) {
			case -2 -> EAGLE;
			case -1 -> BIRDIE;
			case 0 -> PAR;
			case 1 -> BOGEY;
			case 2 -> DOUBLE_BOGEY;
			default -> OVER_PAR;
		};
	}
}
