package pro.apdev.biomegolf.golf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CupDirectionTest {

	@Test
	void mapsCupBearingRelativeToMinecraftYaw() {
		assertEquals("↑", CupDirection.arrow(0, 0, 0, 0, 10));
		assertEquals("→", CupDirection.arrow(0, 0, 0, -10, 0));
		assertEquals("←", CupDirection.arrow(0, 0, 0, 10, 0));
		assertEquals("↓", CupDirection.arrow(0, 0, 0, 0, -10));
		assertEquals("↑", CupDirection.arrow(0, 0, 90, -10, 0));
	}
}
