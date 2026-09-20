package com.prillcode.minecraftgolf.course;

/**
 * M8.10 S0: Minecraft-free result of querying a world position against every
 * protected region of a course.
 *
 * <p>Ordered from least to most restrictive so several region hits can be
 * combined with most-restrictive-wins (see {@link #mostRestrictive}).</p>
 *
 * <ul>
 *   <li>{@link #ALLOW} - the position is outside every protected region.</li>
 *   <li>{@link #DENY_NON_OP} - the position is protected, but operators remain
 *       exempt so course repair stays possible (tee/cup vicinity, or an
 *       unlocked landscape perimeter).</li>
 *   <li>{@link #DENY_ALL} - the position is protected and locked: no player may
 *       mutate it, regardless of permission (locked landscape perimeter).</li>
 * </ul>
 */
public enum ProtectionVerdict {
	ALLOW,
	DENY_NON_OP,
	DENY_ALL;

	/** Returns the stricter of this verdict and {@code other}. */
	public ProtectionVerdict mostRestrictive(ProtectionVerdict other) {
		if (other == null) {
			return this;
		}
		return other.ordinal() > ordinal() ? other : this;
	}

	/**
	 * Whether this verdict forbids a block mutation for a player with the given
	 * operator (gamemaster) permission. {@link #DENY_NON_OP} exempts operators;
	 * {@link #DENY_ALL} does not.
	 */
	public boolean denies(boolean hasOperatorPermission) {
		return switch (this) {
			case ALLOW -> false;
			case DENY_NON_OP -> !hasOperatorPermission;
			case DENY_ALL -> true;
		};
	}
}
