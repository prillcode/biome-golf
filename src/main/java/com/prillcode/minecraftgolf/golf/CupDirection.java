package com.prillcode.minecraftgolf.golf;

/** Client-display bearing from the player's view to an authoritative cup position. */
public final class CupDirection {

	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

	private CupDirection() {
	}

	public static String arrow(double playerX, double playerZ, double playerYaw,
			double cupX, double cupZ) {
		double dx = cupX - playerX;
		double dz = cupZ - playerZ;
		double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
		double relative = wrapDegrees(targetYaw - playerYaw);
		int sector = Math.floorMod((int) Math.round(relative / 45.0), ARROWS.length);
		return ARROWS[sector];
	}

	private static double wrapDegrees(double degrees) {
		double wrapped = degrees % 360.0;
		if (wrapped >= 180.0) wrapped -= 360.0;
		if (wrapped < -180.0) wrapped += 360.0;
		return wrapped;
	}
}
