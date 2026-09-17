package com.prillcode.minecraftgolf.client.input;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.client.hole.GolfMenuScreen;
import com.prillcode.minecraftgolf.client.hole.LobbyHudState;
import com.prillcode.minecraftgolf.net.RoundLobbyActionPayload;

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

	public void tick(Minecraft client) {
		while (dropBall.consumeClick()) {
			if (client.player != null && client.getConnection() != null) {
				// The player-facing command validates practice mode and performs the spawn
				// server-side. Operator /golf spawn remains a separate debug tool.
				client.getConnection().sendCommand("golf practice ball");
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
