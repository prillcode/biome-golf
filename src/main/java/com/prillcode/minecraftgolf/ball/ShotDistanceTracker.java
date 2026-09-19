package com.prillcode.minecraftgolf.ball;

import com.prillcode.minecraftgolf.golf.Vec3;

/** Accumulates horizontal distance traveled by one authoritative shot, in blocks. */
public final class ShotDistanceTracker {
	private double blocks;

	public void reset() {
		blocks = 0.0;
	}

	public void setBlocks(double blocks) {
		if (!Double.isFinite(blocks) || blocks < 0.0) {
			throw new IllegalArgumentException("blocks must be finite and non-negative: " + blocks);
		}
		this.blocks = blocks;
	}

	public void advance(Vec3 from, Vec3 to) {
		blocks += to.subtract(from).horizontalLength();
	}

	public double blocks() {
		return blocks;
	}

	public int roundedBlocks() {
		return (int) Math.round(blocks);
	}
}
