package com.prillcode.minecraftgolf.hole;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure text for the Bedrock/vanilla visitor experience (a promotional watch mode; visitors do
 * not play). Kept free of Minecraft types so it stays unit-testable; the server-side
 * {@code VisitorService} owns delivery and the gamemode changes.
 */
public final class VisitorText {

	private VisitorText() {
	}

	/** Welcome chat lines: the two watch modes plus the "join on Java to play" invite. */
	public static List<String> welcome(String address, String link) {
		List<String> lines = new ArrayList<>();
		lines.add("[golf] Welcome to BirdieBiome! This is a Java mod experience, so you can watch but not play here.");
		lines.add("[golf] Fly and watch: /golf spectator. Walk around (visible): /golf adventure.");
		if (notBlank(address)) {
			lines.add("[golf] Want to play? Join on Java: " + address);
		} else {
			lines.add("[golf] Want to play? Ask the host for the Java server address.");
		}
		if (notBlank(link)) {
			lines.add("[golf] Install the Java mod: " + link);
		}
		return lines;
	}

	/** One-line action-bar reminder shown periodically while a visitor watches. */
	public static String reminder(String address) {
		return notBlank(address)
			? "[golf] Spectating BirdieBiome — play on Java: " + address
			: "[golf] Spectating BirdieBiome — play on Java for the full game";
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}
}
