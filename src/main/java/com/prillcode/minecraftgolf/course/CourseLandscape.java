package com.prillcode.minecraftgolf.course;

import java.util.Objects;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;

/**
 * M8.10 S0: Minecraft-free whole-course landscape perimeter.
 *
 * <p>Operator-authored metadata, separate from the immutable
 * {@link CourseDefinition}, so it can be authored for both drafts and
 * finalized courses. The shape reuses the hole boundary box
 * ({@link HoleBoundary}, built from two captured corners by
 * {@link AuthoredHoleBounds#fromCorners}) with Y expanded to the world build
 * height; only X/Z come from the operator's corners.</p>
 *
 * <p>{@code locked} makes the perimeter {@link ProtectionVerdict#DENY_ALL} so
 * even operators cannot mutate the landscape; when unlocked it is
 * {@link ProtectionVerdict#DENY_NON_OP}, i.e. non-operator players are denied
 * but operators can still repair the course. Absent metadata means "no
 * perimeter, unlocked" and is fully backward compatible.</p>
 *
 * @param courseId  normalized id of the course this perimeter belongs to
 * @param dimension normalized dimension id the perimeter applies to
 * @param bounds    inclusive axis-aligned perimeter box (Y spans build height)
 * @param locked    whether the lock removes the operator exemption
 */
public record CourseLandscape(String courseId, String dimension, HoleBoundary bounds, boolean locked) {

	public CourseLandscape {
		courseId = AuthoredCourseStore.normalizeId(courseId);
		dimension = AuthoredCourseStore.normalizeDimension(dimension);
		Objects.requireNonNull(bounds, "bounds");
	}

	/** Returns whether the exact block coordinate lies inside the perimeter. */
	public boolean contains(int x, int y, int z) {
		return bounds.contains(new Vec3(x, y, z));
	}

	/** Returns whether the precise position lies inside the perimeter. */
	public boolean contains(Vec3 position) {
		return bounds.contains(Objects.requireNonNull(position, "position"));
	}

	/**
	 * The perimeter's protection contribution at a block position:
	 * {@link ProtectionVerdict#ALLOW} outside, {@link ProtectionVerdict#DENY_ALL}
	 * inside a locked perimeter, else {@link ProtectionVerdict#DENY_NON_OP}.
	 */
	public ProtectionVerdict verdictFor(int x, int y, int z) {
		if (!contains(x, y, z)) {
			return ProtectionVerdict.ALLOW;
		}
		return locked ? ProtectionVerdict.DENY_ALL : ProtectionVerdict.DENY_NON_OP;
	}

	/** Same as {@link #verdictFor(int, int, int)} for a precise position. */
	public ProtectionVerdict verdictFor(Vec3 position) {
		if (!contains(position)) {
			return ProtectionVerdict.ALLOW;
		}
		return locked ? ProtectionVerdict.DENY_ALL : ProtectionVerdict.DENY_NON_OP;
	}

	/** Returns a copy with the same perimeter and the given lock state. */
	public CourseLandscape withLocked(boolean newLocked) {
		return new CourseLandscape(courseId, dimension, bounds, newLocked);
	}
}
