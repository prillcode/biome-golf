package com.prillcode.minecraftgolf.hole;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.golf.Vec3;

class TapInRulesTest {
	@Test
	void usesHorizontalOneBlockBoundary() {
		Vec3 cup = Vec3.of(10, 64, 10);
		assertTrue(TapInRules.withinOneBlock(Vec3.of(10.5, 90, 10.8), cup));
		assertFalse(TapInRules.withinOneBlock(Vec3.of(10.8, 64, 10.8), cup));
		assertTrue(TapInRules.withinOneBlock(Vec3.of(10, 500, 10), cup));
	}
}
