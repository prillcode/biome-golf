package pro.apdev.biomegolf.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Low collision ring with an open center; the flag is visual only. */
public final class GolfCupBlock extends Block {

	private static final VoxelShape RING = Shapes.or(
		Block.box(0.0, 0.0, 0.0, 3.0, 2.0, 16.0),
		Block.box(13.0, 0.0, 0.0, 16.0, 2.0, 16.0),
		Block.box(3.0, 0.0, 0.0, 13.0, 2.0, 3.0),
		Block.box(3.0, 0.0, 13.0, 13.0, 2.0, 16.0)
	);

	public GolfCupBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return RING;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return RING;
	}
}
