package com.prillcode.minecraftgolf.club;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeldShotRulesTest {

	@Test
	void aSingleTapStillProducesMinimumPower() {
		assertEquals(HeldShotRules.MIN_POWER, HeldShotRules.powerForTaps(0), 0.0001f);
		assertEquals(HeldShotRules.MIN_POWER, HeldShotRules.powerForTaps(1), 0.0001f);
	}

	@Test
	void powerScalesWithTapsAndClampsAtFull() {
		assertEquals(0.5f, HeldShotRules.powerForTaps(2), 0.0001f);
		assertEquals(0.75f, HeldShotRules.powerForTaps(3), 0.0001f);
		assertEquals(1.0f, HeldShotRules.powerForTaps(HeldShotRules.MAX_TAPS), 0.0001f);
		assertEquals(1.0f, HeldShotRules.powerForTaps(HeldShotRules.MAX_TAPS * 3), 0.0001f);
	}

	@Test
	void firesAtMaxTapsOrWhenTheTapWindowCloses() {
		assertTrue(HeldShotRules.shouldFire(HeldShotRules.MAX_TAPS, 0));
		assertFalse(HeldShotRules.shouldFire(1, HeldShotRules.FIRE_DELAY_TICKS - 1));
		assertTrue(HeldShotRules.shouldFire(1, HeldShotRules.FIRE_DELAY_TICKS));
	}

	@Test
	void aLongPauseStartsAFreshMeter() {
		assertTrue(HeldShotRules.isNewMeter(0, 0));
		assertFalse(HeldShotRules.isNewMeter(2, HeldShotRules.METER_RESET_TICKS - 1));
		assertTrue(HeldShotRules.isNewMeter(2, HeldShotRules.METER_RESET_TICKS));
	}

	@Test
	void rapidRepeatsAreNotCountedAsTaps() {
		assertTrue(HeldShotRules.isRepeatWithinGap(HeldShotRules.MIN_TAP_GAP_TICKS - 1));
		assertFalse(HeldShotRules.isRepeatWithinGap(HeldShotRules.MIN_TAP_GAP_TICKS));
	}
}
