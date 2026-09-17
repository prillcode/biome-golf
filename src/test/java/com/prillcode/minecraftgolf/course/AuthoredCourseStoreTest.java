package com.prillcode.minecraftgolf.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;

class AuthoredCourseStoreTest {

	private static final String DIMENSION = "minecraft:overworld";

	@Test
	void createDraftFinalizeHappyPath() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		String id = store.createCourse("Sunny_Meadows", "Sunny Meadows", " Minecraft:Overworld ");
		assertEquals("sunny_meadows", id);

		completeHole(store, id, 1, 4);
		completeHole(store, id, 2, 3);

		assertTrue(store.isDraft(id));
		assertFalse(store.isFinalized(id));

		CourseDefinition course = store.finalize(id);

		assertTrue(store.isFinalized(id));
		assertFalse(store.isDraft(id));
		assertEquals("sunny_meadows", course.id());
		assertEquals("Sunny Meadows", course.displayName());
		assertEquals(DIMENSION, course.dimension());
		assertEquals(new GeneratedLayoutIdentity("authored:sunny_meadows", 1), course.generatedLayout());
		assertEquals(2, course.holes().size());
		assertEquals("sunny_meadows:hole_1", course.hole(1).id());
		assertEquals("sunny_meadows:hole_2", course.hole(2).id());
		assertEquals(7, course.totalPar());
		assertEquals(course, store.finalizedCourse(id));
	}

	@Test
	void rejectsDuplicateCourseIdsAcrossDraftsAndFinalized() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("Links", "Links", DIMENSION);
		assertThrows(IllegalStateException.class,
			() -> store.createCourse("links", "Links Again", DIMENSION));

		completeHole(store, "links", 1, 4);
		store.finalize("links");
		assertThrows(IllegalStateException.class,
			() -> store.createCourse("LINKS", "Links Once More", DIMENSION));
	}

	@Test
	void rejectsInvalidIdsNamesAndDimensions() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		assertThrows(IllegalArgumentException.class, () -> store.createCourse("  ", "Name", DIMENSION));
		assertThrows(IllegalArgumentException.class,
			() -> store.createCourse("has space", "Name", DIMENSION));
		assertThrows(IllegalArgumentException.class,
			() -> store.createCourse("bad!id", "Name", DIMENSION));
		assertThrows(IllegalArgumentException.class, () -> store.createCourse("ok", " ", DIMENSION));
		assertThrows(IllegalArgumentException.class, () -> store.createCourse("ok", "Name", " "));
		assertThrows(IllegalArgumentException.class, () -> store.createCourse("ok", "Name", "overworld"));
		assertThrows(IllegalArgumentException.class,
			() -> store.createCourse("ok", "Name", "minecraft:over world"));
	}

	@Test
	void holeDraftsAreIncrementalAndReplaceable() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("links", "Links", DIMENSION);
		store.setHolePar("links", 1, 4);
		store.setHoleTee("links", 1, tee(0));

		AuthoredCourseStore.HoleSnapshot snapshot = store.draftSnapshot("links").holes().getFirst();
		assertEquals(1, snapshot.number());
		assertEquals(4, snapshot.par());
		assertEquals(tee(0), snapshot.tee());
		assertEquals(null, snapshot.cup());

		// Replacing a value overwrites the previous one.
		store.setHolePar("links", 1, 5);
		assertEquals(5, store.draftSnapshot("links").holes().getFirst().par());
	}

	@Test
	void rejectsUnknownCoursesAndInvalidHoleInput() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		assertThrows(IllegalArgumentException.class, () -> store.setHolePar("missing", 1, 4));
		assertThrows(IllegalArgumentException.class, () -> store.draftSnapshot("missing"));
		assertThrows(IllegalArgumentException.class, () -> store.finalize("missing"));
		assertThrows(IllegalArgumentException.class, () -> store.finalizedCourse("missing"));

		store.createCourse("links", "Links", DIMENSION);
		assertThrows(IllegalArgumentException.class, () -> store.setHolePar("links", 0, 4));
		assertThrows(IllegalArgumentException.class, () -> store.setHolePar("links", 1, 0));
		assertThrows(IllegalArgumentException.class, () -> store.setHolePar("links", 1, -2));
	}

	@Test
	void finalizeRejectsEmptyGappedAndIncompleteDrafts() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("empty", "Empty", DIMENSION);
		assertThrows(IllegalStateException.class, () -> store.finalize("empty"));

		store.createCourse("gapped", "Gapped", DIMENSION);
		completeHole(store, "gapped", 1, 4);
		completeHole(store, "gapped", 3, 5);
		assertThrows(IllegalStateException.class, () -> store.finalize("gapped"));

		store.createCourse("incomplete", "Incomplete", DIMENSION);
		completeHole(store, "incomplete", 1, 4);
		store.setHolePar("incomplete", 2, 3);
		IllegalStateException error = assertThrows(IllegalStateException.class,
			() -> store.finalize("incomplete"));
		assertTrue(error.getMessage().contains("tee"));
		assertTrue(error.getMessage().contains("cup"));
		assertFalse(error.getMessage().contains("par"));
	}

	@Test
	void finalizeRejectsUnsafeHolesThroughDomainInvariants() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("unsafe", "Unsafe", DIMENSION);
		completeHole(store, "unsafe", 1, 4);
		store.setHoleTee("unsafe", 1, new Vec3(500.0, 64.0, 0.0)); // outside boundary
		assertThrows(IllegalArgumentException.class, () -> store.finalize("unsafe"));

		AuthoredCourseStore cupStore = new AuthoredCourseStore();
		cupStore.createCourse("unsafe", "Unsafe", DIMENSION);
		completeHole(cupStore, "unsafe", 1, 4);
		cupStore.setHoleCup("unsafe", 1, new Vec3(500.0, 64.0, 0.0));
		assertThrows(IllegalArgumentException.class, () -> cupStore.finalize("unsafe"));
	}

	@Test
	void draftsAreNotExposedAsPlayableCourses() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("links", "Links", DIMENSION);
		completeHole(store, "links", 1, 4);
		assertThrows(IllegalArgumentException.class, () -> store.finalizedCourse("links"));
		assertEquals(0, store.finalizedCourses().size());
	}

	@Test
	void finalizeConsumesDraftAndDefaultsTransition() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("links", "Links", DIMENSION);
		completeHole(store, "links", 1, 4);

		CourseDefinition course = store.finalize("links");
		assertEquals(HoleTransition.at(tee(0)), course.hole(1).transition());
		// The draft is gone; hole edits now fail with a clear message.
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
			() -> store.setHolePar("links", 1, 3));
		assertTrue(error.getMessage().contains("finalized"));
	}

	@Test
	void finalizeAllowsHoleWithoutBoundsAndUsesUnboundedBoundary() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("open", "Open", DIMENSION);
		store.setHoleTee("open", 1, tee(0));
		store.setHoleCup("open", 1, cup(0));
		store.setHolePar("open", 1, 4);

		CourseDefinition course = store.finalize("open");

		assertTrue(course.hole(1).boundary().contains(new Vec3(1_000_000.0, 320.0, -1_000_000.0)));
	}

	@Test
	void renameUpdatesDraftsAndFinalizedCourses() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("drafty", "Old Draft", DIMENSION);
		store.renameCourse("drafty", "New Draft");
		assertEquals("New Draft", store.draftSnapshot("drafty").displayName());

		store.createCourse("links", "Old Links", DIMENSION);
		completeHole(store, "links", 1, 4);
		store.finalize("links");
		store.renameCourse("links", "New Links");
		assertEquals("New Links", store.finalizedCourse("links").displayName());
		assertEquals("links", store.finalizedCourse("links").id());

		assertThrows(IllegalArgumentException.class, () -> store.renameCourse("missing", "Name"));
		assertThrows(IllegalArgumentException.class, () -> store.renameCourse("links", " "));
	}

	@Test
	void cloneFinalizedCourseCreatesIndependentDraft() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("original", "Original", DIMENSION);
		completeHole(store, "original", 1, 4);
		store.finalize("original");

		String cloneId = store.cloneCourse("original", "variant", "Variant");
		assertEquals("variant", cloneId);
		assertTrue(store.isDraft(cloneId));
		assertEquals("Variant", store.draftSnapshot(cloneId).displayName());
		assertEquals(store.finalizedCourse("original").hole(1).tee(),
			store.draftSnapshot(cloneId).holes().getFirst().tee());

		store.setHolePar(cloneId, 1, 5);
		assertEquals(4, store.finalizedCourse("original").hole(1).par());
		assertEquals("variant:hole_1", store.finalize(cloneId).hole(1).id());
	}

	@Test
	void cloneDraftPreservesIncompleteMetadataAndRejectsCollisions() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("draft", "Draft", DIMENSION);
		store.setHoleTee("draft", 1, tee(0));

		store.cloneCourse("draft", "copy", null);
		AuthoredCourseStore.HoleSnapshot hole = store.draftSnapshot("copy").holes().getFirst();
		assertEquals("Draft Copy", store.draftSnapshot("copy").displayName());
		assertEquals(tee(0), hole.tee());
		assertEquals(null, hole.cup());
		assertThrows(IllegalStateException.class, () -> store.cloneCourse("draft", "COPY", null));
		assertThrows(IllegalArgumentException.class, () -> store.cloneCourse("missing", "other", null));
	}

	@Test
	void supportsMultipleCoursesAndSingleHoleCourses() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("one", "One", DIMENSION);
		store.createCourse("two", "Two", "minecraft:the_nether");
		completeHole(store, "one", 1, 4);
		completeHole(store, "two", 1, 3);
		completeHole(store, "two", 2, 3);

		assertEquals(1, store.finalize("one").holes().size());
		CourseDefinition two = store.finalize("two");
		assertEquals(2, two.holes().size());
		assertEquals("minecraft:the_nether", two.hole(2).dimension());
		assertEquals(2, store.finalizedCourses().size());
	}

	@Test
	void defaultCourseMustBeFinalizedAndCannotBeDeleted() {
		AuthoredCourseStore store = new AuthoredCourseStore();
		store.createCourse("links", "Links", DIMENSION);
		completeHole(store, "links", 1, 4);

		assertThrows(IllegalArgumentException.class, () -> store.setDefaultCourse("links"));
		store.finalize("links");
		store.setDefaultCourse("LINKS");

		assertEquals("links", store.defaultCourseId().orElseThrow());
		assertThrows(IllegalStateException.class, () -> store.removeCourse("links"));
		store.clearDefaultCourse();
		store.removeCourse("links");
		assertTrue(store.defaultCourseId().isEmpty());
	}

	private static void completeHole(AuthoredCourseStore store, String courseId, int number, int par) {
		store.setHoleTee(courseId, number, tee(number - 1));
		store.setHoleCup(courseId, number, cup(number - 1));
		store.setHolePar(courseId, number, par);
		store.setHoleBounds(courseId, number, boundary());
	}

	private static Vec3 tee(int offset) {
		return new Vec3(offset * 50.0 + 10.0, 64.0, 0.0);
	}

	private static Vec3 cup(int offset) {
		return new Vec3(offset * 50.0 + 40.0, 64.0, 0.0);
	}

	private static HoleBoundary boundary() {
		return new HoleBoundary(new Vec3(-100.0, 0.0, -100.0), new Vec3(400.0, 128.0, 100.0));
	}
}
