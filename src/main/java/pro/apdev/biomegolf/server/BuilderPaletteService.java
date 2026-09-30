package pro.apdev.biomegolf.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.club.ClubDefinition;
import pro.apdev.biomegolf.club.GolfClubs;
import pro.apdev.biomegolf.config.BuilderPalette;
import pro.apdev.biomegolf.item.GolfClubItem;
import pro.apdev.biomegolf.item.GolfItems;

/**
 * M8.15: server-side owner of the curated Course Builder material palette.
 *
 * <p>Reads {@code config/minecraft_golf/builder_palette.json} (a server-global
 * Fabric config, not per-world). The whole file is validated as a replacement
 * before it is adopted: a malformed file, an unknown item id, a non-placeable
 * item, or an explicitly unsafe block keeps the last valid palette. The default
 * file is generated on first use. Palette membership grants material access, not
 * arbitrary item components; loadout stacks are built fresh from registered
 * items.</p>
 */
public final class BuilderPaletteService {

	private static final String DIRECTORY = "minecraft_golf";
	private static final String FILE_NAME = "builder_palette.json";
	/** One item per palette slot; inventory replenishes consumed items to this depth. */
	private static final int LOADOUT_COUNT = 1;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final BuilderPaletteService INSTANCE = new BuilderPaletteService();

	private List<Item> items = List.of();
	private Path configPath;

	private BuilderPaletteService() {
	}

	public static BuilderPaletteService instance() {
		return INSTANCE;
	}

	/** Loads the palette at mod initialization; installs the reload path used by the command. */
	public static void register() {
		INSTANCE.configPath = FabricLoader.getInstance().getConfigDir()
			.resolve(DIRECTORY).resolve(FILE_NAME);
		INSTANCE.loadInitial();
		MinecraftGolf.LOGGER.info("Registered builder palette ({} entries from {})",
			INSTANCE.items.size(), INSTANCE.configPath);
	}

	/** The active palette items in order. */
	public List<Item> items() {
		return items;
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	/** Whether a held stack is a currently-supplied Builder palette item. */
	public boolean isAllowed(ItemStack stack) {
		return stack != null && !stack.isEmpty() && isAllowed(stack.getItem());
	}

	public boolean isAllowed(Item item) {
		return items.contains(item) || item instanceof GolfClubItem;
	}

	/** The Builder's play-test club set: everything except the putter. */
	private static final List<ClubDefinition> BUILDER_CLUBS = GolfClubs.ALL.stream()
		.filter(club -> club != GolfClubs.PUTTER)
		.toList();

	/**
	 * Builds a fresh per-builder loadout: the play-test club set first (hotbar 0-5),
	 * then one-of-each palette stacks. Suppliers never share ItemStack instances, and
	 * 30 palette items + 6 clubs exactly fill the 36-slot main inventory.
	 */
	public List<ItemStack> loadout() {
		List<ItemStack> stacks = new ArrayList<>(items.size() + BUILDER_CLUBS.size());
		for (ClubDefinition club : BUILDER_CLUBS) {
			stacks.add(GolfItems.customStack(club));
		}
		for (Item item : items) {
			stacks.add(new ItemStack(item, LOADOUT_COUNT));
		}
		return stacks;
	}

	/**
	 * Replenishes a Builder's placed-once items toward a full loadout. Every palette
	 * stack already in the inventory is topped back up to one, and missing palette
	 * items are re-added; removed or non-palette items are never re-added, so a
	 * palette reload takes effect within a second.
	 *
	 * @param inventory the Builder's non-equipment inventory (the same list
	 *                  {@code giveLoadout} fills)
	 * @return the new stacks that could not be placed (never empty in a normal
	 *         Builder inventory, which has room for one of each palette item)
	 */
	public List<ItemStack> restock(java.util.List<ItemStack> inventory) {
		List<ItemStack> missing = new ArrayList<>(items.size());
		java.util.Map<Item, ItemStack> existing = new java.util.HashMap<>(items.size());
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && items.contains(stack.getItem())) {
				if (stack.getCount() < LOADOUT_COUNT) {
					stack.setCount(LOADOUT_COUNT);
				}
				existing.put(stack.getItem(), stack);
			}
		}
		for (Item item : items) {
			if (!existing.containsKey(item)) {
				missing.add(new ItemStack(item, LOADOUT_COUNT));
			}
		}
		return missing;
	}

	/** Human-readable summary for {@code /golf mode status} and command feedback. */
	public String describe() {
		if (items.isEmpty()) {
			return "empty";
		}
		StringBuilder builder = new StringBuilder();
		for (Item item : items) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(BuiltInRegistries.ITEM.getKey(item));
		}
		return items.size() + " item(s): " + builder;
	}

	/**
	 * Operator command: reloads the palette from disk. A valid file replaces the
	 * active palette; any validation failure retains the previous palette and
	 * reports the reason.
	 */
	public ReloadResult reload() {
		BuilderPalette parsed;
		try {
			parsed = readPalette();
		} catch (IOException | JsonSyntaxException | IllegalArgumentException exception) {
			MinecraftGolf.LOGGER.error("Builder palette reload failed; keeping the previous palette", exception);
			return new ReloadResult(false, "reload failed: " + exception.getMessage());
		}
		List<Item> resolved = new ArrayList<>(parsed.items().size());
		for (String id : parsed.items()) {
			Item item = resolve(id);
			if (item == null) {
				MinecraftGolf.LOGGER.error("Builder palette reload failed: unknown item '{}'", id);
				return new ReloadResult(false, "reload failed: unknown item '" + id + "'");
			}
			if (isUnsafe(item)) {
				MinecraftGolf.LOGGER.error("Builder palette reload failed: unsafe or non-placeable item '{}'", id);
				return new ReloadResult(false, "reload failed: unsafe or non-placeable item '" + id + "'");
			}
			resolved.add(item);
		}
		items = List.copyOf(resolved);
		MinecraftGolf.LOGGER.info("Builder palette reloaded: {}", describe());
		return new ReloadResult(true, "palette reloaded: " + describe());
	}

	private void loadInitial() {
		if (configPath == null) {
			items = resolveAll(BuilderPalette.defaults());
			return;
		}
		if (!Files.exists(configPath)) {
			items = resolveAll(BuilderPalette.defaults());
			writeDefault();
			return;
		}
		try {
			BuilderPalette parsed = readPalette();
			List<Item> resolved = resolveAll(parsed);
			if (resolved == null) {
				items = resolveAllStrictDefaults();
				return;
			}
			items = resolved;
		} catch (IOException | JsonSyntaxException | IllegalArgumentException exception) {
			MinecraftGolf.LOGGER.error("Invalid builder palette at {}; using defaults and leaving the file untouched",
				configPath, exception);
			items = resolveAllStrictDefaults();
		}
	}

	private BuilderPalette readPalette() throws IOException {
		String text = Files.readString(configPath);
		JsonElement element = JsonParser.parseString(text);
		if (!element.isJsonObject()) {
			throw new IllegalArgumentException("builder palette root must be a JSON object");
		}
		JsonObject root = element.getAsJsonObject();
		int schema = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : BuilderPalette.CURRENT_SCHEMA;
		if (!root.has("items") || !root.get("items").isJsonArray()) {
			throw new IllegalArgumentException("builder palette must contain an 'items' array");
		}
		JsonArray array = root.getAsJsonArray("items");
		List<String> ids = new ArrayList<>(array.size());
		for (JsonElement item : array) {
			if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
				throw new IllegalArgumentException("builder palette items must be strings");
			}
			ids.add(item.getAsString());
		}
		return new BuilderPalette(schema, ids);
	}

	private void writeDefault() {
		JsonObject root = new JsonObject();
		root.addProperty("schemaVersion", BuilderPalette.CURRENT_SCHEMA);
		JsonArray array = new JsonArray();
		for (String id : BuilderPalette.DEFAULT_ITEMS) {
			array.add(id);
		}
		root.add("items", array);
		try {
			Files.createDirectories(configPath.getParent());
			Files.writeString(configPath, GSON.toJson(root));
			MinecraftGolf.LOGGER.info("Generated default builder palette at {}", configPath);
		} catch (IOException exception) {
			MinecraftGolf.LOGGER.error("Failed to write default builder palette to {}", configPath, exception);
		}
	}

	private static List<Item> resolveAll(BuilderPalette palette) {
		List<Item> resolved = new ArrayList<>(palette.items().size());
		for (String id : palette.items()) {
			Item item = resolve(id);
			if (item == null || isUnsafe(item)) {
				return null;
			}
			resolved.add(item);
		}
		return List.copyOf(resolved);
	}

	private static List<Item> resolveAllStrictDefaults() {
		List<Item> resolved = resolveAll(BuilderPalette.defaults());
		if (resolved == null) {
			throw new IllegalStateException("default builder palette is invalid");
		}
		return resolved;
	}

	private static Item resolve(String id) {
		Identifier identifier;
		try {
			identifier = Identifier.parse(id);
		} catch (RuntimeException exception) {
			return null;
		}
		return BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
	}

	/**
	 * Rejects anything that is not a placeable block plus the small set of
	 * deliberately unsafe block types; spawn eggs, buckets, and other non-block
	 * items fail the {@link BlockItem} requirement.
	 */
	private static boolean isUnsafe(Item item) {
		if (!(item instanceof BlockItem blockItem)) {
			return true;
		}
		Block block = blockItem.getBlock();
		return block instanceof BedBlock
			|| block == Blocks.TNT
			|| block == Blocks.RESPAWN_ANCHOR
			|| block == Blocks.SPAWNER
			|| block == Blocks.END_PORTAL_FRAME;
	}

	public record ReloadResult(boolean success, String message) {
	}
}
