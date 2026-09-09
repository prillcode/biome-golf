package com.prillcode.minecraftgolf.dev;

/** Declarative fill operation; later applicable operations override earlier overlaps. */
public record LayoutOperation(BlockVolume volume, LayoutBlock block, ReplacementRule replacementRule) {

	public LayoutOperation(BlockVolume volume, LayoutBlock block) {
		this(volume, block, ReplacementRule.ALWAYS);
	}

	public LayoutOperation {
		if (volume == null || block == null || replacementRule == null) {
			throw new NullPointerException("layout operation values must not be null");
		}
		if (replacementRule == ReplacementRule.VEGETATION_ONLY && block != LayoutBlock.AIR) {
			throw new IllegalArgumentException("vegetation-only operations must clear to air");
		}
	}
}
