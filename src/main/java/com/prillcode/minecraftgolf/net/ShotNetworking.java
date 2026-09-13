package com.prillcode.minecraftgolf.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.server.ActiveHoleService;

/**
 * Wire-up for the M3 shot-request payload (ARCH §20 — typed custom payload;
 * the server owns the outcome).
 *
 * <p>Server-side only: registers the payload codec and installs a global
 * receiver that validates and executes via {@link ShotService}. No client code
 * (and no client entrypoint) is referenced here, so a dedicated server loads it
 * intact.</p>
 */
public final class ShotNetworking {

	private static boolean registered;

	private ShotNetworking() {
	}

	/**
	 * Registers the payload codec + server receiver. Called during mod startup;
	 * safe on a dedicated server or an integrated (singleplayer) server.
	 */
	public static void register() {
		if (registered) {
			return;
		}
		registered = true;

		// Serverbound (client -> server) direction only: a finalized shot request.
		PayloadTypeRegistry.serverboundPlay().register(
				ShotRequestPayload.TYPE, ShotRequestPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(
				ShotRequestPayload.TYPE, ShotNetworking::onShotRequest);

		MinecraftGolf.LOGGER.info("Registered M3 shot-request networking");
	}

	private static void onShotRequest(ShotRequestPayload payload,
			ServerPlayNetworking.Context context) {
		ServerPlayer player = context.player();
		ShotOutcome outcome = ShotService.attempt(
				player,
				payload.ballId(),
				payload.aimYawDeg(),
				payload.aimPitchDeg(),
				payload.power(),
				payload.accuracy());

		if (outcome != ShotOutcome.SUCCESS) {
			player.sendSystemMessage(Component.literal(describeFailure(outcome)), true);
		}
		if (outcome == ShotOutcome.MISSING_ACTIVE_BALL) {
			ActiveHoleService.instance().notifyMissingBall(player);
		}
		// On success the client sees the live server-authoritative entity motion.
	}

	private static String describeFailure(ShotOutcome o) {
		return switch (o) {
			case BALL_NOT_FOUND -> "[golf] that golf ball is not here";
			case NOT_YOUR_BALL -> "[golf] that golf ball belongs to another player";
			case BALL_MOVING -> "[golf] wait for the ball to stop";
			case BALL_TOO_FAR -> "[golf] walk closer to the ball before taking the next shot";
			case NO_CLUB -> "[golf] hold a golf club to take a shot";
			case AIM_NOT_LEGAL -> "[golf] aim level with the ground";
			case NOT_ACTIVE_BALL -> "[golf] use the ball assigned to the active hole attempt";
			case MISSING_ACTIVE_BALL -> "[golf] your assigned ball is missing; use /golf hole restart";
			case HOLE_COMPLETE -> "[golf] this hole is complete; use /golf hole restart, or /golf round restart after the round ends";
			case DRIVER_NOT_ALLOWED_ON_SAND -> "[golf] Driver cannot be used from sand; select another club";
			case SUCCESS, UNKNOWN -> "";
		};
	}
}
