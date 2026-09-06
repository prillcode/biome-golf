package com.prillcode.minecraftgolf.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.client.render.GolfBallEntityRenderer;
import com.prillcode.minecraftgolf.entity.GolfBallEntities;

public class MinecraftGolfClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MinecraftGolf.LOGGER.info("{} client initialized", MinecraftGolf.MOD_NAME);

		// Placeholder renderer (M001-S03 interim, replaced by the real ball model
		// in M001-S04). Required because Minecraft 26.2 crashes when a tracked
		// entity type has no registered client renderer. Client-only code; the
		// register method is made public via Fabric Transitive Access Wideners.
		EntityRenderers.register(GolfBallEntities.GOLF_BALL, GolfBallEntityRenderer::new);
	}
}
