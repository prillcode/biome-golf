package com.prillcode.minecraftgolf.surface;

import java.util.Objects;

/**
 * Tunable coefficients for one logical golf surface.
 *
 * <p>Minecraft blocks are mapped to these surfaces at the integration
 * boundary; the pure physics engine only sees these coefficients.</p>
 *
 * @param rollingFriction  horizontal velocity multiplier per tick while rolling (0-1)
 * @param bounceMultiplier energy multiplier applied to the reflected normal component
 */
public record SurfaceDefinition(String id, double rollingFriction, double bounceMultiplier) {

	public SurfaceDefinition {
		Objects.requireNonNull(id, "id");
		if (rollingFriction < 0.0 || rollingFriction > 1.0) {
			throw new IllegalArgumentException("rollingFriction must be within [0,1]: " + rollingFriction);
		}
		if (bounceMultiplier < 0.0) {
			throw new IllegalArgumentException("bounceMultiplier must be >= 0: " + bounceMultiplier);
		}
	}

	public static final SurfaceDefinition NORMAL = new SurfaceDefinition("normal", 0.95, 1.0);
	public static final SurfaceDefinition SAND = new SurfaceDefinition("sand", 0.60, 0.30);
	public static final SurfaceDefinition ICE = new SurfaceDefinition("ice", 0.995, 0.95);
	public static final SurfaceDefinition SLIME = new SurfaceDefinition("slime", 0.90, 1.60);
	public static final SurfaceDefinition HONEY = new SurfaceDefinition("honey", 0.55, 0.05);
	public static final SurfaceDefinition GENERIC = new SurfaceDefinition("generic", 0.94, 0.90);
}
