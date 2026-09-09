package com.prillcode.minecraftgolf.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.Identifier;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.client.camera.PostShotCamera;
import com.prillcode.minecraftgolf.client.hole.HoleHud;
import com.prillcode.minecraftgolf.client.hole.HoleHudState;
import com.prillcode.minecraftgolf.client.input.PracticeKeybindings;
import com.prillcode.minecraftgolf.client.render.GolfBallEntityRenderer;
import com.prillcode.minecraftgolf.client.swing.SwingController;
import com.prillcode.minecraftgolf.client.swing.SwingHud;
import com.prillcode.minecraftgolf.entity.GolfBallEntities;
import com.prillcode.minecraftgolf.net.HoleStatePayload;

public class MinecraftGolfClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MinecraftGolf.LOGGER.info("{} client initialized", MinecraftGolf.MOD_NAME);

		// Minecraft 26.2 uses the render-state submit pipeline. Registration stays
		// client-only; Fabric's transitive access widener exposes this vanilla API.
		EntityRenderers.register(GolfBallEntities.GOLF_BALL, GolfBallEntityRenderer::new);

		PostShotCamera postShotCamera = new PostShotCamera();
		SwingController swing = new SwingController(postShotCamera);
		PracticeKeybindings practiceKeys = new PracticeKeybindings();
		ClientTickEvents.END_CLIENT_TICK.register(swing::tick);
		ClientTickEvents.END_CLIENT_TICK.register(practiceKeys::tick);
		ClientTickEvents.END_CLIENT_TICK.register(postShotCamera::tick);
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (level.isClientSide() && player == Minecraft.getInstance().player) {
				swing.click(Minecraft.getInstance(), hand);
			}
			return net.minecraft.world.InteractionResult.PASS;
		});
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "swing_hud"),
				new SwingHud(swing)::render);

		// S03: receive authoritative hole-state snapshots from the server.
		ClientPlayNetworking.registerGlobalReceiver(
				HoleStatePayload.TYPE,
				(payload, context) -> HoleHudState.update(payload));

		// S03: clear stale display state on disconnect.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HoleHudState.clear());

		// S03: hole HUD panel — rendered top-left, separate from the swing HUD (top-right).
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "hole_hud"),
				HoleHud::render);
	}
}
