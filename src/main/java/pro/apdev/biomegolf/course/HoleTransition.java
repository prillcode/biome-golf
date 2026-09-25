package pro.apdev.biomegolf.course;

import java.util.Objects;

import pro.apdev.biomegolf.golf.Vec3;

/** Minecraft-free player placement metadata used when entering a hole. */
public record HoleTransition(Vec3 playerPosition, double yaw, double pitch) {

	public HoleTransition {
		Objects.requireNonNull(playerPosition, "playerPosition");
		if (!Double.isFinite(yaw) || !Double.isFinite(pitch)) {
			throw new IllegalArgumentException("transition rotation must be finite");
		}
		if (pitch < -90.0 || pitch > 90.0) {
			throw new IllegalArgumentException("transition pitch must be between -90 and 90 degrees");
		}
	}

	public static HoleTransition at(Vec3 playerPosition) {
		return new HoleTransition(playerPosition, 0.0, 0.0);
	}
}
