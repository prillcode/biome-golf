package com.prillcode.minecraftgolf.ball;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.surface.SurfaceDefinition;

/**
 * Server-authoritative golf-ball physics stepper (ARCHITECTURE.md §10).
 *
 * <p>Pure Java / no Minecraft: takes a {@link BallState}, a {@link PhysicsConfig},
 * and a synthetic {@link BallCollisionWorld}, and returns the next tick's state.</p>
 */
public final class BallPhysics {

	private static final double GROUND_NORMAL_Y = 0.5;
	private static final double SUPPORT_PROBE_DELTA = 1.0e-4;

	private BallPhysics() {
	}

	/**
	 * Steps one tick of physics, using sub-stepping to avoid tunneling.
	 *
	 * <p>The loop shape per sub-step is:</p>
	 * <ol>
	 *   <li>apply gravity and air drag while airborne, or rolling friction while grounded</li>
	 *   <li>sweep the ball through the world</li>
	 *   <li>on collision, bounce or roll based on impact speed and surface</li>
	 * </ol>
	 */
	public static BallState step(BallState state, PhysicsConfig config, BallCollisionWorld world) {
		return step(state, config, world, ShotPhysicsProfile.STANDARD);
	}

	public static BallState step(BallState state, PhysicsConfig config, BallCollisionWorld world,
			ShotPhysicsProfile shotProfile) {
		if (state.resting()) {
			return state;
		}

		Vec3 velocity = sanitizeNaN(state.velocity());
		Vec3 position = state.position();
		boolean grounded = state.grounded();

		int substeps = substepCount(velocity, config);
		double dt = 1.0 / substeps;

		for (int i = 0; i < substeps; i++) {
			if (grounded) {
				SurfaceDefinition surface = world.surfaceAt(position);
				double rollingRetention = surface.rollingFriction()
					* shotProfile.rollingFrictionMultiplier();
				velocity = velocity.scaleHorizontal(Math.pow(rollingRetention, dt));
			} else {
				velocity = velocity.add(new Vec3(0.0, -config.gravity() * dt, 0.0));
				velocity = velocity.scale(Math.pow(config.airDrag(), dt));
			}

			CollisionResult result = world.move(position, velocity.scale(dt));
			position = result.position();

			if (result.collided()) {
				Vec3 normal = result.normal().normalize();
				SurfaceDefinition surface = world.surfaceAt(position);
				Vec3 reflected = velocity.reflect(normal);
				double impactSpeed = -velocity.dot(normal);

				if (normal.y() > GROUND_NORMAL_Y && impactSpeed < config.bounceFloorSpeed()) {
					// Gentle landing: remove normal velocity and roll.
					grounded = true;
					velocity = landingTangent(reflected, normal, shotProfile);
				} else {
					grounded = false;
					double restitutionFactor = config.restitution() * surface.bounceMultiplier();
					Vec3 tangent = landingTangent(reflected, normal, shotProfile);
					Vec3 normalPart = normal.scale(reflected.along(normal) * restitutionFactor);
					velocity = tangent.add(normalPart);
				}
			} else if (grounded) {
				// We thought we were on the ground but this sweep missed it.
				// Probe straight down to see if support still exists (edge case).
				CollisionResult probe = world.move(position, new Vec3(0.0, -SUPPORT_PROBE_DELTA, 0.0));
				grounded = probe.collided() && probe.normal().y() > GROUND_NORMAL_Y;
			}
		}

		if (grounded && velocity.horizontalLength() < config.stopSpeed()) {
			return BallState.atRest(position);
		}

		return new BallState(position, velocity, grounded, false);
	}

	private static Vec3 landingTangent(
		Vec3 reflected, Vec3 normal, ShotPhysicsProfile shotProfile
	) {
		Vec3 tangent = reflected.tangent(normal);
		return normal.y() > GROUND_NORMAL_Y
			? tangent.scaleHorizontal(shotProfile.landingHorizontalRetention())
			: tangent;
	}

	/** Caps launch velocity at {@code maxLaunchSpeed} while preserving direction. */
	public static Vec3 clampLaunch(Vec3 velocity, PhysicsConfig config) {
		double len = velocity.length();
		if (len <= config.maxLaunchSpeed() || len == 0.0) {
			return velocity;
		}
		return velocity.scale(config.maxLaunchSpeed() / len);
	}

	/**
	 * Computes how many sub-steps this velocity needs given {@code maxStepDistance},
	 * clamped to {@code [1, maxSubsteps]}.
	 */
	public static int substepCount(Vec3 velocity, PhysicsConfig config) {
		int count = (int) Math.ceil(velocity.length() / config.maxStepDistance());
		if (count < 1) {
			return 1;
		}
		return Math.min(count, config.maxSubsteps());
	}

	private static Vec3 sanitizeNaN(Vec3 velocity) {
		double x = velocity.x();
		double y = velocity.y();
		double z = velocity.z();
		boolean dirty = false;
		if (Double.isNaN(x)) {
			x = 0.0;
			dirty = true;
		}
		if (Double.isNaN(y)) {
			y = 0.0;
			dirty = true;
		}
		if (Double.isNaN(z)) {
			z = 0.0;
			dirty = true;
		}
		return dirty ? new Vec3(x, y, z) : velocity;
	}
}
