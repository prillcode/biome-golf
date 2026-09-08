package com.prillcode.minecraftgolf.client.hole;

import com.prillcode.minecraftgolf.net.HoleStatePayload;

/**
 * Client-only store for the last authoritative hole-state snapshot received
 * from the server (S03). Updated on the Minecraft client thread; null means
 * no snapshot has been received yet or the display was cleared on disconnect.
 */
public final class HoleHudState {

	private static HoleStatePayload current;

	private HoleHudState() {
	}

	public static void update(HoleStatePayload payload) {
		current = payload;
	}

	public static void clear() {
		current = null;
	}

	public static HoleStatePayload get() {
		return current;
	}
}
