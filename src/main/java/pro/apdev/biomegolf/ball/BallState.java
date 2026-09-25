package pro.apdev.biomegolf.ball;

import java.util.Objects;

import pro.apdev.biomegolf.golf.Vec3;

/**
 * Server-authoritative kinematic state of one golf ball (ARCHITECTURE.md §9.2).
 *
 * <p>Immutable: the physics step produces the next state; the owning entity
 * applies it. Gameplay state beyond kinematics (owner, strokes, round data)
 * deliberately lives outside the ball (§9.2).</p>
 *
 * @param position ball center, blocks
 * @param velocity blocks/tick
 * @param grounded true while rolling/in contact with ground below
 * @param resting  true when the ball has stopped; stays true until relaunched
 */
public record BallState(Vec3 position, Vec3 velocity, boolean grounded, boolean resting) {

	public BallState {
		Objects.requireNonNull(position, "position");
		Objects.requireNonNull(velocity, "velocity");
	}

	/** A ball sitting still at the given position. */
	public static BallState atRest(Vec3 position) {
		return new BallState(position, Vec3.ZERO, true, true);
	}

	/** A freshly launched, airborne ball. */
	public static BallState launched(Vec3 position, Vec3 velocity) {
		return new BallState(position, velocity, false, false);
	}

	@Override
	public String toString() {
		return "BallState[pos=" + position + ", vel=" + velocity
				+ (grounded ? ", grounded" : "") + (resting ? ", resting" : "") + "]";
	}
}
