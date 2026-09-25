package pro.apdev.biomegolf.server;

import java.util.Optional;

/** Pure candidate search for a safe player position near an authoritative stopped ball. */
public final class TravelDestinationSearch {

	private static final int[][] OFFSETS = {
		{2, 0}, {-2, 0}, {0, 2}, {0, -2},
		{2, 2}, {2, -2}, {-2, 2}, {-2, -2},
		{3, 0}, {-3, 0}, {0, 3}, {0, -3}
	};

	private TravelDestinationSearch() {
	}

	public static Optional<Destination> find(int ballX, int ballY, int ballZ, SafetyCheck safety) {
		for (int[] offset : OFFSETS) {
			for (int y = ballY + 3; y >= ballY - 4; y--) {
				Destination candidate = new Destination(ballX + offset[0], y, ballZ + offset[1]);
				if (safety.isSafe(candidate)) {
					return Optional.of(candidate);
				}
			}
		}
		return Optional.empty();
	}

	@FunctionalInterface
	public interface SafetyCheck {
		boolean isSafe(Destination destination);
	}

	public record Destination(int x, int y, int z) {
	}
}
