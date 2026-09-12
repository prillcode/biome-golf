package com.prillcode.minecraftgolf.dev;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import com.prillcode.minecraftgolf.block.GolfBlocks;
import com.prillcode.minecraftgolf.entity.GolfBallEntity;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

/** Explicit, bounded development-world preparation for a configured flat practice hole. */
public final class DevelopmentHoleBuilder {

	private static final int END_MARGIN_X = 2;
	private static final int SIDE_MARGIN_Z = 3;
	private static final int CLEAR_HEIGHT = 18;

	private DevelopmentHoleBuilder() {
	}

	public static Layout layout(HoleDefinition hole) {
		int teeFloorY = floorBelowBall(hole.tee().y());
		int cupFloorY = floorBelowBall(hole.cup().y());
		if (teeFloorY != cupFloorY) {
			throw new IllegalArgumentException(
				"development-hole preparation requires tee and cup on the same floor level");
		}

		int teeX = floor(hole.tee().x());
		int cupX = floor(hole.cup().x());
		int teeZ = floor(hole.tee().z());
		int cupZ = floor(hole.cup().z());
		return new Layout(
			Math.min(teeX, cupX) - END_MARGIN_X,
			Math.max(teeX, cupX) + END_MARGIN_X,
			teeFloorY,
			Math.min(teeZ, cupZ) - SIDE_MARGIN_Z,
			Math.max(teeZ, cupZ) + SIDE_MARGIN_Z,
			teeFloorY + 1,
			teeFloorY + CLEAR_HEIGHT,
			cupX,
			floor(hole.cup().y() - GolfBallEntity.BALL_RADIUS),
			cupZ);
	}

	public static Layout prepare(ServerLevel level, HoleDefinition hole) {
		Layout layout = layout(hole);
		loadChunks(level, layout);

		for (BlockPos pos : BlockPos.betweenClosed(
			layout.minX(), layout.clearMinY(), layout.minZ(),
			layout.maxX(), layout.clearMaxY(), layout.maxZ())) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}
		for (BlockPos pos : BlockPos.betweenClosed(
			layout.minX(), layout.floorY(), layout.minZ(),
			layout.maxX(), layout.floorY(), layout.maxZ())) {
			level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		}
		level.setBlock(
			new BlockPos(layout.cupX(), layout.cupY(), layout.cupZ()),
			GolfBlocks.GOLF_CUP.defaultBlockState(),
			3);
		level.setBlock(
			new BlockPos(layout.cupX(), layout.cupY() + 1, layout.cupZ()),
			GolfBlocks.GOLF_FLAG.defaultBlockState(),
			3);
		level.setBlock(
			new BlockPos(layout.cupX(), layout.cupY() + 2, layout.cupZ()),
			GolfBlocks.GOLF_FLAG_TOP.defaultBlockState(),
			3);
		return layout;
	}

	private static void loadChunks(ServerLevel level, Layout layout) {
		for (int chunkX = layout.minX() >> 4; chunkX <= layout.maxX() >> 4; chunkX++) {
			for (int chunkZ = layout.minZ() >> 4; chunkZ <= layout.maxZ() >> 4; chunkZ++) {
				level.getChunk(chunkX, chunkZ);
			}
		}
	}

	private static int floorBelowBall(double y) {
		return floor(y - GolfBallEntity.BALL_RADIUS - 0.01);
	}

	private static int floor(double value) {
		return (int) Math.floor(value);
	}

	public record Layout(
		int minX,
		int maxX,
		int floorY,
		int minZ,
		int maxZ,
		int clearMinY,
		int clearMaxY,
		int cupX,
		int cupY,
		int cupZ
	) {
	}
}
