package pro.apdev.biomegolf.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The visitor rejection must read correctly for a Java client on an older mod version,
 * which fails the same {@code !canSend(HoleStatePayload)} gate as a vanilla/Bedrock client.
 */
class VisitorServiceTextTest {

	@Test
	void playRejectionCoversOutdatedJavaModsNotJustBedrock() {
		String message = VisitorService.playRejection();

		assertTrue(message.contains("install or update"),
			"should tell a Java player to install or update the mod: " + message);
		assertFalse(message.contains("Java-only"),
			"must not imply the player is on Bedrock: " + message);
	}
}
