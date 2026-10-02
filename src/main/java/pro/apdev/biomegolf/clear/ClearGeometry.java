package pro.apdev.biomegolf.clear;

/**
 * Minecraft-free geometry for the Builder bulk-clear tool: yaw-to-axis
 * resolution, odd width rounding, and the inclusive cleared box.
 *
 * <p>Minecraft yaw convention: 0 degrees faces +Z (south), 90 faces -X
 * (west), 180 faces -Z (north), 270 faces +X (east). A diagonal yaw rounds to
 * the nearest cardinal axis.</p>
 */
public final class ClearGeometry {

	public enum Axis {
		POS_X, NEG_X, POS_Z, NEG_Z
	}

	public static final int MAX_WIDTH = 21;
	public static final int MAX_DEPTH = 64;
	public static final int MIN_HEIGHT = -64;
	public static final int MAX_HEIGHT = 64;

	private ClearGeometry() {
	}

	/** Nearest cardinal axis for a Minecraft yaw in degrees. */
	public static Axis axisForYaw(double yawDegrees) {
		double normalized = ((yawDegrees % 360.0) + 360.0) % 360.0;
		int quadrant = (int) Math.floor((normalized + 45.0) / 90.0) % 4;
		return switch (quadrant) {
			case 0 -> Axis.POS_Z;
			case 1 -> Axis.NEG_X;
			case 2 -> Axis.NEG_Z;
			default -> Axis.POS_X;
		};
	}

	/** Rounds an even width up to the next odd number (8 -> 9); odd values pass through. */
	public static int oddWidth(int width) {
		if (width <= 0) {
			return 1;
		}
		return width % 2 == 0 ? width + 1 : width;
	}

	/**
	 * The inclusive cleared box for a player standing on {@code (feetX, feetY,
	 * feetZ)}. Depth starts one block ahead of the feet along {@code axis};
	 * width is centered on the feet block. A positive height clears upward from
	 * feet level; a negative height clears downward from feet level, including
	 * the foot-level block — safe because the box never contains the player's
	 * own standing column (depth starts ahead of the feet), so digging leaves no
	 * rim at foot level.
	 */
	public static ClearBox box(int feetX, int feetY, int feetZ, Axis axis, int depth, int width, int height) {
		int side = (oddWidth(width) - 1) / 2;
		int depthBlocks = Math.max(1, depth);
		int minX;
		int maxX;
		int minZ;
		int maxZ;
		switch (axis) {
			case POS_X -> {
				minX = feetX + 1;
				maxX = feetX + depthBlocks;
				minZ = feetZ - side;
				maxZ = feetZ + side;
			}
			case NEG_X -> {
				minX = feetX - depthBlocks;
				maxX = feetX - 1;
				minZ = feetZ - side;
				maxZ = feetZ + side;
			}
			case POS_Z -> {
				minX = feetX - side;
				maxX = feetX + side;
				minZ = feetZ + 1;
				maxZ = feetZ + depthBlocks;
			}
			default -> {
				minX = feetX - side;
				maxX = feetX + side;
				minZ = feetZ - depthBlocks;
				maxZ = feetZ - 1;
			}
		}
		int minY;
		int maxY;
		if (height > 0) {
			minY = feetY;
			maxY = feetY + height - 1;
		} else {
			minY = feetY + height;
			maxY = feetY;
		}
		return new ClearBox(minX, maxX, minY, maxY, minZ, maxZ);
	}

	/** Inclusive integer box over the cleared volume. */
	public record ClearBox(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		public int volume() {
			return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
		}
	}
}