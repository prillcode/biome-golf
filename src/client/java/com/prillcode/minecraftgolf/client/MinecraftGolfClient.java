package com.prillcode.minecraftgolf.client;

import net.fabricmc.api.ClientModInitializer;

import com.prillcode.minecraftgolf.MinecraftGolf;

public class MinecraftGolfClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MinecraftGolf.LOGGER.info("{} client initialized", MinecraftGolf.MOD_NAME);
	}
}
