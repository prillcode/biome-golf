package pro.apdev.biomegolf.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pro.apdev.biomegolf.course.AuthoredCourseStore;
import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.hole.HoleBoundary;

/** Plain-JVM tests for the Minecraft-free persistence core of {@link AuthoredCourseService}. */
class AuthoredCourseServiceTest {

	@TempDir
	Path tempDir;

	@Test
	void loadMissingFileYieldsEmptyStore() {
		AuthoredCourseStore store = AuthoredCourseService.loadOrEmpty(tempDir.resolve("missing.json"));
		assertTrue(store.draftSnapshots().isEmpty());
		assertTrue(store.finalizedCourses().isEmpty());
	}

	@Test
	void loadMalformedFileFailsClosedWithoutThrowing() throws Exception {
		Path path = tempDir.resolve("store.json");
		Files.writeString(path, "{ this is not valid json !!!");
		AuthoredCourseStore store = assertDoesNotThrow(() -> AuthoredCourseService.loadOrEmpty(path));
		assertTrue(store.draftSnapshots().isEmpty());
		assertTrue(store.finalizedCourses().isEmpty());
	}

	@Test
	void loadTamperedContentFailsClosedWithoutThrowing() throws Exception {
		Path path = tempDir.resolve("store.json");
		Files.writeString(path, "{\"drafts\": [{\"id\": \"BAD ID\", \"displayName\": \"x\","
			+ " \"dimension\": \"minecraft:overworld\"}], \"finalized\": []}");
		AuthoredCourseStore store = assertDoesNotThrow(() -> AuthoredCourseService.loadOrEmpty(path));
		assertTrue(store.draftSnapshots().isEmpty());
	}

	@Test
	void saveThenLoadRoundTripsDraftsAndFinalizedCourses() {
		Path path = tempDir.resolve("store.json");
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("cliffside", "Cliffside", "minecraft:overworld");
		store.setHoleTee("cliffside", 1, new Vec3(1.5, 65.25, 2.5));
		store.setHoleCup("cliffside", 1, new Vec3(30.5, 64.25, 40.5));
		store.setHolePar("cliffside", 1, 4);
		store.setHoleBounds("cliffside", 1,
			new HoleBoundary(new Vec3(0, -64, 0), new Vec3(64, 320, 64)));
		store.finalize("cliffside");
		store.createCourse("wip", "Work In Progress", "minecraft:overworld");
		store.setHolePar("wip", 1, 3);

		AuthoredCourseService.saveQuietly(store, path);
		AuthoredCourseStore loaded = AuthoredCourseService.loadOrEmpty(path);

		assertTrue(loaded.isFinalized("cliffside"));
		assertEquals(1, loaded.finalizedCourse("cliffside").holes().size());
		assertEquals(4, loaded.finalizedCourse("cliffside").hole(1).par());
		assertTrue(loaded.isDraft("wip"));
		assertEquals(3, loaded.draftSnapshot("wip").holes().getFirst().par());
	}

	@Test
	void saveAfterMutationPersistsEachChange() {
		Path path = tempDir.resolve("store.json");
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("links", "Links", "minecraft:overworld");
		AuthoredCourseService.saveQuietly(store, path);
		assertTrue(AuthoredCourseService.loadOrEmpty(path).isDraft("links"));

		store.setHolePar("links", 1, 5);
		AuthoredCourseService.saveQuietly(store, path);
		assertEquals(5, AuthoredCourseService.loadOrEmpty(path)
			.draftSnapshot("links").holes().getFirst().par());
	}

	@Test
	void onlyFinalizedCoursesAreSelectable() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("draft", "Draft", "minecraft:overworld");

		assertThrows(IllegalArgumentException.class, () -> store.finalizedCourse("draft"));
		assertThrows(IllegalArgumentException.class, () -> store.finalizedCourse("missing"));

		store.setHoleTee("draft", 1, new Vec3(1.5, 65.25, 2.5));
		store.setHoleCup("draft", 1, new Vec3(30.5, 64.25, 40.5));
		store.setHolePar("draft", 1, 4);
		assertEquals("draft", store.finalize("draft").id());
		assertEquals(1, store.finalizedCourse("draft").holes().size());
	}
}
