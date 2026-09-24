package com.prillcode.minecraftgolf.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
 * ({@link BallCameraService#isClientLight}) joins as a <em>visitor</em> who can watch but not
 * play, with a "join on Java" invite. This turns the shelved Bedrock play mode into a
 * promotional surface rather than a degraded game.
 *
 * <p>Visitors pick one of two view modes: {@link Mode#SPECTATOR} (fly, phase through blocks,
 * invisible) and {@link Mode#SURVIVAL} (normal world play — building and breaking are free
 * outside authored course regions, which the course guard protects). Neither can golf. The
 * modded Java client never reaches any of this.</p>
 *
 * <p>Config (Java address, mod link, reminder interval, optional viewpoint) persists to world
 * JSON, matching the practice-range pattern.</p>
 */
public final class VisitorService {

	/** View modes; neither can golf, but Survival is a normal world player. */
	public enum Mode {
		SPECTATOR,
		SURVIVAL
	}

	private static final String FILE_NAME = "minecraft_golf_visitor.json";
	private static final int DEFAULT_REMINDER_SECONDS = 60;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final VisitorService INSTANCE = new VisitorService();

	private final Map<UUID, Mode> modes = new HashMap<>();
	private final Map<UUID, Integer> nextReminderTick = new HashMap<>();

	private String javaAddress = "";
	private String modLink = "";
	private int reminderSeconds = DEFAULT_REMINDER_SECONDS;
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
		ServerTickEvents.END_SERVER_TICK.register(INSTANCE::onServerTick);
		MinecraftGolf.LOGGER.info("Registered visitor service (world JSON at data/{})", FILE_NAME);
	}

	/** Any client-light (Bedrock via Geyser, or unmodified Java) is a non-playing visitor. */
	public static boolean isVisitor(ServerPlayer player) {
		return BallCameraService.isClientLight(player);
	}

	/** Feedback when a visitor tries a golf command. */
	public static String playRejection() {
		return "[golf] golf is Java-only — join on Java to play golf (build and explore freely here)";
	}

	/** Puts a joining client-light player into spectator mode and sends the visitor welcome. */
	public void onPlayerJoined(ServerPlayer player) {
		if (!isVisitor(player)) {
			return;
		}
		modes.put(player.getUUID(), Mode.SPECTATOR);
		applyMode(player, Mode.SPECTATOR);
		teleportToViewpoint(player);
		sendWelcome(player);
		scheduleReminder(player);
	}

	public void onPlayerDisconnected(ServerPlayer player) {
		modes.remove(player.getUUID());
		nextReminderTick.remove(player.getUUID());
	}

	public Mode mode(UUID playerId) {
		return modes.getOrDefault(playerId, Mode.SPECTATOR);
	}

	/** Switches a visitor between the two non-playing presentation modes. */
	public void setMode(ServerPlayer player, Mode mode) {
		if (!isVisitor(player)) {
			player.sendSystemMessage(Component.literal("[golf] visitor view is for Bedrock/vanilla clients"));
			return;
		}
		modes.put(player.getUUID(), mode);
		applyMode(player, mode);
		player.sendSystemMessage(Component.literal(mode == Mode.SPECTATOR
			? "[golf] Spectator view: fly around and watch. /golf spectator leave to join the world."
			: "[golf] Game mode now survival/peaceful. Leave golf course bounds to break/build."));
	}

	private static void applyMode(ServerPlayer player, Mode mode) {
		player.setGameMode(mode == Mode.SPECTATOR ? GameType.SPECTATOR : GameType.SURVIVAL);
	}

	private void onServerTick(MinecraftServer server) {
		if (nextReminderTick.isEmpty() || reminderSeconds <= 0) {
			return;
		}
		int tick = server.getTickCount();
		Iterator<Map.Entry<UUID, Integer>> iterator = nextReminderTick.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Integer> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				iterator.remove();
				continue;
			}
			if (tick < entry.getValue()) {
				continue;
			}
			player.sendSystemMessage(Component.literal(VisitorText.reminder(javaAddress)), true);
			entry.setValue(tick + reminderTicks());
		}
	}

	private void scheduleReminder(ServerPlayer player) {
		if (reminderSeconds <= 0) {
			return;
		}
		nextReminderTick.put(player.getUUID(),
			player.level().getServer().getTickCount() + reminderTicks());
	}

	private int reminderTicks() {
		return Math.max(1, reminderSeconds) * 20;
	}

	private void sendWelcome(ServerPlayer player) {
		for (String line : VisitorText.welcome(javaAddress, modLink)) {
			player.sendSystemMessage(Component.literal(line));
		}
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

	public int reminderSeconds() {
		return reminderSeconds;
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

	public void setReminderSeconds(int value) {
		reminderSeconds = Math.max(0, value);
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
		nextReminderTick.clear();
		savePath = null;
	}

	private void load() {
		javaAddress = "";
		modLink = "";
		reminderSeconds = DEFAULT_REMINDER_SECONDS;
		viewpoint = null;
		if (savePath == null || !Files.exists(savePath)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseReader(Files.newBufferedReader(savePath)).getAsJsonObject();
			if (root.has("javaAddress")) javaAddress = root.get("javaAddress").getAsString();
			if (root.has("modLink")) modLink = root.get("modLink").getAsString();
			if (root.has("reminderSeconds")) reminderSeconds = Math.max(0, root.get("reminderSeconds").getAsInt());
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
			root.addProperty("reminderSeconds", reminderSeconds);
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
