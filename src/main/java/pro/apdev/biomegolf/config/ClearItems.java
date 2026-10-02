package pro.apdev.biomegolf.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Minecraft-free definition of the Builder bulk-clear target groups.
 *
 * <p>Each named group is a list of block IDs and block tags; a leading
 * {@code #} marks a block tag (e.g. {@code "#minecraft:logs"}). This record
 * owns parsing and structural validation only — deliberately free of any
 * Minecraft registry so it stays plain-Java testable. Registry resolution
 * happens in {@code pro.apdev.biomegolf.server.ClearItemsService}.</p>
 */
public record ClearItems(int schemaVersion, Map<String, List<String>> groups) {

	public static final int CURRENT_SCHEMA = 1;

	/** The built-in default groups; use when no config file exists or a file is invalid. */
	public static ClearItems defaults() {
		return new ClearItems(CURRENT_SCHEMA, DEFAULT_GROUPS);
	}

	/** Built-in groups used when no config file exists or a file is invalid. */
	public static final Map<String, List<String>> DEFAULT_GROUPS = Map.of(
		"trees", List.of("#minecraft:logs", "#minecraft:leaves"),
		"logs", List.of("#minecraft:logs"),
		"leaves", List.of("#minecraft:leaves"),
		"ground", List.of(
			"minecraft:grass_block",
			"minecraft:podzol",
			"minecraft:mycelium",
			"#minecraft:dirt",
			"#minecraft:sand",
			"#minecraft:terracotta",
			"#minecraft:stone_ore_replaceables",
			"#minecraft:deepslate_ore_replaceables",
			"#minecraft:coal_ores",
			"#minecraft:iron_ores",
			"#minecraft:copper_ores",
			"#minecraft:gold_ores",
			"#minecraft:redstone_ores",
			"#minecraft:emerald_ores",
			"#minecraft:lapis_ores",
			"#minecraft:diamond_ores"),
		"grass", List.of(
			"minecraft:short_grass",
			"minecraft:fern",
			"minecraft:tall_grass",
			"minecraft:large_fern"));

	public ClearItems {
		if (schemaVersion <= 0) {
			throw new IllegalArgumentException("schemaVersion must be positive: " + schemaVersion);
		}
		Objects.requireNonNull(groups, "groups");
		Map<String, List<String>> copy = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
			String name = Objects.requireNonNull(entry.getKey(), "group names must not be null");
			if (name.isBlank()) {
				throw new IllegalArgumentException("group names must not be blank");
			}
			List<String> entries = Objects.requireNonNull(entry.getValue(), "group entries must not be null");
			if (entries.isEmpty()) {
				throw new IllegalArgumentException("group '" + name + "' must contain at least one block or tag");
			}
			for (String value : entries) {
				Objects.requireNonNull(value, "group entries must not be null");
				if (value.isBlank()) {
					throw new IllegalArgumentException("group '" + name + "' contains a blank entry");
				}
			}
			copy.put(name, List.copyOf(entries));
		}
		groups = Map.copyOf(copy);
	}

	/** Parses and validates the JSON config; structural problems throw {@link IllegalArgumentException}. */
	public static ClearItems parse(String json) {
		JsonElement element = JsonParser.parseString(json);
		if (!element.isJsonObject()) {
			throw new IllegalArgumentException("clear items root must be a JSON object");
		}
		JsonObject root = element.getAsJsonObject();
		int schema = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : CURRENT_SCHEMA;
		if (!root.has("items") || !root.get("items").isJsonObject()) {
			throw new IllegalArgumentException("clear items must contain an 'items' object");
		}
		Map<String, List<String>> groups = new LinkedHashMap<>();
		JsonObject items = root.getAsJsonObject("items");
		for (String name : items.keySet()) {
			JsonElement value = items.get(name);
			if (!value.isJsonArray()) {
				throw new IllegalArgumentException("clear group '" + name + "' must be an array of block ids/tags");
			}
			List<String> entries = new java.util.ArrayList<>();
			for (JsonElement entry : value.getAsJsonArray()) {
				if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
					throw new IllegalArgumentException("clear group '" + name + "' entries must be strings");
				}
				entries.add(entry.getAsString());
			}
			groups.put(name, entries);
		}
		return new ClearItems(schema, groups);
	}

	/** Serializes groups to the JSON shape used by {@code ClearItemsService}. */
	public static String toJson(ClearItems items) {
		JsonObject root = new JsonObject();
		root.addProperty("schemaVersion", items.schemaVersion());
		JsonObject groups = new JsonObject();
		for (Map.Entry<String, List<String>> entry : items.groups().entrySet()) {
			JsonArray array = new JsonArray();
			for (String value : entry.getValue()) {
				array.add(value);
			}
			groups.add(entry.getKey(), array);
		}
		root.add("items", groups);
		return new GsonBuilder().setPrettyPrinting().create().toJson(root);
	}
}