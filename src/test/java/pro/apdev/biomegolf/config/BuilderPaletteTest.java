package pro.apdev.biomegolf.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** M8.15 pure validation of the operator-editable builder palette config. */
class BuilderPaletteTest {

	@Test
	void defaultsAreValidAndCoverLandscaping() {
		BuilderPalette palette = BuilderPalette.defaults();
		assertEquals(BuilderPalette.CURRENT_SCHEMA, palette.schemaVersion());
		assertTrue(palette.items().contains("minecraft:lime_wool"));
		assertTrue(palette.items().contains("minecraft:green_wool"));
		assertTrue(palette.items().contains("minecraft:moss_carpet"));
		// The agreed pale fence/button are pale oak, not birch.
		assertTrue(palette.items().contains("minecraft:pale_oak_fence"));
		assertTrue(palette.items().contains("minecraft:pale_oak_button"));
		assertFalse(palette.items().contains("minecraft:birch_fence"));
		assertFalse(palette.items().contains("minecraft:oak_button"));
		// Landscape essentials requested for course building.
		assertTrue(palette.items().contains("minecraft:sand"));
		assertTrue(palette.items().contains("minecraft:stone"));
		assertTrue(palette.items().contains("minecraft:oak_leaves"));
		assertTrue(palette.items().contains("minecraft:oxeye_daisy"));
		// One slot per item so a one-of-each loadout always fits.
		assertEquals(BuilderPalette.MAX_ITEMS, palette.items().size());
	}

	@Test
	void duplicateItemsAreDedupedPreservingOrder() {
		BuilderPalette palette = new BuilderPalette(1,
			List.of("minecraft:lime_wool", "minecraft:oak_button", "minecraft:lime_wool"));
		assertEquals(List.of("minecraft:lime_wool", "minecraft:oak_button"), palette.items());
	}

	@Test
	void rejectsEmptyPalette() {
		assertThrows(IllegalArgumentException.class, () -> new BuilderPalette(1, List.of()));
	}

	@Test
	void rejectsUnsupportedSchema() {
		assertThrows(IllegalArgumentException.class,
			() -> new BuilderPalette(2, List.of("minecraft:lime_wool")));
	}

	@Test
	void rejectsMalformedItemId() {
		assertThrows(IllegalArgumentException.class,
			() -> new BuilderPalette(1, List.of("lime_wool")));
		assertThrows(IllegalArgumentException.class,
			() -> new BuilderPalette(1, List.of("minecraft:Lime_Wool")));
		assertThrows(IllegalArgumentException.class,
			() -> new BuilderPalette(1, List.of("minecraft:")));
	}

	@Test
	void rejectsOversizedPalette() {
		List<String> tooMany = new ArrayList<>();
		for (int index = 0; index <= BuilderPalette.MAX_ITEMS; index++) {
			tooMany.add("minecraft:item_" + index);
		}
		assertThrows(IllegalArgumentException.class, () -> new BuilderPalette(1, tooMany));
	}
}
