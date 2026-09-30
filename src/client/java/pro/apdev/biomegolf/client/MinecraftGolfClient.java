package pro.apdev.biomegolf.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.Identifier;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.client.camera.PostShotCamera;
import pro.apdev.biomegolf.client.hole.HoleHud;
import pro.apdev.biomegolf.client.hole.HoleHudState;
import pro.apdev.biomegolf.client.hole.CourseBrowserScreen;
import pro.apdev.biomegolf.client.hole.CourseBrowserState;
import pro.apdev.biomegolf.client.hole.LobbyHudState;
import pro.apdev.biomegolf.client.hole.NextHolePrompt;
import pro.apdev.biomegolf.client.hole.RoundScorecardScreen;
import pro.apdev.biomegolf.client.hole.RoundScorecardState;
import pro.apdev.biomegolf.client.input.PracticeKeybindings;
import pro.apdev.biomegolf.client.input.HudVisibility;
import pro.apdev.biomegolf.client.render.GolfBallEntityRenderer;
import pro.apdev.biomegolf.client.swing.SwingController;
import pro.apdev.biomegolf.client.swing.SwingHud;
import pro.apdev.biomegolf.entity.GolfBallEntities;
import pro.apdev.biomegolf.net.HoleStatePayload;
import pro.apdev.biomegolf.net.ToggleHudPayload;
import net.minecraft.network.chat.Component;
import pro.apdev.biomegolf.net.RoundScorecardPayload;
import pro.apdev.biomegolf.net.CourseListPayload;
import pro.apdev.biomegolf.net.LobbyStatePayload;

public class MinecraftGolfClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MinecraftGolf.LOGGER.info("{} client initialized", MinecraftGolf.MOD_NAME);

		// Minecraft 26.2 uses the render-state submit pipeline. Registration stays
		// client-only; Fabric's transitive access widener exposes this vanilla API.
		EntityRenderers.register(GolfBallEntities.GOLF_BALL, GolfBallEntityRenderer::new);

		PostShotCamera postShotCamera = new PostShotCamera();
		SwingController swing = new SwingController(postShotCamera);
		PracticeKeybindings practiceKeys = new PracticeKeybindings(swing);
		// /golf stays server-owned: a client root shadows sibling commands in Fabric 26.2.
		ClientPlayNetworking.registerGlobalReceiver(ToggleHudPayload.TYPE, (payload, context) -> {
			boolean visible = HudVisibility.toggle();
			context.player().sendOverlayMessage(Component.literal(
				"[golf] HUDs " + (visible ? "shown" : "hidden")));
		});
		NextHolePrompt nextHolePrompt = new NextHolePrompt();
		ClientTickEvents.END_CLIENT_TICK.register(swing::tick);
		ClientTickEvents.END_CLIENT_TICK.register(practiceKeys::tick);
		ClientTickEvents.END_CLIENT_TICK.register(postShotCamera::tick);
		ClientTickEvents.END_CLIENT_TICK.register(nextHolePrompt::tick);
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
			(payload, context) -> {
				HoleHudState.update(payload);
				if (payload.phase() != HoleStatePayload.Phase.ROUND_COMPLETE) {
					RoundScorecardState.clear();
				}
			});
		ClientPlayNetworking.registerGlobalReceiver(RoundScorecardPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> {
				RoundScorecardState.update(payload);
				Minecraft.getInstance().setScreenAndShow(new RoundScorecardScreen());
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(CourseListPayload.TYPE, (payload, context) ->
			Minecraft.getInstance().execute(() -> {
				CourseBrowserState.update(payload);
				Minecraft.getInstance().setScreenAndShow(new CourseBrowserScreen());
			}));
		ClientPlayNetworking.registerGlobalReceiver(LobbyStatePayload.TYPE, (payload, context) ->
			LobbyHudState.update(payload));

		// S03: clear stale display state on disconnect.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			HoleHudState.clear();
			CourseBrowserState.clear();
			LobbyHudState.clear();
			RoundScorecardState.clear();
		});

		// S03: hole HUD panel — rendered top-left, separate from the swing HUD (top-right).
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "hole_hud"),
				HoleHud::render);
	}
}
