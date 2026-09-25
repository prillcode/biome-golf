package pro.apdev.biomegolf.client.camera;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.entity.GolfBallEntity;

/**
 * Simple client-only post-shot camera that trails the authoritative ball.
 * It never predicts or changes ball state and restores the player's previous
 * perspective on rest, rejection, removal, timeout, or manual sneak cancel.
 */
public final class PostShotCamera {

	private static final int LAUNCH_GRACE_TICKS = 40;
	private static final int MAX_FOLLOW_TICKS = 20 * 30;
	private static final double HEADING_EPSILON_SQ = 1.0E-6;
	private static final float VIEW_PITCH_DEGREES = 15.0F;

	private GolfBallEntity ball;
	private Entity previousCameraEntity;
	private CameraType previousCameraType;
	private Vec3 previousBallPosition;
	private int ticks;
	private boolean movementObserved;
	private boolean sneakWasDown;

	public void follow(Minecraft client, GolfBallEntity target) {
		if (client.player == null || target == null || target.isRemoved()) {
			return;
		}
		if (isFollowing()) {
			restore(client, "replaced by newer shot");
		}

		ball = target;
		previousCameraEntity = client.getCameraEntity();
		previousCameraType = client.options.getCameraType();
		previousBallPosition = target.position();
		ticks = 0;
		movementObserved = !target.isResting();
		sneakWasDown = client.options.keyShift.isDown();

		target.setYRot(client.player.getYRot());
		target.setXRot(VIEW_PITCH_DEGREES);
		client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		client.setCameraEntity(target);
		MinecraftGolf.LOGGER.info("Following golf ball {} with post-shot camera", target.getId());
	}

	public void tick(Minecraft client) {
		if (!isFollowing()) {
			return;
		}
		if (client.player == null || client.level == null) {
			int ballId = ball.getId();
			if (previousCameraType != null) {
				client.options.setCameraType(previousCameraType);
			}
			clear();
			MinecraftGolf.LOGGER.info(
					"Stopped following golf ball {}: client disconnected", ballId);
			return;
		}
		if (ball.isRemoved() || ball.level() != client.level) {
			restore(client, "ball unavailable");
			return;
		}

		boolean sneakDown = client.options.keyShift.isDown();
		boolean manualCancel = sneakDown && !sneakWasDown;
		sneakWasDown = sneakDown;
		if (manualCancel) {
			restore(client, "manual cancel");
			return;
		}

		ticks++;
		updateHeading();
		if (!ball.isResting()) {
			movementObserved = true;
		} else if (movementObserved) {
			restore(client, "ball at rest");
			return;
		} else if (ticks >= LAUNCH_GRACE_TICKS) {
			restore(client, "shot not launched");
			return;
		}

		if (ticks >= MAX_FOLLOW_TICKS) {
			restore(client, "follow timeout");
		}
	}

	private void updateHeading() {
		Vec3 current = ball.position();
		double dx = current.x - previousBallPosition.x;
		double dz = current.z - previousBallPosition.z;
		if (dx * dx + dz * dz > HEADING_EPSILON_SQ) {
			ball.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
		}
		ball.setXRot(VIEW_PITCH_DEGREES);
		previousBallPosition = current;
	}

	private void restore(Minecraft client, String reason) {
		int ballId = ball.getId();
		if (client.getCameraEntity() == ball) {
			Entity restoreEntity = previousCameraEntity;
			if (restoreEntity == null || restoreEntity.isRemoved()) {
				restoreEntity = client.player;
			}
			client.setCameraEntity(restoreEntity);
			if (previousCameraType != null) {
				client.options.setCameraType(previousCameraType);
			}
		}
		clear();
		MinecraftGolf.LOGGER.info("Stopped following golf ball {}: {}", ballId, reason);
	}

	private void clear() {
		ball = null;
		previousCameraEntity = null;
		previousCameraType = null;
		previousBallPosition = null;
		ticks = 0;
		movementObserved = false;
	}

	public boolean isFollowing() {
		return ball != null;
	}
}
