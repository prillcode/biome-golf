package com.prillcode.minecraftgolf.net;

import java.util.ArrayList;
import java.util.List;

import com.prillcode.minecraftgolf.hole.HoleScoringDisplay;

/**
 * M10.3 S3: renders a {@link RoundScorecardPayload} as plain chat lines for clients that
 * cannot receive the scorecard screen (vanilla Java and Bedrock through Geyser).
 *
 * <p>Pure formatting so it is unit-testable without a Minecraft runtime; the caller owns
 * delivery. Payload shape and the modded scorecard screen are untouched.</p>
 */
public final class RoundScorecardText {

	private RoundScorecardText() {
	}

	/** One header line, then a per-hole block and total for each authoritative player row. */
	public static List<String> lines(RoundScorecardPayload payload) {
		List<String> lines = new ArrayList<>();
		lines.add("[golf] FINAL SCORECARD — " + payload.courseId());
		for (RoundScorecardPayload.PlayerRow player : payload.players()) {
			lines.add(player.name() + ":");
			int totalStrokes = 0;
			int totalPar = 0;
			for (int index = 0; index < payload.holes().size(); index++) {
				RoundScorecardPayload.HoleColumn hole = payload.holes().get(index);
				int strokes = player.strokes().get(index);
				if (strokes < 0) {
					continue;
				}
				totalStrokes += strokes;
				totalPar += hole.par();
				lines.add("  Hole " + hole.number() + " — " + strokes + " strokes on Par "
					+ hole.par() + " (" + HoleScoringDisplay.formatToPar(strokes - hole.par()) + ")");
			}
			lines.add("  Total — " + totalStrokes + " strokes on Par " + totalPar
				+ " (" + HoleScoringDisplay.formatToPar(totalStrokes - totalPar) + ")");
		}
		return lines;
	}
}
