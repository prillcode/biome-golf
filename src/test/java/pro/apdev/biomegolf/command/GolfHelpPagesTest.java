package pro.apdev.biomegolf.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class GolfHelpPagesTest {

	private static final List<String> TOP_LEVEL_GOLF_LITERALS = List.of(
		"help", "spectator", "visitor", "round", "browse", "hole", "clubs", "practice",
		"pickup", "tapin", "swing", "nexthole", "course", "spawn", "launch", "inspect", "dev", "hud",
		"mode", "builder");

	@Test
	void everyTopLevelGolfLiteralAppearsInPlayerOrAdminHelp() {
		String help = Stream.concat(GolfHelpPages.PLAYER.stream(), GolfHelpPages.ADMIN.stream())
			.collect(Collectors.joining("\n"));

		for (String literal : TOP_LEVEL_GOLF_LITERALS) {
			assertTrue(help.contains("/golf " + literal),
				"Missing top-level /golf " + literal + " from both help pages");
		}
	}
}
