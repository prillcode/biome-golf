package com.prillcode.minecraftgolf;

import java.io.IOException;
import java.nio.file.Path;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntityType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prillcode.minecraftgolf.block.GolfBlocks;
import com.prillcode.minecraftgolf.command.GolfDevCommands;
import com.prillcode.minecraftgolf.command.GolfHoleCommands;
import com.prillcode.minecraftgolf.config.HoleConfigLoader;
import com.prillcode.minecraftgolf.entity.GolfBallEntities;
import com.prillcode.minecraftgolf.item.GolfItems;
import com.prillcode.minecraftgolf.net.HoleStateNetworking;
import com.prillcode.minecraftgolf.net.ShotNetworking;
import com.prillcode.minecraftgolf.server.ActiveHoleService;

public class MinecraftGolf implements ModInitializer {
	public static final String MOD_ID = "minecraft_golf";
	public static final String MOD_NAME = "Minecraft Golf";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		if (!com.prillcode.minecraftgolf.domain.ModId.isValidModId(MOD_ID)) {
			throw new IllegalStateException("Configured mod id '" + MOD_ID + "' is not a valid Fabric mod id");
		}

		// Force registry population and log evidence of the entity registration
		// (M001-S02 after-this: runServer boots; entity registered; summonable).
		EntityType<com.prillcode.minecraftgolf.entity.GolfBallEntity> type = GolfBallEntities.GOLF_BALL;
		LOGGER.info("{} initialized", MOD_NAME);
		LOGGER.info("Registered entity {} as {}",
				EntityType.getKey(type),
				type.toShortString());

		// M2 club items: seven Minecraft items backed by ClubDefinition data.
		GolfItems.registerAll();
		LOGGER.info("Registered {} golf club items", GolfItems.CLUB_ITEMS.size());

		// M4 cup/flag block; authoritative detection remains server-side.
		GolfBlocks.registerAll();

		// Developer launch and test controls (M001-S03). Server-side only; the
		// command tree is op-gated and consumes only server-authoritative state.
		GolfDevCommands.register();

		// M4: one human-readable configured hole and its player lifecycle.
		Path holeConfig = FabricLoader.getInstance().getConfigDir()
				.resolve("minecraft_golf").resolve("hole.json");
		try {
			ActiveHoleService.instance().initialize(HoleConfigLoader.loadOrCreate(holeConfig));
		} catch (IOException | IllegalArgumentException exception) {
			throw new IllegalStateException("Could not load Minecraft Golf hole config at " + holeConfig, exception);
		}
		GolfHoleCommands.register();

		// M3: typed shot-request networking (payload codec + server receiver).
		ShotNetworking.register();

		// S03: clientbound hole-state snapshot networking; client receiver registered in MinecraftGolfClient.
		HoleStateNetworking.register();

		// S03: send the current hole-state snapshot to each player on join/reconnect.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			ActiveHoleService.instance().sendCurrentSnapshot(handler.getPlayer()));
	}
}
