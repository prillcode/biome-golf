package pro.apdev.biomegolf.world;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pure helper for the highest collision-shape top under a horizontal point
 * within a single block.
 *
 * <p>The water-drop recovery must rest the ball on the block's real surface.
 * Assuming a full-block top floats the ball above carpets, slabs, snow layers,
 * dirt paths, farms, and similar partial blocks; using the shape maximum would
 * instead float it over the low half of stairs. This scans the shape's AABBs at
 * the ball's local x/z so the ball sits on the solid part only.</p>
 */
public final class CollisionShapeTop {

	private static final double EDGE_EPSILON = 1.0E-7;

	private CollisionShapeTop() {
	}

	/**
	 * Returns the highest collision top under the given block-local horizontal
	 * point, or {@link Double#NaN} when the shape has no collision there.
	 */
	public static double at(VoxelShape shape, double localX, double localZ) {
		if (shape.isEmpty()) {
			return Double.NaN;
		}
		double top = Double.NaN;
		for (AABB box : shape.toAabbs()) {
			if (localX >= box.minX - EDGE_EPSILON && localX <= box.maxX + EDGE_EPSILON
					&& localZ >= box.minZ - EDGE_EPSILON && localZ <= box.maxZ + EDGE_EPSILON) {
				top = Double.isNaN(top) ? box.maxY : Math.max(top, box.maxY);
			}
		}
		return top;
	}
}
