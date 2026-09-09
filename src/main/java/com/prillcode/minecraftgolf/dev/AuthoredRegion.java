package com.prillcode.minecraftgolf.dev;

/** Named subregion that an authored layout is allowed to mutate. */
public record AuthoredRegion(String id, BlockVolume bounds) {

	public AuthoredRegion {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("authored region id must not be blank");
		}
		if (bounds == null) {
			throw new NullPointerException("authored region bounds must not be null");
		}
	}
}
