package pro.apdev.biomegolf.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModIdTest {
	@Test
	void acceptsValidModIds() {
		assertTrue(ModId.isValidModId("minecraft_golf"));
		assertTrue(ModId.isValidModId("ab"));
		assertTrue(ModId.isValidModId("fabric-api"));
		assertTrue(ModId.isValidModId("a1_b2-c3"));
	}

	@Test
	void rejectsInvalidModIds() {
		assertFalse(ModId.isValidModId(null));
		assertFalse(ModId.isValidModId(""));
		assertFalse(ModId.isValidModId("a"));
		assertFalse(ModId.isValidModId("1abc"));
		assertFalse(ModId.isValidModId("-abc"));
		assertFalse(ModId.isValidModId("has space"));
		assertFalse(ModId.isValidModId("Upper"));
		assertFalse(ModId.isValidModId("a".repeat(65)));
	}
}
