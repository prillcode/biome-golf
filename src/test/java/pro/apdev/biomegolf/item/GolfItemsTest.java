package pro.apdev.biomegolf.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.club.ClubDefinition;
import pro.apdev.biomegolf.club.GolfClubs;

/**
 * M10 club representation. Real {@code ItemStack}s need the in-game data-component
 * initializer, so this covers the pure identification scheme the fallback
 * representation relies on; the stacks themselves are verified in-game.
 */
class GolfItemsTest {

	@Test
	void everyClubTagRoundTripsToItsClubId() {
		for (ClubDefinition club : GolfClubs.ALL) {
			String recovered = GolfItems.clubIdFromTag(GolfItems.clubTag(club.id()));
			assertEquals(club.id(), recovered);
			assertTrue(GolfClubs.byId(recovered).isPresent(), "recoverable id must resolve");
		}
	}

	@Test
	void absentOrEmptyTagsDoNotResolveToAClub() {
		assertEquals("", GolfItems.clubIdFromTag(null));
		assertEquals("", GolfItems.clubIdFromTag(new CompoundTag()));
		assertTrue(GolfClubs.byId("").isEmpty());
		assertTrue(GolfClubs.byId("not_a_club").isEmpty());
	}
}
