package com.prillcode.minecraftgolf.client.hole;

import com.prillcode.minecraftgolf.net.LobbyStatePayload;

/** Client cache for the latest authoritative lobby snapshot. */
public final class LobbyHudState {
	private static LobbyStatePayload current;
	private LobbyHudState() {}
	public static void update(LobbyStatePayload payload) { current = payload; }
	public static LobbyStatePayload get() { return current; }
	public static void clear() { current = null; }
}
