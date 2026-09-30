package pro.apdev.biomegolf.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Source-boundary regression: a partial client /golf root breaks server command routing. */
class GolfCommandRoutingTest {

	@Test
	void clientDoesNotRegisterAShadowGolfCommandRoot() throws IOException {
		try (var sources = Files.walk(Path.of("src/client/java"))) {
			for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
				String text = Files.readString(source);
				assertFalse(text.matches("(?s).*\\.literal\\s*\\(\\s*\"golf\"\\s*\\).*"),
					"Keep /golf server-owned; a partial client root shadows B and other commands: " + source);
			}
		}
	}

	@Test
	void hudCommandUsesServerTransportAndClientRetainsVisibility() throws IOException {
		String server = Files.readString(Path.of(
			"src/main/java/pro/apdev/biomegolf/command/GolfHoleCommands.java"));
		assertTrue(server.contains("Commands.literal(\"hud\").executes(GolfHoleCommands::toggleHud)"));
		assertTrue(server.contains("HudVisibilityNetworking.toggle(player)"));
		String client = Files.readString(Path.of(
			"src/client/java/pro/apdev/biomegolf/client/MinecraftGolfClient.java"));
		assertTrue(client.contains("registerGlobalReceiver(ToggleHudPayload.TYPE"));
		assertTrue(client.contains("HudVisibility.toggle()"));
	}
}
