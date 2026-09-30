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
	public static final int MAX_ITEMS = 64;

	/** The documented default generated on first use (lime/green wool, moss carpet, pale fence, two buttons). */
	public static final List<String> DEFAULT_ITEMS = List.of(
		"minecraft:lime_wool",
		"minecraft:green_wool",
		"minecraft:moss_carpet",
		"minecraft:birch_fence",
		"minecraft:stone_button",
		"minecraft:oak_button");

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
