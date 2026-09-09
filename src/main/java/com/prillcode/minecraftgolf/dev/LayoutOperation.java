package com.prillcode.minecraftgolf.dev;

/** Declarative fill operation; later operations override earlier overlaps. */
public record LayoutOperation(BlockVolume volume, LayoutBlock block) {

	public LayoutOperation {
		if (volume == null || block == null) {
			throw new NullPointerException("layout operation values must not be null");
		}
	}
}
