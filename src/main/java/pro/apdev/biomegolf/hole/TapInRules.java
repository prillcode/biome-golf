package pro.apdev.biomegolf.hole;

import pro.apdev.biomegolf.golf.Vec3;

/** Minecraft-free boundary rule for the server-authoritative tap-in action. */
public final class TapInRules {

	/** Horizontal center-to-cup radius, in blocks, within which a resting ball may be tapped in. */
	public static final double TAP_IN_RADIUS_BLOCKS = 2.0;

	private TapInRules() {
	}

	public static boolean withinTapInRadius(Vec3 ball, Vec3 cup) {
		double dx = ball.x() - cup.x();
		double dz = ball.z() - cup.z();
		return dx * dx + dz * dz <= TAP_IN_RADIUS_BLOCKS * TAP_IN_RADIUS_BLOCKS;
	}
}
