package com.prillcode.minecraftgolf.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.Identifier;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.client.render.GolfBallEntityRenderer;
import com.prillcode.minecraftgolf.client.swing.SwingController;
import com.prillcode.minecraftgolf.client.swing.SwingHud;
import com.prillcode.minecraftgolf.entity.GolfBallEntities;

public class MinecraftGolfClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MinecraftGolf.LOGGER.info("{} client initialized", MinecraftGolf.MOD_NAME);

		// Minecraft 26.2 uses the render-state submit pipeline. Registration stays
		// client-only; Fabric's transitive access widener exposes this vanilla API.
		EntityRenderers.register(GolfBallEntities.GOLF_BALL, GolfBallEntityRenderer::new);

		SwingController swing = new SwingController();
		ClientTickEvents.END_CLIENT_TICK.register(swing::tick);
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (level.isClientSide() && player == Minecraft.getInstance().player) {
				swing.click(Minecraft.getInstance(), hand);
			}
			return net.minecraft.world.InteractionResult.PASS;
		});
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "swing_hud"),
				new SwingHud(swing)::render);
	}
}
