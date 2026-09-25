package pro.apdev.biomegolf.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.server.ActiveHoleService;

/** Server-side validation and dispatch for contextual Golf Menu actions. */
public final class GolfMenuNetworking {
	private static boolean registered;
	private GolfMenuNetworking() {}
	public static void register() {
		if (registered) return;
		registered = true;
		PayloadTypeRegistry.serverboundPlay().register(GolfMenuActionPayload.TYPE,
			GolfMenuActionPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(GolfMenuActionPayload.TYPE,
			GolfMenuNetworking::onAction);
		MinecraftGolf.LOGGER.info("Registered contextual Golf Menu networking");
	}
	private static void onAction(GolfMenuActionPayload payload, ServerPlayNetworking.Context context) {
		ActiveHoleService service = ActiveHoleService.instance();
		ActiveHoleService.StartResult result = switch (payload.action()) {
			case OPEN_COURSE_BROWSER -> service.openCourseBrowser(context.player());
			case START_ROUND -> service.startRound(context.player());
			case LEAVE_ROUND -> service.leaveRound(context.player());
			case REPLAY_HOLE -> service.restart(context.player());
			case REPLAY_ROUND -> service.restartRound(context.player());
		};
		if (!result.success()) context.player().sendSystemMessage(Component.literal(result.message()), true);
	}
}
