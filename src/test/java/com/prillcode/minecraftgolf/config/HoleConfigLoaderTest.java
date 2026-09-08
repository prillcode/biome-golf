package com.prillcode.minecraftgolf.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.prillcode.minecraftgolf.hole.HoleDefinition;

class HoleConfigLoaderTest {

	@TempDir
	Path tempDir;

	@Test
	void createsReadableDefaultAndLoadsIt() throws IOException {
		Path path = tempDir.resolve("minecraft_golf/hole.json");
		HoleDefinition hole = HoleConfigLoader.loadOrCreate(path);

		assertTrue(Files.exists(path));
		assertEquals("family_test:1", hole.id());
		assertEquals(4, hole.par());
		assertEquals(10, hole.strokeLimit());
		assertEquals(hole, HoleConfigLoader.load(path));
	}

	@Test
	void loadsConfiguredHole() throws IOException {
		Path path = tempDir.resolve("hole.json");
		Files.writeString(path, """
			{
			  "id": "cliff_course:3",
			  "number": 3,
			  "dimension": "minecraft:the_nether",
			  "tee": [10.5, 70.25, -4.5],
			  "cup": [30.5, 66.25, 12.5],
			  "par": 5,
			  "boundary": {"min": [0, 0, -20], "max": [40, 128, 20]}
			}
			""");

		HoleDefinition hole = HoleConfigLoader.load(path);
		assertEquals("cliff_course:3", hole.id());
		assertEquals("minecraft:the_nether", hole.dimension());
		assertEquals(12, hole.strokeLimit());
	}

	@Test
	void rejectsMissingMalformedAndDomainInvalidValues() throws IOException {
		Path missing = tempDir.resolve("missing.json");
		Files.writeString(missing, "{\"id\": \"test\"}");
		assertThrows(IllegalArgumentException.class, () -> HoleConfigLoader.load(missing));

		Path malformed = tempDir.resolve("malformed.json");
		Files.writeString(malformed, "{not json");
		assertThrows(IllegalArgumentException.class, () -> HoleConfigLoader.load(malformed));

		Path invalid = tempDir.resolve("invalid.json");
		Files.writeString(invalid, """
			{
			  "id": "test:1", "number": 1, "dimension": "minecraft:overworld",
			  "tee": [100, 64, 100], "cup": [2, 64, 2], "par": 0,
			  "boundary": {"min": [0, 0, 0], "max": [10, 100, 10]}
			}
			""");
		assertThrows(IllegalArgumentException.class, () -> HoleConfigLoader.load(invalid));

		Path fractional = tempDir.resolve("fractional.json");
		Files.writeString(fractional, """
			{
			  "id": "test:1", "number": 1.5, "dimension": "minecraft:overworld",
			  "tee": [1, 1, 1], "cup": [2, 1, 2], "par": 4,
			  "boundary": {"min": [0, 0, 0], "max": [10, 10, 10]}
			}
			""");
		assertThrows(IllegalArgumentException.class, () -> HoleConfigLoader.load(fractional));

		Path stringCoordinate = tempDir.resolve("string-coordinate.json");
		Files.writeString(stringCoordinate, """
			{
			  "id": "test:1", "number": 1, "dimension": "minecraft:overworld",
			  "tee": ["1", 1, 1], "cup": [2, 1, 2], "par": 4,
			  "boundary": {"min": [0, 0, 0], "max": [10, 10, 10]}
			}
			""");
		assertThrows(IllegalArgumentException.class, () -> HoleConfigLoader.load(stringCoordinate));
	}
}
