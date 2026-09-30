package pro.apdev.biomegolf.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * M8.15: Minecraft-free parsed form of {@code config/minecraft_golf/builder_palette.json}.
 *
 * <p>The palette is an operator-editable allowlist of item ids that an authorized
 * Builder may place. Parsing/validation here is deliberately free of any Minecraft
 * registry access so it can be unit-tested on a plain JVM; the server-side
 * {@code BuilderPaletteService} additionally verifies that each id resolves to a
 * registered, placeable, non-unsafe item.</p>
 *
 * <p>Unknown/malformed input is rejected by throwing, so the service can retain the
 * last valid palette instead of silently substituting air.</p>
 */
public record BuilderPalette(int schemaVersion, List<String> items) {

	public static final int CURRENT_SCHEMA = 1;
	// One inventory slot per palette item (36 is the vanilla main-inventory slot
	// count), so a one-of-each loadout always fits and stacks are replenished
	// to one item as they are used — Builder materials never run out or overflow.
	public static final int MAX_ITEMS = 36;

	/** Default course-building palette of 36 landscaping and marker blocks. */
	public static final List<String> DEFAULT_ITEMS = List.of(
		// Course markers and decorative details.
		"minecraft:lime_wool",
		"minecraft:green_wool",
		"minecraft:moss_carpet",
		"minecraft:pale_oak_fence",
		"minecraft:stone_button",
		"minecraft:pale_oak_button",
		// Ground, soil, and sand.
		"minecraft:grass_block",
		"minecraft:dirt",
		"minecraft:coarse_dirt",
		"minecraft:podzol",
		"minecraft:sand",
		"minecraft:red_sand",
		"minecraft:gravel",
		// Common natural stone.
		"minecraft:stone",
		"minecraft:cobblestone",
		"minecraft:mossy_cobblestone",
		"minecraft:granite",
		"minecraft:diorite",
		"minecraft:andesite",
		"minecraft:tuff",
		// Trees, leaves, and bushes.
		"minecraft:oak_log",
		"minecraft:pale_oak_log",
		"minecraft:oak_leaves",
		"minecraft:pale_oak_leaves",
		"minecraft:oak_sapling",
		"minecraft:pale_oak_sapling",
		"minecraft:azalea",
		"minecraft:flowering_azalea",
		// Ground cover and flowers.
		"minecraft:short_grass",
		"minecraft:fern",
		"minecraft:dead_bush",
		"minecraft:dandelion",
		"minecraft:poppy",
		"minecraft:blue_orchid",
		"minecraft:allium",
		"minecraft:oxeye_daisy");

	private static final Pattern ITEM_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	public BuilderPalette {
		if (schemaVersion != CURRENT_SCHEMA) {
			throw new IllegalArgumentException("unsupported builder palette schemaVersion " + schemaVersion
				+ " (expected " + CURRENT_SCHEMA + ")");
		}
		Objects.requireNonNull(items, "items");
		if (items.isEmpty()) {
			throw new IllegalArgumentException("builder palette must list at least one item");
		}
		if (items.size() > MAX_ITEMS) {
			throw new IllegalArgumentException("builder palette lists " + items.size()
				+ " items, more than the maximum of " + MAX_ITEMS);
		}
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		for (String item : items) {
			unique.add(requireItemId(item));
		}
		items = List.copyOf(unique);
	}

	public static BuilderPalette defaults() {
		return new BuilderPalette(CURRENT_SCHEMA, DEFAULT_ITEMS);
	}

	private static String requireItemId(String item) {
		if (item == null) {
			throw new IllegalArgumentException("builder palette contains a null item id");
		}
		String trimmed = item.trim();
		if (!ITEM_ID.matcher(trimmed).matches()) {
			throw new IllegalArgumentException("'" + item + "' is not a valid namespaced item id");
		}
		return trimmed;
	}
}
