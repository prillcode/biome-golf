package com.prillcode.minecraftgolf.course;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;

/**
 * Minecraft-free construction of an authored {@link HoleBoundary} from two
 * operator-captured corners.
 *
 * <p>X and Z are normalized per axis from the two corners; Y is deliberately
 * expanded to the world build height so cliffs, elevated shots, and deep
 * terrain stay in-bounds regardless of where the operator stood.</p>
 */
public final class AuthoredHoleBounds {

	private AuthoredHoleBounds() {
	}

	/**
	 * Builds the boundary box from two corners, expanding Y to
	 * [{@code worldMinY}, {@code worldMaxY}].
	 */
	public static HoleBoundary fromCorners(Vec3 cornerA, Vec3 cornerB, double worldMinY, double worldMaxY) {
		if (worldMinY > worldMaxY) {
			throw new IllegalArgumentException("worldMinY must not exceed worldMaxY");
		}
		return new HoleBoundary(
			new Vec3(Math.min(cornerA.x(), cornerB.x()), worldMinY, Math.min(cornerA.z(), cornerB.z())),
			new Vec3(Math.max(cornerA.x(), cornerB.x()), worldMaxY, Math.max(cornerA.z(), cornerB.z())));
	}
}
