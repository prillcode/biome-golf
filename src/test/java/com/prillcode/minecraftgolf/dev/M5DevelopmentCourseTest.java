package com.prillcode.minecraftgolf.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

class M5DevelopmentCourseTest {

	@Test
	void fixtureRemainsExactlyThreeHolesOrderedParFourThreeFive() {
		CourseDefinition definition = M5DevelopmentCourse.definition();

		assertEquals(3, definition.holes().size());
		assertEquals(List.of(1, 2, 3),
			definition.holes().stream().map(HoleDefinition::number).toList());
		assertEquals(List.of(4, 3, 5),
			definition.holes().stream().map(HoleDefinition::par).toList());
		assertEquals(12, definition.totalPar());
	}
}
