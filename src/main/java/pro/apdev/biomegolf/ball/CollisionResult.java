package pro.apdev.biomegolf.ball;

import java.util.Objects;

import pro.apdev.biomegolf.golf.Vec3;

/**
 * Result of one collision-swept movement attempt against the world.
 *
 * @param position post-move ball center: the swept endpoint, clamped to the
 *                 blocking surface when a collision occurred
 * @param normal   surface normal at the contact point (unit length, pointing
 *                 away from the surface toward the ball); {@code null} when no
 *                 collision occurred
 */
public record CollisionResult(Vec3 position, Vec3 normal) {

	public CollisionResult {
		Objects.requireNonNull(position, "position");
	}

	/** Movement completed without hitting anything. */
	public static CollisionResult moved(Vec3 position) {
		return new CollisionResult(position, null);
	}

	/** Movement was blocked; position is the clamped contact position. */
	public static CollisionResult hit(Vec3 position, Vec3 normal) {
		if (normal == null) {
			throw new IllegalArgumentException("hit() requires a contact normal");
		}
		return new CollisionResult(position, normal);
	}

	public boolean collided() {
		return normal != null;
	}
}
