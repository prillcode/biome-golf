package pro.apdev.biomegolf.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.golf.Vec3;

/** World-scoped configuration for the operator-built practice range. */
public final class PracticeRangeService {

	private static final String FILE_NAME = "minecraft_golf_practice_range.json";
	public static final int MAX_TARGETS = 8;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final PracticeRangeService INSTANCE = new PracticeRangeService();

	private final Map<Integer, Location> targets = new LinkedHashMap<>();
	private Location tee;
	private Path savePath;

	private PracticeRangeService() {
	}

	public static PracticeRangeService instance() {
		return INSTANCE;
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(instance()::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> instance().onServerStopping());
		MinecraftGolf.LOGGER.info("Registered practice range service (world JSON persistence at data/{})", FILE_NAME);
	}

	private void onServerStarted(MinecraftServer server) {
		savePath = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE_NAME);
		load();
	}

	private void onServerStopping() {
		tee = null;
		targets.clear();
		savePath = null;
	}

	public Location tee() {
		return tee;
	}

	public Location target(int number) {
		return targets.get(number);
	}

	public Map<Integer, Location> targets() {
		return Map.copyOf(targets);
	}

	public void setTee(String dimension, Vec3 position) {
		tee = new Location(dimension, position);
		save();
	}

	public void setTarget(int number, String dimension, Vec3 position) {
		if (!isValidTargetNumber(number)) {
			throw new IllegalArgumentException("practice target must be between 1 and " + MAX_TARGETS);
		}
		targets.put(number, new Location(dimension, position));
		save();
	}

	/** Target numbers are bounded to 1..{@link #MAX_TARGETS}; shared with the command tree. */
	public static boolean isValidTargetNumber(int number) {
		return number >= 1 && number <= MAX_TARGETS;
	}

	public void clearTarget(int number) {
		targets.remove(number);
		save();
	}

	private void load() {
		tee = null;
		targets.clear();
		if (savePath == null || !Files.exists(savePath)) return;
		try {
			JsonObject root = JsonParser.parseReader(Files.newBufferedReader(savePath)).getAsJsonObject();
			tee = optionalLocation(root, "tee");
			JsonObject targetObject = root.has("targets") && root.get("targets").isJsonObject()
				? root.getAsJsonObject("targets") : new JsonObject();
			for (Map.Entry<String, JsonElement> entry : targetObject.entrySet()) {
				try {
					int number = Integer.parseInt(entry.getKey());
					if (isValidTargetNumber(number) && entry.getValue().isJsonObject()) {
						targets.put(number, locationFromJson(entry.getValue().getAsJsonObject()));
					}
				} catch (RuntimeException ignored) {
					MinecraftGolf.LOGGER.warn("Ignoring invalid practice target {} in {}", entry.getKey(), savePath);
				}
			}
		} catch (IOException | RuntimeException exception) {
			MinecraftGolf.LOGGER.error("Failed to load practice range from {}; starting empty", savePath, exception);
		}
	}

	private void save() {
		if (savePath == null) return;
		try {
			Files.createDirectories(savePath.getParent());
			JsonObject root = new JsonObject();
			if (tee != null) root.add("tee", locationToJson(tee));
			JsonObject targetObject = new JsonObject();
			for (Map.Entry<Integer, Location> entry : targets.entrySet()) {
				targetObject.add(String.valueOf(entry.getKey()), locationToJson(entry.getValue()));
			}
			root.add("targets", targetObject);
			Files.writeString(savePath, GSON.toJson(root));
		} catch (IOException exception) {
			MinecraftGolf.LOGGER.error("Failed to save practice range to {}", savePath, exception);
		}
	}

	private static JsonObject locationToJson(Location location) {
		JsonObject json = new JsonObject();
		json.addProperty("dimension", location.dimension());
		JsonArray position = new JsonArray();
		position.add(location.position().x());
		position.add(location.position().y());
		position.add(location.position().z());
		json.add("position", position);
		return json;
	}

	private static Location optionalLocation(JsonObject root, String name) {
		return root.has(name) && root.get(name).isJsonObject()
			? locationFromJson(root.getAsJsonObject(name)) : null;
	}

	private static Location locationFromJson(JsonObject json) {
		String dimension = json.get("dimension").getAsString();
		JsonArray position = json.getAsJsonArray("position");
		if (position.size() != 3) throw new IllegalArgumentException("position must contain three values");
		return new Location(dimension, new Vec3(position.get(0).getAsDouble(), position.get(1).getAsDouble(),
			position.get(2).getAsDouble()));
	}

	public record Location(String dimension, Vec3 position) {
	}
}
