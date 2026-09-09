package com.prillcode.minecraftgolf.dev;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Dedicated-server-safe adapter from the pure generation plan to a loaded level. */
public final class MinecraftDevelopmentCourseWorld implements DevelopmentCourseGenerator.WorldAccess {

	private final ServerLevel level;

	public MinecraftDevelopmentCourseWorld(ServerLevel level, DevelopmentCoursePlan plan) {
		this.level = level;
		loadChunks(plan);
	}

	@Override
	public boolean canReplace(BlockPoint point, LayoutBlock desired, Set<LayoutBlock> generatedPalette) {
		BlockState current = level.getBlockState(pos(point));
		if (current.isAir() || current.is(Blocks.WATER) || current.is(Blocks.SEAGRASS)
				|| current.is(Blocks.TALL_SEAGRASS) || current.is(Blocks.KELP)
				|| current.is(Blocks.KELP_PLANT)) {
			return true;
		}
		return generatedPalette.stream().map(MinecraftDevelopmentCourseWorld::block)
			.anyMatch(current::is);
	}

	@Override
	public boolean matches(BlockPoint point, LayoutBlock desired) {
		return level.getBlockState(pos(point)).is(block(desired));
	}

	@Override
	public void set(BlockPoint point, LayoutBlock desired) {
		level.setBlock(pos(point), block(desired).defaultBlockState(), 3);
	}

	private void loadChunks(DevelopmentCoursePlan plan) {
		for (LayoutOperation operation : plan.operations()) {
			BlockVolume bounds = operation.volume();
			for (int chunkX = bounds.min().x() >> 4; chunkX <= bounds.max().x() >> 4; chunkX++) {
				for (int chunkZ = bounds.min().z() >> 4; chunkZ <= bounds.max().z() >> 4; chunkZ++) {
					level.getChunk(chunkX, chunkZ);
				}
			}
		}
	}

	private static BlockPos pos(BlockPoint point) {
		return new BlockPos(point.x(), point.y(), point.z());
	}

	private static Block block(LayoutBlock block) {
		return switch (block) {
			case AIR -> Blocks.AIR;
			case GRASS_BLOCK -> Blocks.GRASS_BLOCK;
		};
	}
}
