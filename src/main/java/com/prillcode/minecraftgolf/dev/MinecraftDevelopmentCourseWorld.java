package com.prillcode.minecraftgolf.dev;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.prillcode.minecraftgolf.block.GolfBlocks;

/** Dedicated-server-safe adapter from the pure generation plan to a loaded level. */
public final class MinecraftDevelopmentCourseWorld implements DevelopmentCourseGenerator.WorldAccess {

	private final ServerLevel level;

	public MinecraftDevelopmentCourseWorld(ServerLevel level, DevelopmentCoursePlan plan) {
		this.level = level;
		loadChunks(plan);
	}

	@Override
	public boolean matchesReplacementRule(BlockPoint point, ReplacementRule replacementRule) {
		if (replacementRule == ReplacementRule.ALWAYS) {
			return true;
		}
		BlockState state = level.getBlockState(pos(point));
		return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
	}

	@Override
	public boolean canReplace(BlockPoint point, LayoutBlock desired, Set<LayoutBlock> generatedPalette) {
		// The command layer has already required the exact disposable development
		// seed. Preserve stateful/operator-authored blocks as a second safety gate.
		return level.getBlockEntity(pos(point)) == null;
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
			case DIRT -> Blocks.DIRT;
			case SAND -> Blocks.SAND;
			case WATER -> Blocks.WATER;
			case ICE -> Blocks.ICE;
			case SLIME_BLOCK -> Blocks.SLIME_BLOCK;
			case GOLD_BLOCK -> Blocks.GOLD_BLOCK;
			case BLUE_CONCRETE -> Blocks.CONCRETE.blue();
			case YELLOW_CONCRETE -> Blocks.CONCRETE.yellow();
			case BLACK_CONCRETE -> Blocks.CONCRETE.black();
			case GREEN_WOOL -> Blocks.WOOL.green();
			case TARGET -> Blocks.TARGET;
			case GOLF_CUP -> GolfBlocks.GOLF_CUP;
			case GOLF_FLAG -> GolfBlocks.GOLF_FLAG;
			case GOLF_FLAG_TOP -> GolfBlocks.GOLF_FLAG_TOP;
		};
	}
}
