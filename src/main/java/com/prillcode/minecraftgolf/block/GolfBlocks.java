package com.prillcode.minecraftgolf.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import com.prillcode.minecraftgolf.MinecraftGolf;

/** Common/server-safe registration for M4 golf blocks. */
public final class GolfBlocks {

	public static final ResourceKey<Block> GOLF_CUP_KEY = ResourceKey.create(
		Registries.BLOCK,
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "golf_cup")
	);

	public static final GolfCupBlock GOLF_CUP = Registry.register(
		BuiltInRegistries.BLOCK,
		GOLF_CUP_KEY,
		new GolfCupBlock(BlockBehaviour.Properties.of()
			.setId(GOLF_CUP_KEY)
			.mapColor(MapColor.COLOR_GREEN)
			.strength(0.5F)
			.sound(SoundType.WOOL)
			.noOcclusion())
	);

	private GolfBlocks() {
	}

	public static void registerAll() {
		MinecraftGolf.LOGGER.info("Registered golf cup block as {}", GOLF_CUP_KEY.identifier());
	}
}
