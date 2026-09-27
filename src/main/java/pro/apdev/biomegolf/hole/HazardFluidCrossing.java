package pro.apdev.biomegolf.hole;

import java.util.Optional;

import pro.apdev.biomegolf.golf.Vec3;

/**
 * Pure geometric search for the first hazard-fluid contact along one tick's
 * movement segment.
 *
 * <p>The block query needs Minecraft, so this class takes a {@link FluidTest}
 * delegate and stays free of Minecraft types (testable on the plain JVM). A
 * contact is recorded when either the ball center or its lowest point (center
 * minus the ball radius) is inside hazard fluid, matching the pre-existing
 * server-side hazard check.</p>
 */
public final class HazardFluidCrossing {

	private static final double SAMPLE_SPACING = 0.2;
	private static final int REFINEMENT_STEPS = 8;

	private HazardFluidCrossing() {
	}

	@FunctionalInterface
	public interface FluidTest {
		/** Whether hazard fluid occupies the block containing the given world point. */
		boolean isFluid(double x, double y, double z);
	}

	/**
	 * Returns the first point on the {@code from}–{@code to} segment where the
	 * ball touches hazard fluid, refined to the fluid margin by bisection, or
	 * empty when the whole segment is clear.
	 */
	public static Optional<Vec3> firstContact(Vec3 from, Vec3 to, double ballRadius,
			FluidTest fluidTest) {
		Vec3 delta = to.subtract(from);
		double distance = delta.length();
		int steps = Math.max(1, (int) Math.ceil(distance / SAMPLE_SPACING));
		Vec3 previous = from;
		for (int i = 0; i <= steps; i++) {
			Vec3 sample = from.add(delta.scale((double) i / steps));
			if (touchesFluid(sample, ballRadius, fluidTest)) {
				return Optional.of(refine(previous, sample, ballRadius, fluidTest));
			}
			previous = sample;
		}
		return Optional.empty();
	}

	private static Vec3 refine(Vec3 dry, Vec3 wet, double ballRadius, FluidTest fluidTest) {
		Vec3 low = dry;
		Vec3 high = wet;
		for (int i = 0; i < REFINEMENT_STEPS; i++) {
			Vec3 mid = low.add(high).scale(0.5);
			if (touchesFluid(mid, ballRadius, fluidTest)) {
				high = mid;
			} else {
				low = mid;
			}
		}
		return high;
	}

	private static boolean touchesFluid(Vec3 point, double ballRadius, FluidTest fluidTest) {
		return fluidTest.isFluid(point.x(), point.y(), point.z())
			|| fluidTest.isFluid(point.x(), point.y() - ballRadius, point.z());
	}
}
