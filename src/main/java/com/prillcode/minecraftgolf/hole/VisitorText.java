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

	/** Welcome chat lines: the two view modes plus the "join on Java to golf" invite. */
	public static List<String> welcome(String address, String link) {
		List<String> lines = new ArrayList<>();
		lines.add("[golf] Welcome to BirdieBiome! Golf is a Java mod experience, so you can watch and join the world here, but golf itself is on Java.");
		lines.add("[golf] Watch: /golf spectator. Join the world: /golf spectator leave.");
		lines.add("[golf] You spawn inside protected golf course bounds — travel off golf course property to break or build.");
		if (notBlank(address)) {
			lines.add("[golf] Want to golf? Join on Java: " + address);
		} else {
			lines.add("[golf] Want to golf? Ask the host for the Java server address.");
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
