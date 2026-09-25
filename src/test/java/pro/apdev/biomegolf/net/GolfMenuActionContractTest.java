package pro.apdev.biomegolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GolfMenuActionContractTest {
	@Test
	void exposesAllContextualRoundActions() {
		assertEquals(5, GolfMenuActionPayload.Action.values().length);
		assertEquals(GolfMenuActionPayload.Action.START_ROUND,
			GolfMenuActionPayload.Action.valueOf("START_ROUND"));
		assertEquals(GolfMenuActionPayload.Action.LEAVE_ROUND,
			GolfMenuActionPayload.Action.valueOf("LEAVE_ROUND"));
		assertThrows(IllegalArgumentException.class,
			() -> GolfMenuActionPayload.Action.valueOf("ABANDON_ROUND"));
		assertThrows(IllegalArgumentException.class,
			() -> GolfMenuActionPayload.Action.valueOf("DONE"));
	}
}
