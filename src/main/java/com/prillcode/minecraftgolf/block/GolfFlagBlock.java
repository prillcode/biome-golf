package com.prillcode.minecraftgolf.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Visual-only upper section of a golf cup's flag; it has no collision. */
public final class GolfFlagBlock extends Block {

	private static final VoxelShape SELECTION = Block.box(7.5, 0.0, 7.5, 8.5, 16.0, 8.5);

	public GolfFlagBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		// Keep the visual-only flag targetable so stale authored markers can be removed.
		return SELECTION;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return Shapes.empty();
	}
}
