package pro.apdev.biomegolf.dev;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pro.apdev.biomegolf.course.GeneratedLayoutIdentity;

/** Immutable, deterministic plan for explicitly authored development-world mutations. */
public record DevelopmentCoursePlan(
	GeneratedLayoutIdentity identity,
	String dimension,
	BlockVolume campusEnvelope,
	List<AuthoredRegion> authoredRegions,
	List<LayoutOperation> operations
) {
	public DevelopmentCoursePlan {
		Objects.requireNonNull(identity, "identity");
		if (dimension == null || dimension.isBlank()) {
			throw new IllegalArgumentException("dimension must not be blank");
		}
		Objects.requireNonNull(campusEnvelope, "campusEnvelope");
		authoredRegions = List.copyOf(Objects.requireNonNull(authoredRegions, "authoredRegions"));
		operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
		if (authoredRegions.isEmpty()) {
			throw new IllegalArgumentException("at least one authored region is required");
		}
		for (AuthoredRegion region : authoredRegions) {
			if (!campusEnvelope.contains(region.bounds())) {
				throw new IllegalArgumentException("authored region " + region.id() + " leaves campus envelope");
			}
		}
		for (LayoutOperation operation : operations) {
			boolean declared = authoredRegions.stream()
				.anyMatch(region -> region.bounds().contains(operation.volume()));
			if (!declared) {
				throw new IllegalArgumentException("layout operation leaves every authored region");
			}
		}
	}

	/** Final desired blocks in stable operation/coordinate order. */
	public Map<BlockPoint, LayoutBlock> desiredBlocks() {
		Map<BlockPoint, LayoutBlock> desired = new LinkedHashMap<>();
		for (LayoutOperation operation : operations) {
			if (operation.replacementRule() == ReplacementRule.ALWAYS) {
				operation.volume().points().forEach(point -> desired.put(point, operation.block()));
			}
		}
		return Collections.unmodifiableMap(desired);
	}

	public boolean isInsideAuthoredRegion(BlockPoint point) {
		return authoredRegions.stream().anyMatch(region -> region.bounds().contains(point));
	}
}
