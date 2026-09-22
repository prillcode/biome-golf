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
				payload.accuracy(),
				payload.shotType());

		if (outcome != ShotOutcome.SUCCESS) {
			player.sendSystemMessage(Component.literal(outcome.description()), true);
		}
		if (outcome == ShotOutcome.MISSING_ACTIVE_BALL) {
			ActiveHoleService.instance().notifyMissingBall(player);
		}
		// On success the client sees the live server-authoritative entity motion.
	}
}
