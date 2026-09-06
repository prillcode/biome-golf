package com.prillcode.minecraftgolf.ball;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.surface.SurfaceDefinition;

/**
 * The collision world as seen by the pure physics engine (ARCHITECTURE.md §2.3).
 *
 * <p>Minecraft remains responsible for world geometry and block collision
 * shapes; the golf entity implements this boundary by delegating to Minecraft
 * collision. Keeping it as an interface lets unit tests drive physics against
 * synthetic worlds.</p>
 */
public interface BallCollisionWorld {

	/**
	 * Attempts to move the ball from {@code from} by {@code delta}.
	 *
	 * <p>Implementations must sweep the path (not teleport) and clamp the
	 * endpoint against the first blocking surface, returning the surface
	 * normal of that contact.</p>
	 *
	 * @param from  current ball center
	 * @param delta movement for this sub-step (blocks)
	 * @return the swept result, never null
	 */
	CollisionResult move(Vec3 from, Vec3 delta);

	/**
	 * Resolves the logical golf surface at/under the given position
	 * (grass, sand, ice, slime, honey, ...).
	 */
	SurfaceDefinition surfaceAt(Vec3 position);
}
