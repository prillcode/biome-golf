package pro.apdev.biomegolf.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Server-side command transport only; visibility remains entirely client-local. */
public final class HudVisibilityNetworking {

	private static boolean registered;

	private HudVisibilityNetworking() {
	}

	public static void register() {
		if (registered) return;
		registered = true;
		PayloadTypeRegistry.clientboundPlay().register(ToggleHudPayload.TYPE, ToggleHudPayload.STREAM_CODEC);
	}

	/** Returns false for clients without the HUD toggle receiver; never sends unsupported payloads. */
	public static boolean toggle(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, ToggleHudPayload.TYPE)) return false;
		ServerPlayNetworking.send(player, ToggleHudPayload.INSTANCE);
		return true;
	}
}
