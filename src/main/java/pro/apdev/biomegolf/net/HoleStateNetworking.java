package pro.apdev.biomegolf.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import pro.apdev.biomegolf.MinecraftGolf;

/**
 * Registers the clientbound hole-state payload codec and provides the
 * server-side send helper (S03, ARCH §20).
 *
 * <p>No client classes are imported here; the client receiver is registered
 * separately in {@code MinecraftGolfClient}.</p>
 */
public final class HoleStateNetworking {

	private static boolean registered;

	private HoleStateNetworking() {
	}

	/**
	 * Registers the clientbound payload codec. Called during common mod startup;
	 * safe on a dedicated server or integrated server.
	 */
	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		PayloadTypeRegistry.clientboundPlay().register(
				HoleStatePayload.TYPE, HoleStatePayload.STREAM_CODEC);
		MinecraftGolf.LOGGER.info("Registered S03 hole-state clientbound networking");
	}

	/** Sends a hole-state snapshot to the given player. */
	public static void send(ServerPlayer player, HoleStatePayload payload) {
		if (!ServerPlayNetworking.canSend(player, HoleStatePayload.TYPE)) {
			return;
		}
		ServerPlayNetworking.send(player, payload);
		MinecraftGolf.LOGGER.debug("Sent hole-state snapshot to {} phase={} strokes={}",
				player.getName().getString(), payload.phase(), payload.strokes());
	}
}
