package com.prillcode.minecraftgolf.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.prillcode.minecraftgolf.course.AuthoredCourseStore;
import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.HoleTransition;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;

class AuthoredCourseStoreJsonTest {

	private static final String DIMENSION = "minecraft:overworld";

	@TempDir
	Path directory;

	@Test
	void roundTripsDraftsAndFinalizedCourses() throws IOException {
		AuthoredCourseStore store = new AuthoredCourseStore();

		// Partial draft: hole 1 complete, hole 2 has only a par.
		store.createCourse("drafty", "Drafty", DIMENSION);
		completeHole(store, "drafty", 1, 4);
		store.setHolePar("drafty", 2, 3);

		// Finalized course.
		store.createCourse("links", "Links", DIMENSION);
		completeHole(store, "links", 1, 4);
		completeHole(store, "links", 2, 5);
		CourseDefinition expected = store.finalize("links");

		Path path = directory.resolve("courses.json");
		AuthoredCourseStoreJson.save(store, path);

		AuthoredCourseStore loaded = AuthoredCourseStoreJson.load(path);
		assertEquals(expected, loaded.finalizedCourse("links"));
		assertEquals(1, loaded.finalizedCourses().size());

		AuthoredCourseStore.DraftSnapshot draft = loaded.draftSnapshot("drafty");
		assertEquals("Drafty", draft.displayName());
		assertEquals(2, draft.holes().size());
		assertEquals(tee(0), draft.holes().get(0).tee());
		assertEquals(null, draft.holes().get(1).tee());
		assertEquals(3, draft.holes().get(1).par());

		// The loaded draft can still be completed and finalized.
		completeHole(loaded, "drafty", 2, 3);
		assertEquals(2, loaded.finalize("drafty").holes().size());
	}

	@Test
	void roundTripsExplicitTransitions() throws IOException {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("links", "Links", DIMENSION);
		completeHole(store, "links", 1, 4);
		HoleTransition transition = new HoleTransition(tee(0), 118.0, -5.0);
		store.setHoleTransition("links", 1, transition);
		store.finalize("links");

		Path path = directory.resolve("courses.json");
		AuthoredCourseStoreJson.save(store, path);
		assertEquals(transition,
			AuthoredCourseStoreJson.load(path).finalizedCourse("links").hole(1).transition());
	}

	@Test
	void rejectsMalformedJson() throws IOException {
		Path path = directory.resolve("broken.json");
		Files.writeString(path, "{ not json");
		assertThrows(IllegalArgumentException.class, () -> AuthoredCourseStoreJson.load(path));

		Path notObject = directory.resolve("array.json");
		Files.writeString(notObject, "[]");
		assertThrows(IllegalArgumentException.class, () -> AuthoredCourseStoreJson.load(notObject));

		Path missing = directory.resolve("missing.json");
		Files.writeString(missing, "{\"drafts\": []}");
		assertThrows(IllegalArgumentException.class, () -> AuthoredCourseStoreJson.load(missing));

		Path badHole = directory.resolve("bad-hole.json");
		Files.writeString(badHole, """
			{"drafts": [{"id": "links", "displayName": "Links", "dimension": "minecraft:overworld",
			  "holes": [{"number": 1, "tee": [1, 2]}]}], "finalized": []}
			""");
		assertThrows(IllegalArgumentException.class, () -> AuthoredCourseStoreJson.load(badHole));
	}

	@Test
	void loadRevalidatesDomainInvariants() throws IOException {
		// Hand-edited finalized course with the tee outside the boundary.
		Path unsafe = directory.resolve("unsafe.json");
		Files.writeString(unsafe, """
			{"drafts": [], "finalized": [{
			  "id": "links", "displayName": "Links", "dimension": "minecraft:overworld",
			  "holes": [{"number": 1, "tee": [500.0, 64.0, 0.0], "cup": [40.0, 64.0, 0.0],
			    "par": 4, "boundary": {"min": [-100.0, 0.0, -100.0], "max": [400.0, 128.0, 100.0]},
			    "transition": {"playerPosition": [10.0, 64.0, 0.0], "yaw": 0.0, "pitch": 0.0}}]}]}
			""");
		IllegalArgumentException unsafeError = assertThrows(IllegalArgumentException.class,
			() -> AuthoredCourseStoreJson.load(unsafe));
		assertTrue(unsafeError.getMessage().contains("tee"));

		// Hand-edited finalized course with a numbering gap.
		Path gapped = directory.resolve("gapped.json");
		Files.writeString(gapped, """
			{"drafts": [], "finalized": [{
			  "id": "links", "displayName": "Links", "dimension": "minecraft:overworld",
			  "holes": [
			    {"number": 2, "tee": [10.0, 64.0, 0.0], "cup": [40.0, 64.0, 0.0],
			     "par": 4, "boundary": {"min": [-100.0, 0.0, -100.0], "max": [400.0, 128.0, 100.0]},
			     "transition": {"playerPosition": [10.0, 64.0, 0.0], "yaw": 0.0, "pitch": 0.0}}]}]}
			""");
		assertThrows(IllegalArgumentException.class, () -> AuthoredCourseStoreJson.load(gapped));

		// Duplicate course ids across drafts and finalized.
		Path duplicate = directory.resolve("duplicate.json");
		Files.writeString(duplicate, """
			{"drafts": [{"id": "links", "displayName": "Draft", "dimension": "minecraft:overworld"}],
			  "finalized": [{"id": "links", "displayName": "Final", "dimension": "minecraft:overworld",
			    "holes": [{"number": 1, "tee": [10.0, 64.0, 0.0], "cup": [40.0, 64.0, 0.0],
			      "par": 4, "boundary": {"min": [-100.0, 0.0, -100.0], "max": [400.0, 128.0, 100.0]},
			      "transition": {"playerPosition": [10.0, 64.0, 0.0], "yaw": 0.0, "pitch": 0.0}}]}]}
			""");
		assertThrows(IllegalArgumentException.class, () -> AuthoredCourseStoreJson.load(duplicate));
	}

	private static void completeHole(AuthoredCourseStore store, String courseId, int number, int par) {
		store.setHoleTee(courseId, number, tee(number - 1));
		store.setHoleCup(courseId, number, new Vec3((number - 1) * 50.0 + 40.0, 64.0, 0.0));
		store.setHolePar(courseId, number, par);
		store.setHoleBounds(courseId, number,
			new HoleBoundary(new Vec3(-100.0, 0.0, -100.0), new Vec3(400.0, 128.0, 100.0)));
	}

	private static Vec3 tee(int offset) {
		return new Vec3(offset * 50.0 + 10.0, 64.0, 0.0);
	}
}
