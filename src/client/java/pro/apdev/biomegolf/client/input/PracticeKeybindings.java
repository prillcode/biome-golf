package pro.apdev.biomegolf.client.input;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.client.DistanceDisplayState;
import pro.apdev.biomegolf.client.hole.GolfMenuScreen;
import pro.apdev.biomegolf.client.hole.LobbyHudState;
import pro.apdev.biomegolf.client.hole.HoleHudState;
import pro.apdev.biomegolf.client.swing.SwingController;
import pro.apdev.biomegolf.net.HoleStatePayload;
import pro.apdev.biomegolf.net.RoundLobbyActionPayload;

/** Client-only practice shortcuts; server commands retain authority and permissions. */
public final class PracticeKeybindings {

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "controls"));

	private final KeyMapping dropBall = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.minecraft_golf.drop_ball",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			CATEGORY));
	private final KeyMapping golfMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.minecraft_golf.start_round", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
	private final KeyMapping leaveRound = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.minecraft_golf.leave_round", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, CATEGORY));
	private final KeyMapping shotCycle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.minecraft_golf.shot_cycle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));
	private final KeyMapping distanceUnits = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.minecraft_golf.distance_units", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY));
	private final SwingController swing;

	public PracticeKeybindings(SwingController swing) {
		this.swing = swing;
	}

	public void tick(Minecraft client) {
		while (dropBall.consumeClick()) {
			if (client.player != null && client.getConnection() != null) {
				HoleStatePayload state = HoleHudState.get();
				if (state != null && state.phase() == HoleStatePayload.Phase.ACTIVE && state.tapInAvailable()) {
					client.getConnection().sendCommand("golf tapin");
				} else {
					client.getConnection().sendCommand("golf practice ball");
				}
			}
		}
		while (shotCycle.consumeClick()) swing.cycleShotType(client);
		while (distanceUnits.consumeClick()) {
			if (client.player != null) {
				client.player.sendOverlayMessage(
						Component.literal("Distance units: " + DistanceDisplayState.toggle().label()));
			}
		}
		while (golfMenu.consumeClick()) {
			if (client.gui.screen() == null) client.setScreenAndShow(new GolfMenuScreen());
		}
		while (leaveRound.consumeClick()) {
			sendAction(RoundLobbyActionPayload.Action.LEAVE);
		}
	}
	private static void sendAction(RoundLobbyActionPayload.Action action) {
		if (LobbyHudState.get() != null && LobbyHudState.get().participating()
				&& ClientPlayNetworking.canSend(RoundLobbyActionPayload.TYPE))
			ClientPlayNetworking.send(new RoundLobbyActionPayload(action, LobbyHudState.get().roundId().toString()));
	}
}
