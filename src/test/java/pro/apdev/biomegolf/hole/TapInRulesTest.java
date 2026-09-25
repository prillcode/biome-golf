package pro.apdev.biomegolf.hole;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import pro.apdev.biomegolf.golf.Vec3;

class TapInRulesTest {
	@Test
	void usesHorizontalTwoBlockBoundaryAndIgnoresHeight() {
		Vec3 cup = Vec3.of(10, 64, 10);
		// Well inside the two-block radius.
		assertTrue(TapInRules.withinTapInRadius(Vec3.of(10.5, 90, 10.8), cup));
		// 1.13 blocks out: outside the old one-block rule, inside the current two-block rule.
		assertTrue(TapInRules.withinTapInRadius(Vec3.of(10.8, 64, 10.8), cup));
		// Exactly two blocks out is inclusive.
		assertTrue(TapInRules.withinTapInRadius(Vec3.of(12.0, 64, 10.0), cup));
		// Just beyond two blocks is not eligible.
		assertFalse(TapInRules.withinTapInRadius(Vec3.of(12.01, 64, 10.0), cup));
		// Horizontal only: flight height is ignored.
		assertTrue(TapInRules.withinTapInRadius(Vec3.of(10, 500, 10), cup));
	}
}
