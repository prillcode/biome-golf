package com.prillcode.minecraftgolf;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prillcode.minecraftgolf.block.GolfBlocks;
import com.prillcode.minecraftgolf.command.GolfCourseCommands;
import com.prillcode.minecraftgolf.command.GolfDevCommands;
import com.prillcode.minecraftgolf.command.GolfHoleCommands;
import com.prillcode.minecraftgolf.entity.GolfBallEntities;
import com.prillcode.minecraftgolf.item.GolfItems;
import com.prillcode.minecraftgolf.net.HoleStateNetworking;
import com.prillcode.minecraftgolf.net.NextHoleNetworking;
import com.prillcode.minecraftgolf.net.RoundScorecardNetworking;
import com.prillcode.minecraftgolf.net.RoundLobbyNetworking;
import com.prillcode.minecraftgolf.net.GolfMenuNetworking;
import com.prillcode.minecraftgolf.net.ShotNetworking;
import com.prillcode.minecraftgolf.server.ActiveHoleService;
import com.prillcode.minecraftgolf.server.AuthoredCourseService;
import com.prillcode.minecraftgolf.server.BallCameraService;
import com.prillcode.minecraftgolf.server.HeldShotService;
import com.prillcode.minecraftgolf.server.PracticeRangeService;
import com.prillcode.minecraftgolf.server.VisitorService;
import com.prillcode.minecraftgolf.server.CourseBlockBreakGuard;

public class MinecraftGolf implements ModInitializer {
	public static final String MOD_ID = "minecraft_golf";
	public static final String MOD_NAME = "Minecraft Golf";
	private static final String WELCOME_TAG = MOD_ID + ":welcome_seen";

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

		// M10 (shelved): our blocks/items/entity live in vanilla registries. Fabric registry
		// sync otherwise disconnects any client without them ("This server requires Fabric
		// Loader and Fabric API installed on your client!"). Marking those registries
		// OPTIONAL lets unmodified Java and Bedrock/Geyser clients connect; they simply do
		// not resolve our custom content. Confirmed reachable in Fabric API 0.160.0+26.2
		// (fabric-registry-sync-v0 7.1.1: areAllRegistriesOptional -> no disconnect). This
		// and the other client-light fallbacks stay gated to non-mod clients; the Java
		// experience remains the supported target and no further Bedrock work is planned.
		markClientRegistryContentOptional();

		// Developer launch and test controls (M001-S03). Server-side only; the
		// command tree is op-gated and consumes only server-authoritative state.
		GolfDevCommands.register();

		// Courses are selected explicitly by an operator after server startup.
		GolfHoleCommands.register();

		// M8 S2: authored course store persistence (world JSON, fail-closed) and
		// operator authoring/selection commands. No course is selected at boot.
		AuthoredCourseService.register();
		PracticeRangeService.register();
		VisitorService.register();
		ActiveHoleService.register();
		GolfCourseCommands.register();

		// M3: typed shot-request networking (payload codec + server receiver).
		ShotNetworking.register();

		// M10.1: client-light held-use shot input (right-click hold near own ball).
		HeldShotService.register();

		// S03: clientbound hole-state snapshot networking; client receiver registered in MinecraftGolfClient.
		HoleStateNetworking.register();
		NextHoleNetworking.register();
		RoundScorecardNetworking.register();
		RoundLobbyNetworking.register();
		GolfMenuNetworking.register();

		// M7 S1: stop creative-mode destruction of the authored course (tee/cup vicinity
		// guard); operator/dev-exempt so /golf dev preparecourse recovery still works.
		CourseBlockBreakGuard.register();

		// S5: reconnect a suspended golfer and send the authoritative snapshot on join.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (!VisitorService.isVisitor(player)) {
				// Visitors get their own welcome from VisitorService; skip the generic lines.
				player.sendSystemMessage(Component.literal(player.entityTags().contains(WELCOME_TAG)
					? "Welcome back to BirdieBiome!"
					: "BirdieBiome welcomes you!"));
				player.sendSystemMessage(Component.literal("Use /golf help to get started."));
			}
			player.addTag(WELCOME_TAG);
			ActiveHoleService.instance().onPlayerConnected(player);
			VisitorService.instance().onPlayerJoined(player);
		});

		// S5: apply the Ready Golf suspension policy on disconnect.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			BallCameraService.instance().forget(handler.getPlayer().getUUID());
			HeldShotService.forget(handler.getPlayer().getUUID());
			VisitorService.instance().onPlayerDisconnected(handler.getPlayer());
			ActiveHoleService.instance().onPlayerDisconnected(handler.getPlayer(), server);
		});
	}

	/**
	 * M10: declares the vanilla registries this mod adds entries to as optional for
	 * client sync. This is what lets a client without the mod (vanilla Java, or Bedrock
	 * through Geyser) join at all; without it Fabric API's registry sync rejects them
	 * during configuration.
	 */
	private static void markClientRegistryContentOptional() {
		RegistryAttributeHolder.get(BuiltInRegistries.ENTITY_TYPE).addAttribute(RegistryAttribute.OPTIONAL);
		RegistryAttributeHolder.get(BuiltInRegistries.ITEM).addAttribute(RegistryAttribute.OPTIONAL);
		RegistryAttributeHolder.get(BuiltInRegistries.BLOCK).addAttribute(RegistryAttribute.OPTIONAL);
		LOGGER.info("Marked block/item/entity registries optional for client-light sync");
	}
}
