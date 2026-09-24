package com.prillcode.minecraftgolf.hole;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class VisitorTextTest {

	@Test
	void welcomeIncludesBothWatchModesAndTheJavaInvite() {
		List<String> lines = VisitorText.welcome("golf.example.com", "https://example.com/mod");

		assertTrue(lines.stream().anyMatch(line -> line.contains("/golf spectator")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("/golf adventure")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("golf.example.com")));
		assertTrue(lines.stream().anyMatch(line -> line.contains("https://example.com/mod")));
	}

	@Test
	void welcomeAsksTheHostWhenNoAddressIsConfigured() {
		List<String> lines = VisitorText.welcome("", null);

		assertTrue(lines.stream().anyMatch(line -> line.toLowerCase().contains("ask the host")));
		assertFalse(lines.stream().anyMatch(line -> line.contains("https://")));
	}

	@Test
	void reminderAdvertisesTheAddressWhenSet() {
		assertTrue(VisitorText.reminder("golf.example.com").contains("golf.example.com"));
	}

	@Test
	void reminderFallsBackWhenNoAddressIsSet() {
		assertTrue(VisitorText.reminder(null).contains("play on Java"));
	}
}
