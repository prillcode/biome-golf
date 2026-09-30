package pro.apdev.biomegolf.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * M4.5 regression: starting a hole must equip missing clubs, not drop them at the
 * player's feet when the inventory is full. The automatic grant mirrors the
 * {@code /golf clubs equip} placement instead of calling {@code inventory.add} then
 * dropping whatever did not fit.
 */
class ClubGrantRegressionTest {

	private static final Path SERVICE = Path.of(
		"src/main/java/pro/apdev/biomegolf/server/ActiveHoleService.java");

	@Test
	void automaticGrantReusesTheEquipPlacementAndNeverDrops() throws IOException {
		String source = Files.readString(SERVICE);
		String body = methodBody(source, "grantMissingClubs");
		assertTrue(body.contains("resetClubSet(player)"),
			"grantMissingClubs must reuse the equip placement path");
		assertFalse(body.contains(".drop("),
			"grantMissingClubs must not drop clubs when the inventory is full");
	}

	@Test
	void equipCommandUsesTheSharedPlacement() throws IOException {
		String source = Files.readString(SERVICE);
		assertTrue(methodBody(source, "equipClubs").contains("resetClubSet(player)"));
	}

	@Test
	void onlyTheSharedPlacementDropsDisplacedItems() throws IOException {
		String source = Files.readString(SERVICE);
		assertTrue(methodBody(source, "resetClubSet").contains("player.drop("),
			"resetClubSet is where genuinely-full displacement drops");
		assertFalse(methodBody(source, "grantMissingClubs").contains("player.getInventory().add("),
			"the old add-then-drop grant path must be gone");
	}

	private static String methodBody(String source, String name) {
		int signature = source.indexOf(" " + name + "(ServerPlayer");
		if (signature < 0) {
			throw new AssertionError("method not found: " + name);
		}
		int brace = source.indexOf('{', signature);
		int depth = 0;
		for (int index = brace; index < source.length(); index++) {
			char current = source.charAt(index);
			if (current == '{') {
				depth++;
			} else if (current == '}') {
				depth--;
				if (depth == 0) {
					return source.substring(brace, index + 1);
				}
			}
		}
		throw new AssertionError("unterminated method: " + name);
	}
}
