package com.prillcode.minecraftgolf.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.config.AuthoredCourseStoreJson;
import com.prillcode.minecraftgolf.course.AuthoredCourseStore;
import com.prillcode.minecraftgolf.golf.Vec3;

/**
 * M8 S2 server-side owner of the single {@link AuthoredCourseStore} for the
 * running server/world.
 *
 * <p>The store persists as JSON at
 * {@code <world>/data/minecraft_golf_authored_courses.json} via
 * {@link AuthoredCourseStoreJson}. Loading is lazy on first access per server
	 * start: a missing file means an empty store; a malformed file logs an error
 * and fails closed to an empty store without ever crashing server boot. Every
 * successful mutation is followed by {@link #save()}.</p>
 *
 * <p>Transient per-player authoring state (current draft selection and pending
 * bounds corner) lives here in memory, keyed by player UUID, and is cleared on
 * server stop.</p>
 */
public final class AuthoredCourseService {

	private static final String FILE_NAME = "minecraft_golf_authored_courses.json";

	private static final AuthoredCourseService INSTANCE = new AuthoredCourseService();

	private AuthoredCourseStore store;
	private Path savePath;
	private final Map<UUID, String> currentDrafts = new HashMap<>();
	private final Map<UUID, PendingCorner> pendingCorners = new HashMap<>();
	private final Map<UUID, PendingLandscapeCorner> pendingLandscapeCorners = new HashMap<>();

	private AuthoredCourseService() {
	}

	public static AuthoredCourseService instance() {
		return INSTANCE;
	}

	/** Installs the server lifecycle hooks; call once from mod initialization. */
	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(instance()::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> instance().onServerStopping());
		MinecraftGolf.LOGGER.info("Registered authored course service (world JSON persistence at data/{})",
			FILE_NAME);
	}

	private void onServerStarted(MinecraftServer server) {
		savePath = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE_NAME);
		store = loadOrEmpty(savePath);
		refreshProtection();
	}

	private void onServerStopping() {
		store = null;
		savePath = null;
		currentDrafts.clear();
		pendingCorners.clear();
		pendingLandscapeCorners.clear();
		CourseBlockBreakGuard.clear();
	}

	/** The world-scoped store, loaded lazily on first access per server start. */
	public AuthoredCourseStore store() {
		if (savePath == null) {
			throw new IllegalStateException("authored course service is not attached to a running server");
		}
		if (store == null) {
			store = loadOrEmpty(savePath);
		}
		return store;
	}

	/** Saves the store after a successful mutation; failures are logged, not thrown. */
	public void save() {
		if (store == null || savePath == null) {
			return;
		}
		saveQuietly(store, savePath);
	}

	public void refreshProtection() {
		CourseBlockBreakGuard.replaceAuthoredCourses(
			store == null ? java.util.List.of() : store.finalizedCourses(),
			store == null ? java.util.List.of() : store.landscapes());
	}

	/** Loads the store from {@code path}: missing file or malformed content → empty store. */
	static AuthoredCourseStore loadOrEmpty(Path path) {
		if (!Files.exists(path)) {
			return new AuthoredCourseStore();
		}
		try {
			return AuthoredCourseStoreJson.load(path);
		} catch (IOException | IllegalArgumentException exception) {
			MinecraftGolf.LOGGER.error("Failed to load authored courses from {}; starting with an empty store",
				path, exception);
			return new AuthoredCourseStore();
		}
	}

	/** Saves the store to {@code path}; I/O failures are logged, never thrown. */
	static void saveQuietly(AuthoredCourseStore store, Path path) {
		try {
			AuthoredCourseStoreJson.save(store, path);
		} catch (IOException exception) {
			MinecraftGolf.LOGGER.error("Failed to save authored courses to {}", path, exception);
		}
	}

	// ------------------------------------------------------------------
	// Transient per-player authoring state
	// ------------------------------------------------------------------

	/** The player's current authoring draft id, or {@code null}. */
	public String currentDraft(UUID playerId) {
		return currentDrafts.get(playerId);
	}

	public void setCurrentDraft(UUID playerId, String courseId) {
		currentDrafts.put(playerId, courseId);
		pendingCorners.remove(playerId);
		pendingLandscapeCorners.remove(playerId);
	}

	public void clearCurrentDraft(UUID playerId) {
		currentDrafts.remove(playerId);
		pendingCorners.remove(playerId);
		pendingLandscapeCorners.remove(playerId);
	}

	/** Drops every player's current-draft/pending-corner state referencing {@code courseId}. */
	public void clearCurrentDraftFor(String courseId) {
		currentDrafts.entrySet().removeIf(entry -> entry.getValue().equals(courseId));
		pendingCorners.entrySet().removeIf(entry -> entry.getValue().courseId().equals(courseId));
		pendingLandscapeCorners.entrySet().removeIf(entry -> entry.getValue().courseId().equals(courseId));
	}

	/** The pending first bounds corner, or {@code null} when no capture is open. */
	public PendingCorner pendingCorner(UUID playerId) {
		return pendingCorners.get(playerId);
	}

	public void setPendingCorner(UUID playerId, PendingCorner corner) {
		pendingCorners.put(playerId, corner);
	}

	public void clearPendingCorner(UUID playerId) {
		pendingCorners.remove(playerId);
	}

	/** M8.10 S4: the pending first landscape-perimeter corner, or {@code null}. */
	public PendingLandscapeCorner pendingLandscapeCorner(UUID playerId) {
		return pendingLandscapeCorners.get(playerId);
	}

	public void setPendingLandscapeCorner(UUID playerId, PendingLandscapeCorner corner) {
		pendingLandscapeCorners.put(playerId, corner);
	}

	public void clearPendingLandscapeCorner(UUID playerId) {
		pendingLandscapeCorners.remove(playerId);
	}

	/** First captured corner of a two-corner bounds capture for one hole draft. */
	public record PendingCorner(String courseId, int holeNumber, Vec3 corner) {
	}

	/** M8.10 S4: first captured corner of a two-corner whole-course perimeter capture. */
	public record PendingLandscapeCorner(String courseId, Vec3 corner) {
	}
}
