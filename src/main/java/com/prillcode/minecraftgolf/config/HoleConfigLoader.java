package com.prillcode.minecraftgolf.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

/** Loads the one human-readable configured M4 hole. */
public final class HoleConfigLoader {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private HoleConfigLoader() {
	}

	public static HoleDefinition loadOrCreate(Path path) throws IOException {
		if (Files.notExists(path)) {
			Path parent = path.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Files.writeString(path, GSON.toJson(toJson(defaultHole())) + System.lineSeparator());
		}
		return load(path);
	}

	public static HoleDefinition load(Path path) throws IOException {
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonElement root = JsonParser.parseReader(reader);
			if (!root.isJsonObject()) {
				throw new IllegalArgumentException("hole configuration root must be an object");
			}
			return fromJson(root.getAsJsonObject());
		} catch (JsonParseException | IllegalStateException exception) {
			throw new IllegalArgumentException("invalid hole configuration " + path + ": "
				+ exception.getMessage(), exception);
		}
	}

	public static HoleDefinition defaultHole() {
		return new HoleDefinition(
			"family_test:1",
			1,
			"minecraft:overworld",
			new Vec3(0.5, 63.25, 0.5),
			new Vec3(16.5, 63.25, 0.5),
			4,
			new HoleBoundary(
				new Vec3(-32.0, -64.0, -32.0),
				new Vec3(64.0, 384.0, 32.0)
			)
		);
	}

	private static HoleDefinition fromJson(JsonObject json) {
		JsonObject boundary = requiredObject(json, "boundary");
		return new HoleDefinition(
			requiredString(json, "id"),
			requiredInt(json, "number"),
			requiredString(json, "dimension"),
			requiredVec3(json, "tee"),
			requiredVec3(json, "cup"),
			requiredInt(json, "par"),
			new HoleBoundary(requiredVec3(boundary, "min"), requiredVec3(boundary, "max"))
		);
	}

	private static JsonObject toJson(HoleDefinition hole) {
		JsonObject json = new JsonObject();
		json.addProperty("id", hole.id());
		json.addProperty("number", hole.number());
		json.addProperty("dimension", hole.dimension());
		json.add("tee", vec3(hole.tee()));
		json.add("cup", vec3(hole.cup()));
		json.addProperty("par", hole.par());
		JsonObject boundary = new JsonObject();
		boundary.add("min", vec3(hole.boundary().min()));
		boundary.add("max", vec3(hole.boundary().max()));
		json.add("boundary", boundary);
		return json;
	}

	private static JsonArray vec3(Vec3 value) {
		JsonArray array = new JsonArray();
		array.add(value.x());
		array.add(value.y());
		array.add(value.z());
		return array;
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

	private static Vec3 requiredVec3(JsonObject json, String name) {
		JsonElement value = required(json, name);
		if (!value.isJsonArray()) {
			throw new IllegalArgumentException(name + " must be a three-number array");
		}
		JsonArray array = value.getAsJsonArray();
		if (array.size() != 3) {
			throw new IllegalArgumentException(name + " must contain exactly three coordinates");
		}
		return new Vec3(
			requiredCoordinate(array.get(0), name),
			requiredCoordinate(array.get(1), name),
			requiredCoordinate(array.get(2), name));
	}

	private static double requiredCoordinate(JsonElement value, String name) {
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			throw new IllegalArgumentException(name + " must contain only numbers");
		}
		return value.getAsDouble();
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
