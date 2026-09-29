package pro.apdev.biomegolf.hole;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure text for the Bedrock/vanilla visitor experience. Visitors join in survival/peaceful by
 * default and may golf only on Java; the "join on Java" promo is shown when they opt into
 * spectator mode. Kept free of Minecraft types so it stays unit-testable; the server-side
 * {@code VisitorService} owns delivery and the gamemode changes.
 */
public final class VisitorText {

	private VisitorText() {
	}

	/** One-time welcome shown on join (visitors join in survival/peaceful by default). */
	public static List<String> welcome() {
		List<String> lines = new ArrayList<>();
		lines.add("[golf] Welcome to BirdieBiome! Golf needs the Biome Golf mod on this server's "
			+ "version — Java players, install or update it; vanilla or Bedrock clients can build and explore freely here.");
		lines.add("[golf] You spawn inside protected golf course bounds — travel off golf course property to break or build.");
		lines.add("[golf] Watch a round: /golf spectator.");
		return lines;
	}

	/** Promo shown when a visitor enters spectator mode: the "join on Java to golf" invite. */
	public static List<String> spectatorPromo(String address, String link) {
		List<String> lines = new ArrayList<>();
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

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}
}
