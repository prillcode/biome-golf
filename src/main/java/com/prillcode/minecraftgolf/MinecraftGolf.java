package com.prillcode.minecraftgolf;

import net.fabricmc.api.ModInitializer;
import net.minecraft.world.entity.EntityType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prillcode.minecraftgolf.command.GolfDevCommands;
import com.prillcode.minecraftgolf.entity.GolfBallEntities;
import com.prillcode.minecraftgolf.item.GolfItems;
import com.prillcode.minecraftgolf.net.ShotNetworking;

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

		// Developer launch and test controls (M001-S03). Server-side only; the
		// command tree is op-gated and consumes only server-authoritative state.
		GolfDevCommands.register();

		// M3: typed shot-request networking (payload codec + server receiver).
		ShotNetworking.register();
	}
}
