package pro.apdev.biomegolf.round;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.course.CourseDefinition;
import pro.apdev.biomegolf.course.GeneratedLayoutIdentity;
import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.hole.HoleBoundary;
import pro.apdev.biomegolf.hole.HoleDefinition;

class RoundLobbyProjectionTest {
	@Test
	void preservesStableDistinctSameCourseLobbiesAndFullIds() {
		CourseDefinition course = course("pine");
		UUID firstPlayer = UUID.randomUUID();
		UUID secondPlayer = UUID.randomUUID();
		ReadyGolfRound first = ReadyGolfRound.create(UUID.randomUUID(), course, firstPlayer);
		ReadyGolfRound second = ReadyGolfRound.create(UUID.randomUUID(), course, secondPlayer);

		RoundLobbyProjection projection = RoundLobbyProjection.from(List.of(course), List.of(first, second),
			id -> id.equals(firstPlayer) ? "First" : "Second");

		assertEquals(List.of(first.roundId(), second.roundId()),
			projection.lobbies().stream().map(RoundLobbyProjection.LobbyEntry::roundId).toList());
		assertEquals(List.of("First", "Second"),
			projection.lobbies().stream().map(RoundLobbyProjection.LobbyEntry::coordinatorName).toList());
	}

	@Test
	void supportsMoreThanLegacySixtyFourCourseLimit() {
		List<CourseDefinition> courses = java.util.stream.IntStream.range(0, 65)
			.mapToObj(index -> course("course-" + index)).toList();
		assertEquals(65, RoundLobbyProjection.from(courses, List.of(), UUID::toString).courses().size());
	}

	private static CourseDefinition course(String id) {
		Vec3 tee = new Vec3(0.0, 64.0, 0.0);
		HoleDefinition hole = new HoleDefinition(id + ":1", 1, "minecraft:overworld", tee,
			new Vec3(10.0, 64.0, 0.0), 4,
			new HoleBoundary(new Vec3(-5.0, 0.0, -5.0), new Vec3(20.0, 100.0, 5.0)));
		return new CourseDefinition(id, "Course " + id, "minecraft:overworld",
			new GeneratedLayoutIdentity(id, 1), List.of(hole));
	}
}
