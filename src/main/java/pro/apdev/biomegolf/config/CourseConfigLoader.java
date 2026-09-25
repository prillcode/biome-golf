package pro.apdev.biomegolf.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import pro.apdev.biomegolf.course.CourseDefinition;
import pro.apdev.biomegolf.course.GeneratedLayoutIdentity;
import pro.apdev.biomegolf.course.HoleTransition;
import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.hole.HoleBoundary;
import pro.apdev.biomegolf.hole.HoleDefinition;

/** Loads the human-readable M5 definition for exactly three authored holes. */
public final class CourseConfigLoader {

	private CourseConfigLoader() {
	}

	public static CourseDefinition load(Path path) throws IOException {
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonElement root = JsonParser.parseReader(reader);
			if (!root.isJsonObject()) {
				throw new IllegalArgumentException("course configuration root must be an object");
			}
			return fromJson(root.getAsJsonObject());
		} catch (JsonParseException | IllegalStateException exception) {
			throw new IllegalArgumentException("invalid course configuration " + path + ": "
				+ exception.getMessage(), exception);
		}
	}

	private static CourseDefinition fromJson(JsonObject json) {
		String dimension = requiredString(json, "dimension");
		GeneratedLayoutIdentity courseLayout = requiredLayout(json, "generatedLayout");
		JsonArray holeArray = requiredArray(json, "holes");
		List<HoleDefinition> holes = new ArrayList<>(holeArray.size());
		for (JsonElement element : holeArray) {
			if (!element.isJsonObject()) {
				throw new IllegalArgumentException("holes must contain only objects");
			}
			holes.add(holeFromJson(element.getAsJsonObject(), dimension));
		}
		return new CourseDefinition(requiredString(json, "id"), requiredString(json, "displayName"),
			dimension, courseLayout, holes);
	}

	private static HoleDefinition holeFromJson(JsonObject json, String dimension) {
		JsonObject boundary = requiredObject(json, "boundary");
		JsonObject transition = requiredObject(json, "transition");
		return new HoleDefinition(
			requiredString(json, "id"),
			requiredInt(json, "number"),
			dimension,
			requiredVec3(json, "tee"),
			requiredVec3(json, "cup"),
			requiredInt(json, "par"),
			new HoleBoundary(requiredVec3(boundary, "min"), requiredVec3(boundary, "max")),
			requiredLayout(json, "generatedLayout"),
			new HoleTransition(requiredVec3(transition, "playerPosition"),
				requiredDouble(transition, "yaw"), requiredDouble(transition, "pitch")));
	}

	private static GeneratedLayoutIdentity requiredLayout(JsonObject json, String name) {
		JsonObject layout = requiredObject(json, name);
		return new GeneratedLayoutIdentity(requiredString(layout, "id"), requiredInt(layout, "version"));
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
