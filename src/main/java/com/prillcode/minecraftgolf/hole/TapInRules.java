package com.prillcode.minecraftgolf.hole;

import com.prillcode.minecraftgolf.golf.Vec3;

/** Minecraft-free boundary rule for the server-authoritative tap-in action. */
public final class TapInRules {
	private TapInRules() {
	}

	public static boolean withinOneBlock(Vec3 ball, Vec3 cup) {
		double dx = ball.x() - cup.x();
		double dz = ball.z() - cup.z();
		return dx * dx + dz * dz <= 1.0;
	}
}
