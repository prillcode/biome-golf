package pro.apdev.biomegolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GolfMenuActionPayloadTest {
	@Test
	void menuActionsAreTypedAndRoundTripByOrdinal() {
		for (GolfMenuActionPayload.Action action : GolfMenuActionPayload.Action.values()) {
			assertEquals(action, new GolfMenuActionPayload(action).action());
		}
	}

	@Test
	void nullActionIsRejectedBeforeNetworking() {
		assertThrows(NullPointerException.class, () -> new GolfMenuActionPayload(null));
	}
}
