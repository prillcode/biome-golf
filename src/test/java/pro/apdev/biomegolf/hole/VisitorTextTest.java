package pro.apdev.biomegolf.hole;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class VisitorTextTest {

	@Test
	void welcomePointsAtSpectatorAndTheOffCourseBuildRule() {
		List<String> lines = VisitorText.welcome();

		assertTrue(lines.stream().anyMatch(line -> line.contains("/golf spectator")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("off golf course property")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("Java")));
	}

	@Test
	void welcomeDoesNotCarryThePromoAddress() {
		List<String> lines = VisitorText.welcome();

		assertFalse(lines.stream().anyMatch(line -> line.contains("Join on Java:")));
	}

	@Test
	void spectatorPromoIncludesTheAddressAndLink() {
		List<String> lines = VisitorText.spectatorPromo("golf.example.com", "https://example.com/mod");

		assertTrue(lines.stream().anyMatch(line -> line.contains("golf.example.com")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("https://example.com/mod")));
	}

	@Test
	void spectatorPromoAsksTheHostWhenNoAddressIsConfigured() {
		List<String> lines = VisitorText.spectatorPromo("", null);

		assertTrue(lines.stream().anyMatch(line -> line.toLowerCase().contains("ask the host")));
		assertFalse(lines.stream().anyMatch(line -> line.contains("https://")));
	}
}
