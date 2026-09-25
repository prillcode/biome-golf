package pro.apdev.biomegolf.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.entity.GolfBallEntity;
import pro.apdev.biomegolf.net.HoleStatePayload;

/**
 * M10.2 server-side ball-follow camera for client-light players.
 *
 * <p>Modded Java clients run their own {@code PostShotCamera} and must never be
 * touched. For everyone else (vanilla Java, Bedrock through Geyser) the server
 * attaches the shooter's camera to the ball with {@link ServerPlayer#setCamera}
 * and restores it when the ball rests or is removed.</p>
 *
 * <p>The camera target must be a <em>vanilla</em> entity: Geyser translates
 * Java's SetCamera packet and resolves the target id through its entity cache,
 * then calls {@code EntitySpectateHelper.stop} when the id is unknown. The custom
 * {@code minecraft_golf:golf_ball} entity is unknown to that cache, so we follow
 * the ball's vanilla item mirror instead (see {@link GolfBallEntity#cameraTarget()}).</p>
 */
public final class BallCameraService {

	private static final BallCameraService INSTANCE = new BallCameraService();

	/** Player UUID → ball entity id currently being followed. */
	private final Map<UUID, Integer> following = new HashMap<>();

	private BallCameraService() {
	}

	public static BallCameraService instance() {
		return INSTANCE;
	}

	/** True when the player's client cannot run the modded follow camera itself. */
	public static boolean isClientLight(ServerPlayer player) {
		return !ServerPlayNetworking.canSend(player, HoleStatePayload.TYPE);
	}

	/** Attaches a client-light shooter's camera to the launched ball. */
	public void followShot(ServerPlayer player, GolfBallEntity ball) {
		if (!isClientLight(player)) {
			return;
		}
		Entity target = ball.cameraTarget();
		if (target == null || target == player) {
			return;
		}
		player.setCamera(target);
		following.put(player.getUUID(), ball.getId());
		MinecraftGolf.LOGGER.info("Following golf ball {} with server camera for client-light player {}",
			ball.getId(), player.getName().getString());
	}

	/** Restores the camera of whoever owns a ball that just came to rest. */
	public void onBallRest(GolfBallEntity ball) {
		UUID owner = ball.owner();
		if (owner == null) {
			return;
		}
		MinecraftServer server = ball.level().getServer();
		if (server == null) {
			return;
		}
		ServerPlayer player = server.getPlayerList().getPlayer(owner);
		if (player != null) {
			restore(player);
		}
	}

	/** Restores normal control if the player is currently following a ball. */
	public void restore(ServerPlayer player) {
		if (player == null || following.remove(player.getUUID()) == null) {
			return;
		}
		if (player.getCamera() != player) {
			player.setCamera(player);
		}
		MinecraftGolf.LOGGER.info("Restored server camera for client-light player {}",
			player.getName().getString());
	}

	/** Forgets any follow state for a player (disconnect cleanup). */
	public void forget(UUID playerId) {
		following.remove(playerId);
	}
}
