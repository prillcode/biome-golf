package com.prillcode.minecraftgolf.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.server.ActiveHoleService;
import com.prillcode.minecraftgolf.server.ActiveHoleService.StartResult;

/** Serverbound wiring for the M7 player-initiated next-hole HUD action. */
public final class NextHoleNetworking {

	private static boolean registered;

	private NextHoleNetworking() {
	}

	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		PayloadTypeRegistry.serverboundPlay().register(
			NextHoleRequestPayload.TYPE, NextHoleRequestPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(
			NextHoleRequestPayload.TYPE, NextHoleNetworking::onRequest);
		MinecraftGolf.LOGGER.info("Registered M7 next-hole HUD action networking");
	}

	private static void onRequest(NextHoleRequestPayload payload,
			ServerPlayNetworking.Context context) {
		StartResult result = ActiveHoleService.instance().nextHole(context.player());
		context.player().sendSystemMessage(Component.literal(result.message()), !result.success());
	}
}
