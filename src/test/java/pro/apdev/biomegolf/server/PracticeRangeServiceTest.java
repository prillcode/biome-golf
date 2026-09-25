package pro.apdev.biomegolf.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PracticeRangeServiceTest {

	@Test
	void targetNumbersAreBoundedToOneThroughMaxTargets() {
		assertFalse(PracticeRangeService.isValidTargetNumber(0));
		assertFalse(PracticeRangeService.isValidTargetNumber(-1));
		assertTrue(PracticeRangeService.isValidTargetNumber(1));
		assertTrue(PracticeRangeService.isValidTargetNumber(PracticeRangeService.MAX_TARGETS));
		assertFalse(PracticeRangeService.isValidTargetNumber(PracticeRangeService.MAX_TARGETS + 1));
	}
}
