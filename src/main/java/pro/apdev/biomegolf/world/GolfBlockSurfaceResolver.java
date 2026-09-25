package pro.apdev.biomegolf.world;

import java.util.Objects;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;

import pro.apdev.biomegolf.surface.SurfaceDefinition;

/**
 * Maps a Minecraft {@link BlockState} to the logical golf {@link SurfaceDefinition}
 * the physics engine should use (ARCHITECTURE.md §11).
 *
 * <p>Resolution is tag-driven where vanilla tags exist ({@code #minecraft:sand},
 * {@code #minecraft:ice}). Slime and honey blocks have no dedicated vanilla
 * block tags, so they are matched by block directly. Everything else — grass,
 * dirt, stone, planks, and any future course block — behaves as the NORMAL
 * fairway baseline until a course definition or new tag overrides it.</p>
 */
public final class GolfBlockSurfaceResolver {

	public GolfBlockSurfaceResolver() {
	}

	/**
	 * Resolves the golf surface for a block state.
	 *
	 * @param state block state at the ball's contact/support position
	 * @return the matching logical surface, never null
	 */
	public SurfaceDefinition resolve(BlockState state) {
		Objects.requireNonNull(state, "state");
		if (state.is(BlockTags.SAND)) {
			return SurfaceDefinition.SAND;
		}
		if (state.is(BlockTags.ICE)) {
			return SurfaceDefinition.ICE;
		}
		if (state.is(Blocks.SLIME_BLOCK)) {
			return SurfaceDefinition.SLIME;
		}
		if (state.is(Blocks.HONEY_BLOCK)) {
			return SurfaceDefinition.HONEY;
		}
		return SurfaceDefinition.NORMAL;
	}
}
