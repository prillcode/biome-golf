package com.prillcode.minecraftgolf.client.input;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import com.prillcode.minecraftgolf.MinecraftGolf;

/** Client-only practice shortcuts; server commands retain authority and permissions. */
public final class PracticeKeybindings {

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "controls"));

	private final KeyMapping dropBall = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.minecraft_golf.drop_ball",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			CATEGORY));

	public void tick(Minecraft client) {
		while (dropBall.consumeClick()) {
			if (client.player != null && client.getConnection() != null) {
				// The player-facing command validates practice mode and performs the spawn
				// server-side. Operator /golf spawn remains a separate debug tool.
				client.getConnection().sendCommand("golf practiceball");
			}
		}
	}
}
