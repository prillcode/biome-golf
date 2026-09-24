package com.prillcode.minecraftgolf.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.VisitorText;

/**
 * Bedrock/vanilla visitor experience: any client that cannot receive the modded payloads
 * ({@link BallCameraService#isClientLight}) joins as a <em>visitor</em>. Golf is Java-only, so
 * visitors cannot golf; they join the world in survival/peaceful by default and can opt into
 * spectator mode. The "join on Java" promo is shown when they enter spectator mode, not
 * periodically.
 *
 * <p>Neither mode can golf. {@link Mode#SURVIVAL} is normal world play — building and breaking
 * are free outside authored course regions, which the course guard protects.
 * {@link Mode#SPECTATOR} flies, phases through blocks, and is invisible. The modded Java client
 * never reaches any of this.</p>
 *
 * <p>Config (Java address, optional mod link, optional viewpoint) persists to world JSON,
 * matching the practice-range pattern.</p>
 */
public final class VisitorService {

	/** View modes; neither can golf, but Survival is a normal world player. */
	public enum Mode {
		SPECTATOR,
		SURVIVAL
	}

	private static final String FILE_NAME = "minecraft_golf_visitor.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final VisitorService INSTANCE = new VisitorService();

	private final Map<UUID, Mode> modes = new HashMap<>();

	private String javaAddress = "";
	private String modLink = "";
	private Viewpoint viewpoint;
	private Path savePath;

	private VisitorService() {
	}

	public static VisitorService instance() {
		return INSTANCE;
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(INSTANCE::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> INSTANCE.onServerStopping());
		MinecraftGolf.LOGGER.info("Registered visitor service (world JSON at data/{})", FILE_NAME);
	}

	/** Any client-light (Bedrock via Geyser, or unmodified Java) is a non-golfing visitor. */
	public static boolean isVisitor(ServerPlayer player) {
		return BallCameraService.isClientLight(player);
	}

	/** Feedback when a visitor tries a golf command. */
	public static String playRejection() {
		return "[golf] golf is Java-only — join on Java to play golf (build and explore freely here)";
	}

	/** Puts a joining visitor into survival/peaceful and sends the one-time welcome. */
	public void onPlayerJoined(ServerPlayer player) {
		if (!isVisitor(player)) {
			return;
		}
		modes.put(player.getUUID(), Mode.SURVIVAL);
		applyMode(player, Mode.SURVIVAL);
		teleportToViewpoint(player);
		for (String line : VisitorText.welcome()) {
			player.sendSystemMessage(Component.literal(line));
		}
	}

	public void onPlayerDisconnected(ServerPlayer player) {
		modes.remove(player.getUUID());
	}

	public Mode mode(UUID playerId) {
		return modes.getOrDefault(playerId, Mode.SURVIVAL);
	}

	/**
	 * Switches a visitor between spectator and survival. Entering spectator shows the
	 * "join on Java" promo; entering survival reports the peaceful game mode and the
	 * off-course build reminder.
	 */
	public void setMode(ServerPlayer player, Mode mode) {
		if (!isVisitor(player)) {
			player.sendSystemMessage(Component.literal("[golf] spectator view is for Bedrock/vanilla clients"));
			return;
		}
		modes.put(player.getUUID(), mode);
		applyMode(player, mode);
		if (mode == Mode.SPECTATOR) {
			player.sendSystemMessage(Component.literal(
				"[golf] Spectator view: fly around and watch. /golf spectator leave to join the world."));
			for (String line : VisitorText.spectatorPromo(javaAddress, modLink)) {
				player.sendSystemMessage(Component.literal(line));
			}
		} else {
			player.sendSystemMessage(Component.literal(
				"[golf] Game mode now survival/peaceful. Leave golf course bounds to break/build."));
		}
	}

	private static void applyMode(ServerPlayer player, Mode mode) {
		player.setGameMode(mode == Mode.SPECTATOR ? GameType.SPECTATOR : GameType.SURVIVAL);
	}

	private void teleportToViewpoint(ServerPlayer player) {
		if (viewpoint == null || player.level().getServer() == null) {
			return;
		}
		try {
			ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION,
				Identifier.parse(viewpoint.dimension()));
			ServerLevel level = player.level().getServer().getLevel(key);
			if (level == null) {
				return;
			}
			player.teleportTo(level, viewpoint.position().x(), viewpoint.position().y(),
				viewpoint.position().z(), Set.of(), player.getYRot(), player.getXRot(), true);
		} catch (RuntimeException exception) {
			MinecraftGolf.LOGGER.warn("Invalid visitor viewpoint dimension '{}'", viewpoint.dimension());
		}
	}

	// ── Config accessors ─────────────────────────────────────────────────────

	public String javaAddress() {
		return javaAddress;
	}

	public String modLink() {
		return modLink;
	}

	public boolean hasViewpoint() {
		return viewpoint != null;
	}

	public Viewpoint viewpoint() {
		return viewpoint;
	}

	public void setJavaAddress(String value) {
		javaAddress = value == null ? "" : value.trim();
		save();
	}

	public void setModLink(String value) {
		modLink = value == null ? "" : value.trim();
		save();
	}

	public void setViewpoint(String dimension, Vec3 position) {
		viewpoint = new Viewpoint(dimension, position);
		save();
	}

	public void clearViewpoint() {
		viewpoint = null;
		save();
	}

	// ── Persistence ──────────────────────────────────────────────────────────

	private void onServerStarted(MinecraftServer server) {
		savePath = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE_NAME);
		load();
	}

	private void onServerStopping() {
		modes.clear();
		savePath = null;
	}

	private void load() {
		javaAddress = "";
		modLink = "";
		viewpoint = null;
		if (savePath == null || !Files.exists(savePath)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseReader(Files.newBufferedReader(savePath)).getAsJsonObject();
			if (root.has("javaAddress")) javaAddress = root.get("javaAddress").getAsString();
			if (root.has("modLink")) modLink = root.get("modLink").getAsString();
			if (root.has("viewpoint") && root.get("viewpoint").isJsonObject()) {
				JsonObject json = root.getAsJsonObject("viewpoint");
				JsonArray position = json.getAsJsonArray("position");
				if (position != null && position.size() == 3) {
					viewpoint = new Viewpoint(json.get("dimension").getAsString(),
						new Vec3(position.get(0).getAsDouble(), position.get(1).getAsDouble(),
							position.get(2).getAsDouble()));
				}
			}
		} catch (IOException | RuntimeException exception) {
			MinecraftGolf.LOGGER.error("Failed to load visitor config from {}; using defaults", savePath, exception);
		}
	}

	private void save() {
		if (savePath == null) {
			return;
		}
		try {
			Files.createDirectories(savePath.getParent());
			JsonObject root = new JsonObject();
			root.addProperty("javaAddress", javaAddress);
			root.addProperty("modLink", modLink);
			if (viewpoint != null) {
				JsonObject json = new JsonObject();
				json.addProperty("dimension", viewpoint.dimension());
				JsonArray position = new JsonArray();
				position.add(viewpoint.position().x());
				position.add(viewpoint.position().y());
				position.add(viewpoint.position().z());
				json.add("position", position);
				root.add("viewpoint", json);
			}
			Files.writeString(savePath, GSON.toJson(root));
		} catch (IOException exception) {
			MinecraftGolf.LOGGER.error("Failed to save visitor config to {}", savePath, exception);
		}
	}

	public record Viewpoint(String dimension, Vec3 position) {
	}
}
