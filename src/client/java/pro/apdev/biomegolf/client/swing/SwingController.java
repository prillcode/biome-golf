package pro.apdev.biomegolf.client.swing;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import pro.apdev.biomegolf.client.camera.PostShotCamera;
import pro.apdev.biomegolf.client.DistanceDisplayState;
import pro.apdev.biomegolf.club.ClubDefinition;
import pro.apdev.biomegolf.club.ShotType;
import pro.apdev.biomegolf.entity.GolfBallEntity;
import pro.apdev.biomegolf.item.GolfItems;
import pro.apdev.biomegolf.net.ShotRequestPayload;

/** Client-local three-click swing state. The server receives only final intent. */
public final class SwingController {

	private static final double MAX_STRIKE_DISTANCE_SQ = 6.0 * 6.0;
	private static final int POWER_HALF_SWEEP_TICKS = 24; // 1.2 seconds, 0 -> 100%
	private static final int ACCURACY_HALF_SWEEP_TICKS = 15;
	private static final int SENT_DISPLAY_TICKS = 30;
	private static final int NOTICE_DISPLAY_TICKS = 40;

	public enum Phase {
		IDLE,
		POWER,
		ACCURACY,
		SENT
	}

	private final PostShotCamera postShotCamera;

	private Phase phase = Phase.IDLE;
	private GolfBallEntity targetBall;
	private ClubDefinition club;
	private int phaseTicks;
	private float lockedPower;
	private float lockedAccuracy = 0.5f;
	private String notice = "";
	private ShotType shotType = ShotType.STANDARD;
	private int noticeTicks;

	public SwingController(PostShotCamera postShotCamera) {
		this.postShotCamera = postShotCamera;
	}

	public void tick(Minecraft client) {
		if (client.player == null || client.level == null) {
			reset();
			return;
		}
		if (noticeTicks > 0) {
			noticeTicks--;
		}

		ClubDefinition heldClub = heldClub(client.player);
		if (heldClub == null) {
			reset();
			return;
		}

		if (phase == Phase.SENT) {
			if (++phaseTicks >= SENT_DISPLAY_TICKS) {
				resetSwing();
			}
			return;
		}

		if (phase != Phase.IDLE && !targetStillLegal(client.player, heldClub)) {
			cancel("Swing cancelled: ball or club changed");
			return;
		}

		if (phase == Phase.POWER || phase == Phase.ACCURACY) {
			phaseTicks++;
		}
	}

	/** Called from the client-only Fabric use callback for each real use action. */
	public void click(Minecraft client, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND
				|| client.player == null
				|| client.level == null
				|| !client.mouseHandler.isMouseGrabbed()) {
			return;
		}
		ClubDefinition heldClub = heldClub(client.player);
		if (heldClub == null) {
			return;
		}

		switch (phase) {
			case IDLE -> arm(client, heldClub);
			case POWER -> {
				if (!targetStillLegal(client.player, heldClub)) {
					cancel("Swing cancelled: ball or club changed");
					return;
				}
				lockedPower = meterValue(phaseTicks, POWER_HALF_SWEEP_TICKS);
				phase = Phase.ACCURACY;
				phaseTicks = 0;
			}
			case ACCURACY -> {
				if (!targetStillLegal(client.player, heldClub)) {
					cancel("Swing cancelled: ball or club changed");
					return;
				}
				sendShot(client);
			}
			case SENT -> { }
		}
	}

	private void arm(Minecraft client, ClubDefinition heldClub) {
		GolfBallEntity ball = findStrikeableBall(client.player);
		if (ball == null) {
			showNotice("No resting golf ball in range");
			return;
		}
		targetBall = ball;
		club = heldClub;
		phase = Phase.POWER;
		phaseTicks = 0;
		lockedPower = 0.0f;
		lockedAccuracy = 0.5f;
		notice = "";
		noticeTicks = 0;
	}

	private void sendShot(Minecraft client) {
		lockedAccuracy = meterValue(phaseTicks, ACCURACY_HALF_SWEEP_TICKS);
		ShotRequestPayload payload = new ShotRequestPayload(
				targetBall.getId(),
				client.player.getYRot(),
				client.player.getXRot(),
				lockedPower,
				lockedAccuracy,
				shotType);
		if (!ClientPlayNetworking.canSend(ShotRequestPayload.TYPE)) {
			cancel("Server cannot receive golf shots");
			return;
		}
		ClientPlayNetworking.send(payload);
		postShotCamera.follow(client, targetBall);
		phase = Phase.SENT;
		phaseTicks = 0;
	}

	private boolean targetStillLegal(LocalPlayer player, ClubDefinition heldClub) {
		return targetBall != null
				&& !targetBall.isRemoved()
				&& targetBall.isResting()
				&& targetBall.canBeStruckBy(player.getUUID())
				&& player.distanceToSqr(targetBall) <= MAX_STRIKE_DISTANCE_SQ
				&& club == heldClub;
	}

	private static GolfBallEntity findStrikeableBall(LocalPlayer player) {
		UUID playerId = player.getUUID();
		AABB search = player.getBoundingBox().inflate(6.0);
		List<GolfBallEntity> balls = player.level().getEntities(
				EntityTypeTest.forClass(GolfBallEntity.class),
				search,
				ball -> ball.isResting() && ball.canBeStruckBy(playerId));

		GolfBallEntity owned = null;
		GolfBallEntity unowned = null;
		double ownedDistance = Double.MAX_VALUE;
		double unownedDistance = Double.MAX_VALUE;
		for (GolfBallEntity ball : balls) {
			double distance = player.distanceToSqr(ball);
			if (distance > MAX_STRIKE_DISTANCE_SQ) {
				continue;
			}
			if (playerId.equals(ball.owner()) && distance < ownedDistance) {
				owned = ball;
				ownedDistance = distance;
			} else if (ball.owner() == null && distance < unownedDistance) {
				unowned = ball;
				unownedDistance = distance;
			}
		}
		return owned != null ? owned : unowned;
	}

	private static ClubDefinition heldClub(LocalPlayer player) {
		// Recognises the custom item and the M10 vanilla fallback representation.
		return GolfItems.clubOf(player.getMainHandItem());
	}

	private static float meterValue(int ticks, int halfSweepTicks) {
		int period = halfSweepTicks * 2;
		int within = Math.floorMod(ticks, period);
		float rising = (float) within / halfSweepTicks;
		return rising <= 1.0f ? rising : 2.0f - rising;
	}

	private void cancel(String message) {
		resetSwing();
		showNotice(message);
	}

	private void showNotice(String message) {
		notice = message;
		noticeTicks = NOTICE_DISPLAY_TICKS;
	}

	private void reset() {
		resetSwing();
		notice = "";
		noticeTicks = 0;
	}

	private void resetSwing() {
		phase = Phase.IDLE;
		targetBall = null;
		club = null;
		shotType = ShotType.STANDARD;
		phaseTicks = 0;
	}

	/** Cycles legal choices; the server still validates the requested type. */
	public void cycleShotType(Minecraft client) {
		if (client.player == null) return;
		ClubDefinition held = heldClub(client.player);
		if (held == null) return;
		List<ShotType> choices = new java.util.ArrayList<>();
		choices.add(ShotType.STANDARD);
		choices.addAll(ShotType.choicesFor(held));
		shotType = choices.get((choices.indexOf(shotType) + 1) % choices.size());
		showNotice("Shot: " + shotType.displayName());
	}

	public String shotTypeText(LocalPlayer player) {
		ClubDefinition held = heldClub(player);
		if (held == null) return "";
		return "Shot: " + (shotType.allowedFor(held)
			? shotType.displayName() : ShotType.STANDARD.displayName());
	}

	public Phase phase() {
		return phase;
	}

	public float power() {
		return phase == Phase.POWER
				? meterValue(phaseTicks, POWER_HALF_SWEEP_TICKS)
				: lockedPower;
	}

	public float accuracy() {
		return phase == Phase.ACCURACY
				? meterValue(phaseTicks, ACCURACY_HALF_SWEEP_TICKS)
				: lockedAccuracy;
	}

	public String clubName(LocalPlayer player) {
		ClubDefinition held = heldClub(player);
		return held == null
				? ""
				: String.format(Locale.ROOT, "%s  %.0f°",
						held.displayName(), held.displayLoftDegrees());
	}

	public String distanceText(LocalPlayer player) {
		ClubDefinition held = heldClub(player);
		return held == null
				? ""
				: "~" + DistanceDisplayState.format(held.nominalCarry())
						+ (held.putting() ? " roll" : "");
	}

	public String stateText() {
		if (postShotCamera.isFollowing()) {
			return "Following - sneak to return";
		}
		if (noticeTicks > 0) {
			return notice;
		}
		return switch (phase) {
			case IDLE -> "Right-click: start swing";
			case POWER -> "Right-click: lock power";
			case ACCURACY -> "Right-click: strike";
			case SENT -> "Shot sent";
		};
	}
}
