package pro.apdev.biomegolf.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.server.ActiveHoleService;
import pro.apdev.biomegolf.server.ClientLightPresentation;

/** Networking for the final scorecard and its server-authoritative actions. */
public final class RoundScorecardNetworking {
	private static boolean registered;

	private RoundScorecardNetworking() {}

	public static void register() {
		if (registered) return;
		registered = true;
		PayloadTypeRegistry.clientboundPlay().register(RoundScorecardPayload.TYPE,
			RoundScorecardPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RoundActionPayload.TYPE,
			RoundActionPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RoundActionPayload.TYPE,
			RoundScorecardNetworking::onAction);
		MinecraftGolf.LOGGER.info("Registered final scorecard networking");
	}

	public static void send(ServerPlayer player, RoundScorecardPayload payload) {
		if (ServerPlayNetworking.canSend(player, RoundScorecardPayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
			MinecraftGolf.LOGGER.info("Sent final scorecard to {} with {} player row(s)",
				player.getName().getString(), payload.players().size());
			return;
		}
		// M10.3 S3: client-light clients cannot open the scorecard screen; mirror the same
		// authoritative payload into chat so they still receive their result.
		if (ClientLightPresentation.isClientLight(player)) {
			ClientLightPresentation.sendScorecard(player, payload);
			MinecraftGolf.LOGGER.info("Sent final scorecard chat fallback to {} with {} player row(s)",
				player.getName().getString(), payload.players().size());
			return;
		}
		MinecraftGolf.LOGGER.warn("Could not send final scorecard to {}; client does not advertise {}",
			player.getName().getString(), RoundScorecardPayload.TYPE.id());
	}

	private static void onAction(RoundActionPayload payload, ServerPlayNetworking.Context context) {
		ServerPlayer player = context.player();
		ActiveHoleService service = ActiveHoleService.instance();
		ActiveHoleService.StartResult result = payload.action() == RoundActionPayload.Action.REPLAY
			? service.restartRound(player) : service.leaveRound(player);
		player.sendSystemMessage(Component.literal(result.message()), !result.success());
	}
}
