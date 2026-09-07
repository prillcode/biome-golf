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

		// Minecraft 26.2 uses the render-state submit pipeline. Registration stays
		// client-only; Fabric's transitive access widener exposes this vanilla API.
		EntityRenderers.register(GolfBallEntities.GOLF_BALL, GolfBallEntityRenderer::new);
	}
}
