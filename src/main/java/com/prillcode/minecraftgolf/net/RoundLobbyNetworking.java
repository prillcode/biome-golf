package com.prillcode.minecraftgolf.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.server.ActiveHoleService;

/** Networking for the player-facing course browser and Ready Golf lobby. */
public final class RoundLobbyNetworking {
	private static boolean registered;
	private RoundLobbyNetworking() {}
	public static void register() {
		if (registered) return;
		registered = true;
		PayloadTypeRegistry.serverboundPlay().register(CourseListRequestPayload.TYPE, CourseListRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RoundLobbyActionPayload.TYPE, RoundLobbyActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CourseListPayload.TYPE, CourseListPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LobbyStatePayload.TYPE, LobbyStatePayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CourseListRequestPayload.TYPE, (payload, context) ->
			{
				MinecraftGolf.LOGGER.info("Received finalized-course list request from {}", context.player().getName().getString());
				ActiveHoleService.instance().sendFinalizedCourses(context.player());
			});
		ServerPlayNetworking.registerGlobalReceiver(RoundLobbyActionPayload.TYPE, RoundLobbyNetworking::onAction);
		MinecraftGolf.LOGGER.info("Registered Ready Golf lobby networking");
	}
	private static void onAction(RoundLobbyActionPayload payload, ServerPlayNetworking.Context context) {
		ServerPlayer player = context.player();
		ActiveHoleService service = ActiveHoleService.instance();
		ActiveHoleService.StartResult result = switch (payload.action()) {
			case CREATE -> service.createRound(payload.courseId(), player);
			case JOIN -> service.joinRound(payload.courseId(), player);
			case START -> service.startRound(player);
			case LEAVE -> service.leaveRound(player);
		};
		player.sendSystemMessage(Component.literal(result.message()), !result.success());
		if (result.success()) {
			MinecraftGolf.LOGGER.info("Accepted Golf Menu lobby action {} from {}", payload.action(),
				player.getName().getString());
		}
	}
}
