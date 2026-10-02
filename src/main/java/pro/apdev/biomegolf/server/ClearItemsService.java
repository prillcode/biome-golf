package pro.apdev.biomegolf.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonSyntaxException;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.config.ClearItems;

/**
 * Server-side owner of the Builder bulk-clear target groups.
 *
 * <p>Reads {@code config/minecraft_golf/clear_items.json} (a server-global
 * Fabric config, not per-world), mirroring {@link BuilderPaletteService}. The
 * parsed groups are stored at mod initialization, but block tags are only
 * resolved lazily at first use — registry tags are not bound yet during
 * {@code onInitialize}. An explicit {@code reload} is strict (all-or-nothing:
 * a malformed file, unknown block id, or unknown tag keeps the previous
 * groups), while the initial lazy resolution keeps every individually usable
 * group and logs the dropped ones, so a stray entry never hides the built-ins.
 * The default file is generated on first use and falls back to the built-in
 * groups (trees/logs/leaves/ground) if it is missing or invalid.</p>
 */
public final class ClearItemsService {

	private static final String DIRECTORY = "minecraft_golf";
	private static final String FILE_NAME = "clear_items.json";
	private static final ClearItemsService INSTANCE = new ClearItemsService();

	private volatile ClearItems rawItems;
	private volatile Map<String, ResolvedGroup> resolved = Map.of();
	private Path configPath;

	private ClearItemsService() {
	}

	public static ClearItemsService instance() {
		return INSTANCE;
	}

	/** A registry-resolved target group: matching blocks become air; everything else is untouched. */
	public record ResolvedGroup(String name, List<TagKey<Block>> tags, List<Block> blocks) {
		public boolean matches(BlockState state) {
			for (TagKey<Block> tag : tags) {
				if (state.is(tag)) {
					return true;
				}
			}
			for (Block block : blocks) {
				if (state.is(block)) {
					return true;
				}
			}
			return false;
		}
	}

	public record ReloadResult(boolean success, String message) {
	}

	public static void register() {
		INSTANCE.configPath = FabricLoader.getInstance().getConfigDir()
			.resolve(DIRECTORY).resolve(FILE_NAME);
		INSTANCE.loadInitial();
		MinecraftGolf.LOGGER.info("Registered clear-item groups ({} groups from {})",
			INSTANCE.rawItems.groups().size(), INSTANCE.configPath);
	}

	/** The resolved group for {@code name}, or {@code null} when unknown or unresolvable. */
	public ResolvedGroup resolve(String name) {
		return resolved().get(name);
	}

	/** Names of every available target group, sorted for stable display. */
	public Set<String> names() {
		return resolved().keySet();
	}

	public boolean isEmpty() {
		return resolved().isEmpty();
	}

	public String describe() {
		return resolved().keySet().stream().sorted().reduce((a, b) -> a + ", " + b).orElse("none");
	}

	/**
	 * Operator command: reloads the groups from disk, strict all-or-nothing. A
	 * valid file replaces the active groups; any validation or resolution
	 * failure retains the previous groups and reports the reason.
	 */
	public ReloadResult reload() {
		ClearItems parsed;
		try {
			parsed = readItems();
		} catch (IOException | JsonSyntaxException | IllegalArgumentException exception) {
			MinecraftGolf.LOGGER.error("Clear items reload failed; keeping the previous groups", exception);
			return new ReloadResult(false, "reload failed: " + exception.getMessage());
		}
		Map<String, ResolvedGroup> all = resolveOrNull(parsed);
		if (all == null) {
			MinecraftGolf.LOGGER.error("Clear items reload failed: unknown block id or tag");
			return new ReloadResult(false, "reload failed: unknown block id or tag; keeping the previous groups");
		}
		rawItems = parsed;
		resolved = all;
		MinecraftGolf.LOGGER.info("Clear items reloaded: {}", describe());
		return new ReloadResult(true, "groups reloaded: " + describe());
	}

	private void loadInitial() {
		if (configPath == null) {
			rawItems = ClearItems.defaults();
			return;
		}
		if (!Files.exists(configPath)) {
			rawItems = ClearItems.defaults();
			writeDefault();
			return;
		}
		try {
			rawItems = readItems();
		} catch (IOException | JsonSyntaxException | IllegalArgumentException exception) {
			MinecraftGolf.LOGGER.error("Invalid clear items at {}; using defaults and leaving the file untouched",
				configPath, exception);
			rawItems = ClearItems.defaults();
		}
	}

	private ClearItems readItems() throws IOException {
		return ClearItems.parse(Files.readString(configPath));
	}

	private void writeDefault() {
		try {
			Files.createDirectories(configPath.getParent());
			Files.writeString(configPath, ClearItems.toJson(ClearItems.defaults()));
			MinecraftGolf.LOGGER.info("Generated default clear items at {}", configPath);
		} catch (IOException exception) {
			MinecraftGolf.LOGGER.error("Failed to write default clear items to {}", configPath, exception);
		}
	}

	/** Lazy, partial resolution: keeps resolvable groups, logs and drops the rest. */
	private Map<String, ResolvedGroup> resolved() {
		Map<String, ResolvedGroup> current = resolved;
		ClearItems items = rawItems;
		if (current.isEmpty() && items != null && !items.groups().isEmpty()) {
			Map<String, ResolvedGroup> computed = new LinkedHashMap<>();
			for (Map.Entry<String, List<String>> entry : items.groups().entrySet()) {
				ResolvedGroup group = resolveGroup(entry.getKey(), entry.getValue());
				if (group == null) {
					MinecraftGolf.LOGGER.error("Clear group '{}' is unresolvable (unknown block id or tag); dropped",
						entry.getKey());
				} else {
					computed.put(entry.getKey(), group);
				}
			}
			current = Map.copyOf(computed);
			if (current.isEmpty() && !items.groups().isEmpty()) {
				MinecraftGolf.LOGGER.error("No clear groups resolved; /golf clear is unavailable");
			}
			resolved = current;
		}
		return current;
	}

	/** Strict all-or-nothing resolution; returns {@code null} when any entry is unknown. */
	private static Map<String, ResolvedGroup> resolveOrNull(ClearItems items) {
		Map<String, ResolvedGroup> resolved = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : items.groups().entrySet()) {
			ResolvedGroup group = resolveGroup(entry.getKey(), entry.getValue());
			if (group == null) {
				return null;
			}
			resolved.put(entry.getKey(), group);
		}
		return Map.copyOf(resolved);
	}

	/** Resolves one group; returns {@code null} when any entry is an unknown id or tag. */
	private static ResolvedGroup resolveGroup(String name, List<String> entries) {
		List<TagKey<Block>> tags = new ArrayList<>();
		List<Block> blocks = new ArrayList<>();
		for (String entry : entries) {
			boolean isTag = entry.startsWith("#");
			Identifier identifier;
			try {
				identifier = Identifier.parse(isTag ? entry.substring(1) : entry);
			} catch (RuntimeException exception) {
				return null;
			}
			if (isTag) {
				TagKey<Block> tag = TagKey.create(Registries.BLOCK, identifier);
				if (BuiltInRegistries.BLOCK.getTags().noneMatch(named -> named.key().equals(tag))) {
					return null;
				}
				tags.add(tag);
			} else {
				if (BuiltInRegistries.BLOCK.getOptional(identifier).isEmpty()) {
					return null;
				}
				blocks.add(BuiltInRegistries.BLOCK.getOptional(identifier).orElseThrow());
			}
		}
		return new ResolvedGroup(name, List.copyOf(tags), List.copyOf(blocks));
	}
}