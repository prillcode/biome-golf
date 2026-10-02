package pro.apdev.biomegolf.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClearItemsTest {

	@Test
	void defaultsProvideEveryBuiltInGroup() {
		ClearItems items = ClearItems.defaults();
		assertEquals(ClearItems.CURRENT_SCHEMA, items.schemaVersion());
		assertTrue(items.groups().containsKey("trees"));
		assertTrue(items.groups().containsKey("logs"));
		assertTrue(items.groups().containsKey("leaves"));
		assertTrue(items.groups().containsKey("ground"));
		assertTrue(items.groups().containsKey("grass"));
		assertTrue(items.groups().get("trees").contains("#minecraft:logs"));
		assertTrue(items.groups().get("trees").contains("#minecraft:leaves"));
		// grass_block is the common surface cap and must be in ground (it is not in #minecraft:dirt).
		assertTrue(items.groups().get("ground").contains("minecraft:grass_block"));
		// grass targets the upright plants (the plain grass family includes ferns).
		assertTrue(items.groups().get("grass").contains("minecraft:short_grass"));
		assertTrue(items.groups().get("grass").contains("minecraft:tall_grass"));
		assertTrue(items.groups().get("grass").contains("minecraft:fern"));
		assertTrue(items.groups().get("grass").contains("minecraft:large_fern"));
	}

	@Test
	void parsesValidJson() {
		ClearItems items = ClearItems.parse("""
			{"schemaVersion": 1, "items": {
				"trees": ["#minecraft:logs", "minecraft:oak_leaves"],
				"stone": ["minecraft:stone"]}}""");
		assertEquals(2, items.groups().size());
		assertEquals(java.util.List.of("#minecraft:logs", "minecraft:oak_leaves"),
			items.groups().get("trees"));
		assertTrue(items.groups().get("stone").contains("minecraft:stone"));
	}

	@Test
	void rejectsNonObjectRoot() {
		assertThrows(IllegalArgumentException.class, () -> ClearItems.parse("[]"));
	}

	@Test
	void rejectsMissingItemsObject() {
		assertThrows(IllegalArgumentException.class, () -> ClearItems.parse("{\"schemaVersion\": 1}"));
	}

	@Test
	void rejectsNonArrayGroup() {
		assertThrows(IllegalArgumentException.class,
			() -> ClearItems.parse("{\"schemaVersion\": 1, \"items\": {\"trees\": \"#minecraft:logs\"}}"));
	}

	@Test
	void rejectsBlankGroupNameAndEmptyGroup() {
		assertThrows(IllegalArgumentException.class,
			() -> new ClearItems(1, java.util.Map.of(" ", java.util.List.of("minecraft:stone"))));
		assertThrows(IllegalArgumentException.class,
			() -> new ClearItems(1, java.util.Map.of("stone", java.util.List.of())));
	}

	@Test
	void rejectsInvalidSchemaVersion() {
		assertThrows(IllegalArgumentException.class,
			() -> new ClearItems(0, java.util.Map.of("stone", java.util.List.of("minecraft:stone"))));
	}

	@Test
	void roundTripsThroughToJson() {
		ClearItems items = ClearItems.defaults();
		ClearItems roundTripped = ClearItems.parse(ClearItems.toJson(items));
		assertEquals(items.groups(), roundTripped.groups());
	}
}