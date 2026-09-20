package com.prillcode.minecraftgolf.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import com.prillcode.minecraftgolf.course.AuthoredCourseStore;
import com.prillcode.minecraftgolf.course.AuthoredCourseStore.DraftSnapshot;
import com.prillcode.minecraftgolf.course.AuthoredCourseStore.HoleSnapshot;
import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.CourseLandscape;
import com.prillcode.minecraftgolf.course.HoleTransition;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

/**
 * Gson round-trip of {@link AuthoredCourseStore} contents (drafts, finalized
 * courses, and the persistent default). This is only the persistence model; Minecraft SavedData wiring is
 * deferred. Loading always replays data through the store so every domain
 * invariant is re-validated; malformed or tampered data fails closed with an
 * {@link IllegalArgumentException} carrying a precise message.
 */
public final class AuthoredCourseStoreJson {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private AuthoredCourseStoreJson() {
	}

	public static void save(AuthoredCourseStore store, Path path) throws IOException {
		JsonObject root = new JsonObject();
		JsonArray draftArray = new JsonArray();
		for (DraftSnapshot draft : store.draftSnapshots()) {
			draftArray.add(draftToJson(draft, store.landscape(draft.id()).orElse(null)));
		}
		JsonArray finalizedArray = new JsonArray();
		for (CourseDefinition course : store.finalizedCourses()) {
			finalizedArray.add(courseToJson(course, store.landscape(course.id()).orElse(null)));
		}
		root.add("drafts", draftArray);
		root.add("finalized", finalizedArray);
		store.defaultCourseId().ifPresent(id -> root.addProperty("defaultCourseId", id));
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(root, writer);
		}
	}

	public static AuthoredCourseStore load(Path path) throws IOException {
		JsonElement root;
		try (Reader reader = Files.newBufferedReader(path)) {
			root = JsonParser.parseReader(reader);
		} catch (JsonParseException | IllegalStateException exception) {
			throw new IllegalArgumentException("invalid authored course store " + path + ": "
				+ exception.getMessage(), exception);
		}
		try {
			if (root == null || !root.isJsonObject()) {
				throw new IllegalArgumentException("authored course store root must be an object");
			}
			return fromJson(root.getAsJsonObject());
		} catch (IllegalArgumentException | IllegalStateException exception) {
			throw new IllegalArgumentException("invalid authored course store " + path + ": "
				+ exception.getMessage(), exception);
		}
	}

	private static AuthoredCourseStore fromJson(JsonObject root) {
		AuthoredCourseStore store = new AuthoredCourseStore();
		for (JsonElement element : requiredArray(root, "drafts")) {
			draftFromJson(store, requiredObject(element, "drafts entry"));
		}
		for (JsonElement element : requiredArray(root, "finalized")) {
			finalizedFromJson(store, requiredObject(element, "finalized entry"));
		}
		if (has(root, "defaultCourseId")) {
			store.setDefaultCourse(requiredString(root, "defaultCourseId"));
		}
		return store;
	}

	private static void draftFromJson(AuthoredCourseStore store, JsonObject json) {
		String dimension = requiredString(json, "dimension");
		String id = store.createCourse(requiredString(json, "id"), requiredString(json, "displayName"),
			dimension);
		for (JsonElement element : optionalArray(json, "holes")) {
			holeDraftFromJson(store, id, requiredObject(element, "hole entry"));
		}
		landscapeFromJson(store, json, id, dimension);
	}

	private static void holeDraftFromJson(AuthoredCourseStore store, String courseId, JsonObject json) {
		int number = requiredInt(json, "number");
		Vec3 tee = optionalVec3(json, "tee");
		if (tee != null) {
			store.setHoleTee(courseId, number, tee);
		}
		Vec3 cup = optionalVec3(json, "cup");
		if (cup != null) {
			store.setHoleCup(courseId, number, cup);
		}
		if (has(json, "par")) {
			store.setHolePar(courseId, number, requiredInt(json, "par"));
		}
		JsonObject boundary = optionalObject(json, "boundary");
		if (boundary != null) {
			store.setHoleBounds(courseId, number,
				new HoleBoundary(requiredVec3(boundary, "min"), requiredVec3(boundary, "max")));
		}
		JsonObject transition = optionalObject(json, "transition");
		if (transition != null) {
			store.setHoleTransition(courseId, number,
				new HoleTransition(requiredVec3(transition, "playerPosition"),
					requiredDouble(transition, "yaw"), requiredDouble(transition, "pitch")));
		}
	}

	private static void finalizedFromJson(AuthoredCourseStore store, JsonObject json) {
		// Rebuild every hole through the store so tee/cup/boundary invariants
		// and numbering are re-validated on load.
		String dimension = requiredString(json, "dimension");
		String id = store.createCourse(requiredString(json, "id"), requiredString(json, "displayName"),
			dimension);
		for (JsonElement element : requiredArray(json, "holes")) {
			JsonObject hole = requiredObject(element, "hole entry");
			int number = requiredInt(hole, "number");
			JsonObject boundary = optionalObject(hole, "boundary");
			JsonObject transition = requiredObject(hole, "transition");
			store.setHoleTee(id, number, requiredVec3(hole, "tee"));
			store.setHoleCup(id, number, requiredVec3(hole, "cup"));
			store.setHolePar(id, number, requiredInt(hole, "par"));
			if (boundary != null) {
				store.setHoleBounds(id, number,
					new HoleBoundary(requiredVec3(boundary, "min"), requiredVec3(boundary, "max")));
			}
			store.setHoleTransition(id, number,
				new HoleTransition(requiredVec3(transition, "playerPosition"),
					requiredDouble(transition, "yaw"), requiredDouble(transition, "pitch")));
		}
		store.finalize(id);
		landscapeFromJson(store, json, id, dimension);
	}

	/**
	 * M8.10 S1: reads optional per-course landscape metadata. An absent field
	 * means "no perimeter, unlocked", keeping older files loadable.
	 */
	private static void landscapeFromJson(AuthoredCourseStore store, JsonObject json, String courseId,
			String dimension) {
		JsonObject landscape = optionalObject(json, "landscape");
		if (landscape == null) {
			return;
		}
		HoleBoundary bounds = new HoleBoundary(requiredVec3(landscape, "min"), requiredVec3(landscape, "max"));
		store.setLandscape(new CourseLandscape(courseId, dimension, bounds, requiredBoolean(landscape, "locked")));
	}

	private static JsonObject draftToJson(DraftSnapshot draft, CourseLandscape landscape) {
		JsonObject json = new JsonObject();
		json.addProperty("id", draft.id());
		json.addProperty("displayName", draft.displayName());
		json.addProperty("dimension", draft.dimension());
		JsonArray holes = new JsonArray();
		for (HoleSnapshot hole : draft.holes()) {
			JsonObject holeJson = new JsonObject();
			holeJson.addProperty("number", hole.number());
			holeJson.add("tee", vec3ToJson(hole.tee()));
			holeJson.add("cup", vec3ToJson(hole.cup()));
			if (hole.par() != null) {
				holeJson.addProperty("par", hole.par());
			}
			holeJson.add("boundary", boundaryToJson(hole.boundary()));
			holeJson.add("transition", transitionToJson(hole.transition()));
			holes.add(holeJson);
		}
		json.add("holes", holes);
		json.add("landscape", landscapeToJson(landscape));
		return json;
	}

	private static JsonObject courseToJson(CourseDefinition course, CourseLandscape landscape) {
		JsonObject json = new JsonObject();
		json.addProperty("id", course.id());
		json.addProperty("displayName", course.displayName());
		json.addProperty("dimension", course.dimension());
		JsonArray holes = new JsonArray();
		for (HoleDefinition hole : course.holes()) {
			JsonObject holeJson = new JsonObject();
			holeJson.addProperty("number", hole.number());
			holeJson.add("tee", vec3ToJson(hole.tee()));
			holeJson.add("cup", vec3ToJson(hole.cup()));
			holeJson.addProperty("par", hole.par());
			holeJson.add("boundary", boundaryToJson(hole.boundary()));
			holeJson.add("transition", transitionToJson(hole.transition()));
			holes.add(holeJson);
		}
		json.add("holes", holes);
		json.add("landscape", landscapeToJson(landscape));
		return json;
	}

	/** M8.10 S1: serializes landscape metadata, or JSON null when none is authored. */
	private static JsonElement landscapeToJson(CourseLandscape landscape) {
		if (landscape == null) {
			return JsonNull.INSTANCE;
		}
		JsonObject json = new JsonObject();
		json.add("min", vec3ToJson(landscape.bounds().min()));
		json.add("max", vec3ToJson(landscape.bounds().max()));
		json.addProperty("locked", landscape.locked());
		return json;
	}

	private static JsonElement vec3ToJson(Vec3 value) {
		if (value == null) {
			return JsonNull.INSTANCE;
		}
		JsonArray array = new JsonArray();
		array.add(value.x());
		array.add(value.y());
		array.add(value.z());
		return array;
	}

	private static JsonElement boundaryToJson(HoleBoundary boundary) {
		if (boundary == null) {
			return JsonNull.INSTANCE;
		}
		JsonObject json = new JsonObject();
		json.add("min", vec3ToJson(boundary.min()));
		json.add("max", vec3ToJson(boundary.max()));
		return json;
	}

	private static JsonElement transitionToJson(HoleTransition transition) {
		if (transition == null) {
			return JsonNull.INSTANCE;
		}
		JsonObject json = new JsonObject();
		json.add("playerPosition", vec3ToJson(transition.playerPosition()));
		json.addProperty("yaw", transition.yaw());
		json.addProperty("pitch", transition.pitch());
		return json;
	}

	private static boolean has(JsonObject json, String name) {
		JsonElement value = json.get(name);
		return value != null && !value.isJsonNull();
	}

	private static JsonArray optionalArray(JsonObject json, String name) {
		if (!has(json, name)) {
			return new JsonArray();
		}
		JsonElement value = json.get(name);
		if (!value.isJsonArray()) {
			throw new IllegalArgumentException(name + " must be an array");
		}
		return value.getAsJsonArray();
	}

	private static JsonObject optionalObject(JsonObject json, String name) {
		if (!has(json, name)) {
			return null;
		}
		return requiredObject(json, name);
	}

	private static Vec3 optionalVec3(JsonObject json, String name) {
		if (!has(json, name)) {
			return null;
		}
		return requiredVec3(json, name);
	}

	private static String requiredString(JsonObject json, String name) {
		JsonElement value = required(json, name);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
			throw new IllegalArgumentException(name + " must be a string");
		}
		return value.getAsString();
	}

	private static int requiredInt(JsonObject json, String name) {
		JsonElement value = required(json, name);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			throw new IllegalArgumentException(name + " must be an integer");
		}
		try {
			return value.getAsBigDecimal().intValueExact();
		} catch (ArithmeticException | NumberFormatException exception) {
			throw new IllegalArgumentException(name + " must be an integer", exception);
		}
	}

	private static boolean requiredBoolean(JsonObject json, String name) {
		JsonElement value = required(json, name);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
			throw new IllegalArgumentException(name + " must be a boolean");
		}
		return value.getAsBoolean();
	}

	private static double requiredDouble(JsonObject json, String name) {
		JsonElement value = required(json, name);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			throw new IllegalArgumentException(name + " must be a number");
		}
		double result = value.getAsDouble();
		if (!Double.isFinite(result)) {
			throw new IllegalArgumentException(name + " must be finite");
		}
		return result;
	}

	private static Vec3 requiredVec3(JsonObject json, String name) {
		JsonArray array = requiredArray(json, name);
		if (array.size() != 3) {
			throw new IllegalArgumentException(name + " must contain exactly three coordinates");
		}
		return new Vec3(coordinate(array.get(0), name), coordinate(array.get(1), name),
			coordinate(array.get(2), name));
	}

	private static double coordinate(JsonElement value, String name) {
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			throw new IllegalArgumentException(name + " must contain only numbers");
		}
		double coordinate = value.getAsDouble();
		if (!Double.isFinite(coordinate)) {
			throw new IllegalArgumentException(name + " coordinates must be finite");
		}
		return coordinate;
	}

	private static JsonArray requiredArray(JsonObject json, String name) {
		JsonElement value = required(json, name);
		if (!value.isJsonArray()) {
			throw new IllegalArgumentException(name + " must be an array");
		}
		return value.getAsJsonArray();
	}

	private static JsonObject requiredObject(JsonElement element, String name) {
		if (element == null || !element.isJsonObject()) {
			throw new IllegalArgumentException(name + " must be an object");
		}
		return element.getAsJsonObject();
	}

	private static JsonObject requiredObject(JsonObject json, String name) {
		JsonElement value = required(json, name);
		if (!value.isJsonObject()) {
			throw new IllegalArgumentException(name + " must be an object");
		}
		return value.getAsJsonObject();
	}

	private static JsonElement required(JsonObject json, String name) {
		JsonElement value = json.get(name);
		if (value == null || value.isJsonNull()) {
			throw new IllegalArgumentException("missing required field: " + name);
		}
		return value;
	}
}
