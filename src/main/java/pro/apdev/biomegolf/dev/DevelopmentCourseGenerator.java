package pro.apdev.biomegolf.dev;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Two-phase preflight/apply engine that cannot write beyond a validated plan. */
public final class DevelopmentCourseGenerator {

	private DevelopmentCourseGenerator() {
	}

	public static GenerationResult prepare(DevelopmentCoursePlan plan, WorldAccess world) {
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(world, "world");
		Map<BlockPoint, LayoutBlock> desired = new LinkedHashMap<>();
		for (LayoutOperation operation : plan.operations()) {
			operation.volume().points().forEach(point -> {
				if (operation.replacementRule() == ReplacementRule.ALWAYS
						|| world.matchesReplacementRule(point, operation.replacementRule())) {
					desired.put(point, operation.block());
				}
			});
		}
		Set<LayoutBlock> palette = plan.operations().stream()
			.map(LayoutOperation::block)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());

		for (Map.Entry<BlockPoint, LayoutBlock> entry : desired.entrySet()) {
			if (!world.canReplace(entry.getKey(), entry.getValue(), palette)) {
				throw new UnsafeTerrainException(entry.getKey(),
					"unsafe stateful block in authored footprint");
			}
		}

		int writes = 0;
		for (Map.Entry<BlockPoint, LayoutBlock> entry : desired.entrySet()) {
			if (!world.matches(entry.getKey(), entry.getValue())) {
				world.set(entry.getKey(), entry.getValue());
				writes++;
			}
		}
		return new GenerationResult(desired.size(), writes);
	}

	public interface WorldAccess {
		boolean matchesReplacementRule(BlockPoint point, ReplacementRule replacementRule);

		boolean canReplace(BlockPoint point, LayoutBlock desired, Set<LayoutBlock> generatedPalette);

		boolean matches(BlockPoint point, LayoutBlock desired);

		void set(BlockPoint point, LayoutBlock desired);
	}

	public record GenerationResult(int plannedBlocks, int changedBlocks) {
	}

	public static final class UnsafeTerrainException extends IllegalStateException {
		private final BlockPoint point;

		public UnsafeTerrainException(BlockPoint point, String message) {
			super(message + " at " + point.x() + " " + point.y() + " " + point.z());
			this.point = point;
		}

		public BlockPoint point() {
			return point;
		}
	}
}
