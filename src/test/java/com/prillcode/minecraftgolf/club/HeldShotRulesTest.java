package com.prillcode.minecraftgolf.club;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeldShotRulesTest {

	@Test
	void quickTapStillProducesMinimumPower() {
		assertEquals(HeldShotRules.MIN_POWER, HeldShotRules.power(0), 0.0001f);
		assertEquals(HeldShotRules.MIN_POWER, HeldShotRules.power(1), 0.0001f);
	}

	@Test
	void powerScalesWithHoldAndClampsAtFull() {
		assertEquals(0.5f, HeldShotRules.power(HeldShotRules.CHARGE_TICKS / 2), 0.0001f);
		assertEquals(1.0f, HeldShotRules.power(HeldShotRules.CHARGE_TICKS), 0.0001f);
		assertEquals(1.0f, HeldShotRules.power(HeldShotRules.CHARGE_TICKS * 3), 0.0001f);
	}

	@Test
	void autoFireIsAReservedSafetyValve() {
		assertFalse(HeldShotRules.shouldAutoFire(HeldShotRules.MAX_HOLD_TICKS - 1));
		assertTrue(HeldShotRules.shouldAutoFire(HeldShotRules.MAX_HOLD_TICKS));
	}
}
