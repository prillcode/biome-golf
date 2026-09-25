package pro.apdev.biomegolf.dev;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Inclusive axis-aligned block volume. */
public record BlockVolume(BlockPoint min, BlockPoint max) {

	public BlockVolume {
		if (min == null || max == null) {
			throw new NullPointerException("volume bounds must not be null");
		}
		if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
			throw new IllegalArgumentException("volume min must not exceed max on any axis");
		}
	}

	public boolean contains(BlockPoint point) {
		return point.x() >= min.x() && point.x() <= max.x()
			&& point.y() >= min.y() && point.y() <= max.y()
			&& point.z() >= min.z() && point.z() <= max.z();
	}

	public boolean contains(BlockVolume other) {
		return contains(other.min()) && contains(other.max());
	}

	public Stream<BlockPoint> points() {
		return IntStream.rangeClosed(min.x(), max.x()).boxed().flatMap(x ->
			IntStream.rangeClosed(min.y(), max.y()).boxed().flatMap(y ->
				IntStream.rangeClosed(min.z(), max.z()).mapToObj(z -> new BlockPoint(x, y, z))));
	}
}
