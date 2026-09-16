package com.prillcode.minecraftgolf.client.hole;

import com.prillcode.minecraftgolf.net.RoundScorecardPayload;

/** Client presentation cache for the last server-authoritative scorecard. */
public final class RoundScorecardState {
	private static RoundScorecardPayload payload;

	private RoundScorecardState() {}

	public static RoundScorecardPayload get() { return payload; }
	public static void update(RoundScorecardPayload value) { payload = value; }
	public static void clear() { payload = null; }
}
