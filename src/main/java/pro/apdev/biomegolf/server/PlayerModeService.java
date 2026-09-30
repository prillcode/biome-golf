package pro.apdev.biomegolf.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.course.AuthoredCourseStore;

/**
 * M8.15: server-owned player modes for supported Java golfers.
 *
 * <p>The supported Java experience has three modes over the same Minecraft world:
 * {@link PlayerMode#GOLF} (Survival game type plus Creative-style flight and damage
 * protection, sharing the normal inventory), {@link PlayerMode#WORLD} (ordinary
 * Survival play), and {@link PlayerMode#BUILDER} (operator Creative building with a
 * palette starter kit). Visitors never enter any of them.</p>
 *
 * <p>Mode selection is preserved in {@code <world>/data/minecraft_golf_player_modes.json}.
 * A supported newcomer defaults to Golf. Builder uses the Creative game type for
 * operators: entering Builder (or reconnecting as Builder) fills the inventory with the
 * palette starter kit, and leaving Builder returns to Survival keeping whatever the
 * operator carries — Creative items intentionally carry into World play. Legacy
 * isolation-era World-inventory snapshots under
 * {@code <world>/data/minecraft_golf_builder_inventories/} are restored once on the
 * first new-version join and then deleted. Leaving flight requires safe footing so
 * flight is never removed midair.</p>
 */
public final class PlayerModeService {

	private static final String FILE_NAME = "minecraft_golf_player_modes.json";
	private static final String INVENTORY_DIR = "minecraft_golf_builder_inventories";
	private static final String SCHEMA_KEY = "schemaVersion";
	private static final int SCHEMA_VERSION = 1;
	private static final int LANDING_SCAN_BLOCKS = 96;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final PlayerModeService INSTANCE = new PlayerModeService();

	private final Map<UUID, PlayerMode> modes = new HashMap<>();
	private final Map<UUID, String> builderCourses = new HashMap<>();

	private MinecraftServer server;
	private Path savePath;
	private Path inventoryDir;
	private boolean dirty;

	private PlayerModeService() {
	}

	public static PlayerModeService instance() {
		return INSTANCE;
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> INSTANCE.onServerStarted(server));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> INSTANCE.onServerStopping());
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> INSTANCE.onPlayerJoined(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> INSTANCE.onRespawn(newPlayer));
		MinecraftGolf.LOGGER.info("Registered player mode service (world JSON at data/{})", FILE_NAME);
	}

	// ── Queries ──────────────────────────────────────────────────────────────

	public PlayerMode mode(UUID playerId) {
		return modes.getOrDefault(playerId, PlayerMode.GOLF);
	}

	public String builderCourseId(UUID playerId) {
		return builderCourses.get(playerId);
	}

	public boolean isBuilder(UUID playerId) {
		return mode(playerId) == PlayerMode.BUILDER;
	}

	/** Human-readable mode summary for {@code /golf mode status}. */
	public String status(ServerPlayer player) {
		PlayerMode mode = mode(player.getUUID());
		StringBuilder message = new StringBuilder("[golf] mode: ").append(mode.displayName());
		if (mode == PlayerMode.BUILDER) {
			message.append(" (Creative Builder; palette starter kit");
			String course = builderCourseId(player.getUUID());
			if (course != null) {
				message.append("; course ").append(course);
			}
			message.append(")");
		} else if (mode == PlayerMode.GOLF) {
			message.append(" (flight, no damage; normal Survival items)");
		}
		message.append(" | switch with /golf mode golf|world");
		if (canBuild(player)) {
			message.append("|build");
		}
		return message.toString();
	}

	/** Whether the player currently holds the operator permission required to build. */
	public boolean canBuild(ServerPlayer player) {
		return Commands.LEVEL_GAMEMASTERS.check(player.permissions())
			&& !BuilderPaletteService.instance().isEmpty();
	}

	// ── Transitions ──────────────────────────────────────────────────────────

	public TransitionResult setGolf(ServerPlayer player) {
		return transition(player, PlayerMode.GOLF, null);
	}

	public TransitionResult setWorld(ServerPlayer player) {
		return transition(player, PlayerMode.WORLD, null);
	}

	public TransitionResult enterBuilder(ServerPlayer player, String courseId) {
		String normalized = courseId == null ? null : AuthoredCourseStore.normalizeId(courseId);
		return transition(player, PlayerMode.BUILDER, normalized);
	}

	/** Selects or switches the course a Builder may edit; requires active Builder mode. */
	public TransitionResult selectBuilderCourse(ServerPlayer player, String courseId) {
		if (VisitorService.isVisitor(player)) {
			return TransitionResult.fail("[golf] Builder mode is for supported Java clients");
		}
		if (mode(player.getUUID()) != PlayerMode.BUILDER) {
			return TransitionResult.fail("[golf] enter Builder mode first with /golf mode build");
		}
		String normalized = AuthoredCourseStore.normalizeId(courseId);
		String error = validateBuilderCourse(normalized);
		if (error != null) {
			return TransitionResult.fail(error);
		}
		builderCourses.put(player.getUUID(), normalized);
		dirty = true;
		save();
		MinecraftGolf.LOGGER.info("{} selected builder course {}",
			player.getName().getString(), normalized);
		return TransitionResult.ok("[golf] builder course set to " + normalized
			+ " (reference for status; Creative editing is not scope-limited)");
	}

	/** Draft or finalized courses, shown as an informational Builder reference. */
	public List<AuthoredCourseStore.BuilderCourse> buildableCourses() {
		try {
			return AuthoredCourseService.instance().store().builderCourses();
		} catch (IllegalStateException exception) {
			return List.of();
		}
	}

	/** Refills an active Builder's palette now; other modes are a no-op error. */
	public TransitionResult restockBuilder(ServerPlayer player) {
		if (VisitorService.isVisitor(player)) {
			return TransitionResult.fail("[golf] builder mode is for supported Java clients");
		}
		if (mode(player.getUUID()) != PlayerMode.BUILDER) {
			return TransitionResult.fail("[golf] you are not in Builder mode");
		}
		giveLoadout(player);
		return TransitionResult.ok("[golf] builder starter kit reset: "
			+ BuilderPaletteService.instance().describe());
	}

	/** Best-effort Golf switch used by successful round lifecycle commits; never fails an action. */
	public void enterGolfBestEffort(ServerPlayer player) {
		if (VisitorService.isVisitor(player) || mode(player.getUUID()) == PlayerMode.GOLF) {
			applyMode(player, PlayerMode.GOLF);
			return;
		}
		TransitionResult result = transition(player, PlayerMode.GOLF, null);
		if (!result.success()) {
			MinecraftGolf.LOGGER.warn("Could not switch {} to Golf mode: {}",
				player.getName().getString(), result.message());
		}
	}

	private TransitionResult transition(ServerPlayer player, PlayerMode target, String builderCourse) {
		if (VisitorService.isVisitor(player)) {
			return TransitionResult.fail("[golf] golf modes are for supported Java clients");
		}
		UUID playerId = player.getUUID();
		PlayerMode current = mode(playerId);
		String normalizedCourse = null;
		if (target == PlayerMode.BUILDER) {
			String entryError = validateBuilderEntry(player);
			if (entryError != null) {
				return TransitionResult.fail(entryError);
			}
			normalizedCourse = builderCourse == null ? null : AuthoredCourseStore.normalizeId(builderCourse);
			if (normalizedCourse != null) {
				String courseError = validateBuilderCourse(normalizedCourse);
				if (courseError != null) {
					return TransitionResult.fail(courseError);
				}
			}
		}

		if (target == current) {
			if (target != PlayerMode.BUILDER) {
				applyMode(player, current);
				return TransitionResult.ok("[golf] already in " + current.displayName() + " mode");
			}
			// Builder entry is idempotent: a selected course is kept unless a different one is given.
			String existing = builderCourses.get(playerId);
			if (normalizedCourse == null || Objects.equals(existing, normalizedCourse)) {
				applyMode(player, current);
				return TransitionResult.ok(existing == null
					? "[golf] already in Builder mode; select a course with /golf builder course <id>"
					: "[golf] already in Builder mode for " + existing);
			}
		}

		// Check safe footing before any mutation so a failed switch leaves state unchanged.
		if (target == PlayerMode.WORLD && current != PlayerMode.WORLD && !ensureSafeFooting(player)) {
			return TransitionResult.fail("[golf] land on solid ground before switching to World mode");
		}

		if (target == PlayerMode.BUILDER && current != PlayerMode.BUILDER) {
			// One-time migration from the isolation era: a stored World snapshot
			// restores the operator's original items instead of stranding them.
			if (hasSavedInventory(playerId)) {
				if (!restoreWorldInventory(player)) {
					return TransitionResult.fail("[golf] could not restore your legacy World inventory; staying in "
						+ current.displayName() + " mode");
				}
			} else {
				giveLoadout(player);
			}
		}

		modes.put(playerId, target);
		if (target == PlayerMode.BUILDER) {
			if (normalizedCourse != null) {
				builderCourses.put(playerId, normalizedCourse);
			} else {
				builderCourses.remove(playerId);
			}
		} else {
			builderCourses.remove(playerId);
		}
		dirty = true;
		applyMode(player, target);
		save();
		MinecraftGolf.LOGGER.info("{} switched to {} mode", player.getName().getString(), target.displayName());
		return TransitionResult.ok(switch (target) {
			case GOLF -> "[golf] Golf mode: flight and no damage, normal Survival items";
			case WORLD -> "[golf] World mode: Survival. Build and mine outside protected courses";
			case BUILDER -> normalizedCourse == null
				? "[golf] Builder (Creative): one-click build; /golf builder course <id> marks a course"
				: "[golf] Builder (Creative) for " + normalizedCourse
					+ ": palette starter kit supplied; /golf builder restock resets it";
		});
	}

	/** Builder entry requirements: gamemaster plus a non-empty palette. */
	private String validateBuilderEntry(ServerPlayer player) {
		if (!Commands.LEVEL_GAMEMASTERS.check(player.permissions())) {
			return "[golf] Builder mode requires operator/gamemaster permission";
		}
		if (BuilderPaletteService.instance().isEmpty()) {
			return "[golf] the builder palette is empty; add items to config/minecraft_golf/builder_palette.json";
		}
		return null;
	}

	/** Builder course requirements: any authored draft or finalized course (reference only). */
	private String validateBuilderCourse(String courseId) {
		try {
			var store = AuthoredCourseService.instance().store();
			if (!store.isDraft(courseId) && !store.isFinalized(courseId)) {
				return "[golf] '" + courseId + "' is not an available draft or finalized course";
			}
		} catch (IllegalStateException exception) {
			return "[golf] the authored course store is not available";
		}
		return null;
	}

	// ── Mode application ─────────────────────────────────────────────────────

	private void applyMode(ServerPlayer player, PlayerMode mode) {
		if (mode == PlayerMode.BUILDER) {
			// True Creative for operators: instant break/place, flight, and the full item
			// catalogue. Invulnerable keeps damage off like the other flight modes.
			player.setGameMode(GameType.CREATIVE);
			Abilities abilities = player.getAbilities();
			abilities.mayfly = true;
			abilities.instabuild = true;
			abilities.invulnerable = true;
			player.onUpdateAbilities();
			return;
		}
		player.setGameMode(GameType.SURVIVAL);
		Abilities abilities = player.getAbilities();
		switch (mode) {
			case GOLF -> {
				abilities.mayfly = true;
				abilities.invulnerable = true;
			}
			case WORLD -> {
				abilities.mayfly = false;
				abilities.flying = false;
				abilities.invulnerable = false;
			}
			case BUILDER -> {
				// handled above; the switch only serves the survival-backed modes
			}
		}
		player.onUpdateAbilities();
	}

	/**
	 * When removing flight, puts an airborne flyer on the nearest solid footing
	 * directly below instead of dropping them midair. Returns false (leaving state
	 * unchanged) when no safe column is found, for example over the void.
	 */
	private boolean ensureSafeFooting(ServerPlayer player) {
		Abilities abilities = player.getAbilities();
		if (!abilities.flying || player.onGround()) {
			return true;
		}
		ServerLevel level = (ServerLevel) player.level();
		BlockPos start = player.blockPosition();
		for (int y = start.getY() - 1; y >= level.getMinY() + 1; y--) {
			BlockPos feet = new BlockPos(start.getX(), y, start.getZ());
			if (!level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) continue;
			if (!level.getBlockState(feet).isAir()) continue;
			if (!level.getBlockState(feet.above()).isAir()) continue;
			if (!level.getFluidState(feet).isEmpty()) continue;
			player.teleportTo(level, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5,
				Set.of(), player.getYRot(), player.getXRot(), true);
			player.setDeltaMovement(0, 0, 0);
			player.resetFallDistance();
			abilities.flying = false;
			player.onUpdateAbilities();
			return true;
		}
		return false;
	}

	// ── Lifecycle ────────────────────────────────────────────────────────────

	private void onPlayerJoined(ServerPlayer player) {
		if (VisitorService.isVisitor(player)) {
			return;
		}
		UUID playerId = player.getUUID();
		PlayerMode saved = modes.getOrDefault(playerId, PlayerMode.GOLF);
		if (saved == PlayerMode.BUILDER) {
			String error = validateBuilderEntry(player);
			String builderCourse = builderCourses.get(playerId);
			if (error == null && builderCourse != null) {
				error = validateBuilderCourse(builderCourse);
			}
			if (error != null) {
				MinecraftGolf.LOGGER.warn("Demoting {} from Builder mode on join: {}",
					player.getName().getString(), error);
				if (hasSavedInventory(playerId)) {
					restoreWorldInventory(player);
				}
				builderCourses.remove(playerId);
				modes.put(playerId, PlayerMode.WORLD);
				dirty = true;
				save();
				applyMode(player, PlayerMode.WORLD);
				player.sendSystemMessage(Component.literal("[golf] " + error
					+ "; your World items were restored and you are in World mode"));
				return;
			}
			// One-time migration: restore any isolation-era World snapshot over the
			// stale Builder loadout; otherwise apply the fresh palette starter kit.
			if (hasSavedInventory(playerId)) {
				restoreWorldInventory(player);
			} else {
				giveLoadout(player);
			}
			applyMode(player, PlayerMode.BUILDER);
			return;
		}
		applyMode(player, saved);
	}

	private void onRespawn(ServerPlayer player) {
		if (VisitorService.isVisitor(player)) {
			return;
		}
		PlayerMode mode = mode(player.getUUID());
		if (mode == PlayerMode.BUILDER) {
			giveLoadout(player);
		}
		applyMode(player, mode);
	}

	// ── Legacy Builder inventory snapshot migration ──────────────────────────

	private Path inventoryFile(UUID playerId) {
		return inventoryDir == null ? null : inventoryDir.resolve(playerId + ".nbt");
	}

	private boolean hasSavedInventory(UUID playerId) {
		Path file = inventoryFile(playerId);
		return file != null && Files.exists(file);
	}

	private boolean restoreWorldInventory(ServerPlayer player) {
		Path file = inventoryFile(player.getUUID());
		if (file == null || !Files.exists(file)) {
			MinecraftGolf.LOGGER.error("No saved World inventory for {}", player.getName().getString());
			return false;
		}
		try {
			CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
			var input = TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), tag);
			Inventory inventory = player.getInventory();
			NonNullList<ItemStack> restored = NonNullList.withSize(
				inventory.getNonEquipmentItems().size(), ItemStack.EMPTY);
			ContainerHelper.loadAllItems(input, restored);
			NonNullList<ItemStack> target = inventory.getNonEquipmentItems();
			for (int slot = 0; slot < target.size(); slot++) {
				target.set(slot, restored.get(slot));
			}
			inventory.setChanged();
			player.inventoryMenu.broadcastChanges();
			Files.deleteIfExists(file);
			return true;
		} catch (IOException | RuntimeException exception) {
			MinecraftGolf.LOGGER.error("Failed to restore World inventory for {} from {}",
				player.getName().getString(), file, exception);
			return false;
		}
	}

	/** Resets the Builder inventory to the palette starter kit (clubs + one of each block). */
	private void giveLoadout(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		inventory.clearContent();
		for (ItemStack stack : BuilderPaletteService.instance().loadout()) {
			if (!inventory.add(stack)) {
				player.drop(stack, false);
			}
		}
		inventory.setChanged();
		player.inventoryMenu.broadcastChanges();
	}

	// ── Server persistence ───────────────────────────────────────────────────

	private void onServerStarted(MinecraftServer server) {
		this.server = server;
		Path root = server.getWorldPath(LevelResource.ROOT);
		savePath = root.resolve("data").resolve(FILE_NAME);
		inventoryDir = root.resolve("data").resolve(INVENTORY_DIR);
		load();
	}

	private void onServerStopping() {
		save();
		modes.clear();
		builderCourses.clear();
		server = null;
		savePath = null;
		inventoryDir = null;
		dirty = false;
	}

	private void load() {
		modes.clear();
		builderCourses.clear();
		if (savePath == null || !Files.exists(savePath)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseReader(Files.newBufferedReader(savePath)).getAsJsonObject();
			JsonObject players = root.has("players") && root.get("players").isJsonObject()
				? root.getAsJsonObject("players") : new JsonObject();
			for (Map.Entry<String, JsonElement> entry : players.entrySet()) {
				if (!entry.getValue().isJsonObject()) {
					continue;
				}
				try {
					UUID playerId = UUID.fromString(entry.getKey());
					JsonObject json = entry.getValue().getAsJsonObject();
					PlayerMode mode = PlayerMode.valueOf(json.get("mode").getAsString());
					modes.put(playerId, mode);
					if (mode == PlayerMode.BUILDER && json.has("builderCourse")) {
						builderCourses.put(playerId, json.get("builderCourse").getAsString());
					}
				} catch (RuntimeException ignored) {
					MinecraftGolf.LOGGER.warn("Ignoring invalid player mode entry '{}' in {}", entry.getKey(), savePath);
				}
			}
		} catch (IOException | RuntimeException exception) {
			MinecraftGolf.LOGGER.error("Failed to load player modes from {}; starting with defaults", savePath, exception);
		}
	}

	private void save() {
		if (savePath == null || !dirty) {
			return;
		}
		try {
			Files.createDirectories(savePath.getParent());
			JsonObject root = new JsonObject();
			root.addProperty(SCHEMA_KEY, SCHEMA_VERSION);
			JsonObject players = new JsonObject();
			for (Map.Entry<UUID, PlayerMode> entry : modes.entrySet()) {
				JsonObject json = new JsonObject();
				json.addProperty("mode", entry.getValue().name());
				String builderCourse = builderCourses.get(entry.getKey());
				if (entry.getValue() == PlayerMode.BUILDER && builderCourse != null) {
					json.addProperty("builderCourse", builderCourse);
				}
				players.add(entry.getKey().toString(), json);
			}
			root.add("players", players);
			Files.writeString(savePath, GSON.toJson(root));
			dirty = false;
		} catch (IOException exception) {
			MinecraftGolf.LOGGER.error("Failed to save player modes to {}", savePath, exception);
		}
	}


	public record TransitionResult(boolean success, String message) {
		public static TransitionResult ok(String message) {
			return new TransitionResult(true, message);
		}

		public static TransitionResult fail(String message) {
			return new TransitionResult(false, message);
		}
	}
}
