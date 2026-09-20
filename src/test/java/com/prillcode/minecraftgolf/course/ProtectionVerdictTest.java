package com.prillcode.minecraftgolf.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProtectionVerdictTest {

	@Test
	void mostRestrictiveWins() {
		assertEquals(ProtectionVerdict.ALLOW,
			ProtectionVerdict.ALLOW.mostRestrictive(ProtectionVerdict.ALLOW));
		assertEquals(ProtectionVerdict.DENY_NON_OP,
			ProtectionVerdict.ALLOW.mostRestrictive(ProtectionVerdict.DENY_NON_OP));
		assertEquals(ProtectionVerdict.DENY_NON_OP,
			ProtectionVerdict.DENY_NON_OP.mostRestrictive(ProtectionVerdict.ALLOW));
		assertEquals(ProtectionVerdict.DENY_ALL,
			ProtectionVerdict.DENY_NON_OP.mostRestrictive(ProtectionVerdict.DENY_ALL));
		assertEquals(ProtectionVerdict.DENY_ALL,
			ProtectionVerdict.DENY_ALL.mostRestrictive(ProtectionVerdict.DENY_NON_OP));
		assertEquals(ProtectionVerdict.DENY_ALL,
			ProtectionVerdict.DENY_ALL.mostRestrictive(ProtectionVerdict.ALLOW));
	}

	@Test
	void deniesOperatorsOnlyWhenLocked() {
		assertFalse(ProtectionVerdict.ALLOW.denies(false));
		assertFalse(ProtectionVerdict.ALLOW.denies(true));
		assertTrue(ProtectionVerdict.DENY_NON_OP.denies(false));
		assertFalse(ProtectionVerdict.DENY_NON_OP.denies(true));
		assertTrue(ProtectionVerdict.DENY_ALL.denies(false));
		assertTrue(ProtectionVerdict.DENY_ALL.denies(true));
	}
}
