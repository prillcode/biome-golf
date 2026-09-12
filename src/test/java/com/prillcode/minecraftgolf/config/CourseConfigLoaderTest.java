package com.prillcode.minecraftgolf.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.prillcode.minecraftgolf.course.CourseDefinition;

class CourseConfigLoaderTest {

	@TempDir
	Path tempDir;

	@Test
	void loadsExactlyThreeAuthoredHolesWithLayoutAndTransitionMetadata() throws IOException {
		Path path = tempDir.resolve("course.json");
		Files.writeString(path, validCourseJson());

		CourseDefinition course = CourseConfigLoader.load(path);
		assertEquals("m5-development", course.id());
		assertEquals(7, course.generatedLayout().version());
		assertEquals(3, course.holes().size());
		assertEquals(5, course.hole(3).par());
		assertEquals(90.0, course.hole(2).transition().yaw());
	}

	@Test
	void rejectsMalformedMissingAndInvalidCourseConfiguration() throws IOException {
		Path malformed = tempDir.resolve("malformed.json");
		Files.writeString(malformed, "{not json");
		assertThrows(IllegalArgumentException.class, () -> CourseConfigLoader.load(malformed));

		Path missing = tempDir.resolve("missing.json");
		Files.writeString(missing, "{\"id\":\"test\"}");
		assertThrows(IllegalArgumentException.class, () -> CourseConfigLoader.load(missing));

		Path wrongHoleOrder = tempDir.resolve("wrong-order.json");
		Files.writeString(wrongHoleOrder, validCourseJson().replace("\"number\": 2", "\"number\": 3"));
		assertThrows(IllegalArgumentException.class, () -> CourseConfigLoader.load(wrongHoleOrder));

		Path outsideTransition = tempDir.resolve("outside-transition.json");
		Files.writeString(outsideTransition,
			validCourseJson().replace("\"playerPosition\": [10, 65, 0]",
				"\"playerPosition\": [1000, 65, 0]"));
		assertThrows(IllegalArgumentException.class, () -> CourseConfigLoader.load(outsideTransition));
	}

	private static String validCourseJson() {
		return """
			{
			  "id": "m5-development",
			  "displayName": "M5 Development Course",
			  "dimension": "minecraft:overworld",
			  "generatedLayout": {"id": "m5-campus", "version": 7},
			  "holes": [
			    {
			      "id": "m5:1", "number": 1, "par": 4,
			      "tee": [10, 64.25, 0], "cup": [30, 64.25, 0],
			      "boundary": {"min": [0, 0, -20], "max": [40, 100, 20]},
			      "generatedLayout": {"id": "m5-par-3", "version": 2},
			      "transition": {"playerPosition": [10, 65, 0], "yaw": 0, "pitch": 0}
			    },
			    {
			      "id": "m5:2", "number": 2, "par": 3,
			      "tee": [50, 64.25, 0], "cup": [70, 64.25, 0],
			      "boundary": {"min": [40, 0, -20], "max": [80, 100, 20]},
			      "generatedLayout": {"id": "m5-par-4", "version": 2},
			      "transition": {"playerPosition": [50, 65, 0], "yaw": 90, "pitch": 0}
			    },
			    {
			      "id": "m5:3", "number": 3, "par": 5,
			      "tee": [90, 64.25, 0], "cup": [110, 64.25, 0],
			      "boundary": {"min": [80, 0, -20], "max": [120, 100, 20]},
			      "generatedLayout": {"id": "m5-par-5", "version": 2},
			      "transition": {"playerPosition": [90, 65, 0], "yaw": 180, "pitch": 0}
			    }
			  ]
			}
			""";
	}
}
