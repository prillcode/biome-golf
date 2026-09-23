package com.prillcode.minecraftgolf.hole;

import com.prillcode.minecraftgolf.golf.DistanceUnit;

/**
 * M10.3 S3: pure formatting for the client-light (vanilla Java / Bedrock) presentation
 * fallbacks. Kept free of Minecraft types so it stays unit-testable; the server-side
 * {@code ClientLightPresentation} owns delivery and the {@code !canSend} gate.
 *
 * <p>Distances arrive as authoritative block counts from the server snapshot; this class
 * only renders them.</p>
 */
public final class ClientLightHudText {

	private ClientLightHudText() {
	}

	/** Active-hole action bar: hole, par, strokes, distance to the cup and last shot. */
	public static String active(int holeNumber, int par, int strokes, int strokeLimit,
			int distanceToCupBlocks, int shotDistanceBlocks) {
		StringBuilder line = new StringBuilder("[golf] Hole ").append(holeNumber)
			.append(" Par ").append(par)
			.append(" | strokes ").append(strokes).append('/').append(strokeLimit);
		if (distanceToCupBlocks >= 0) {
			line.append(" | cup ").append(DistanceUnit.BLOCKS.format(distanceToCupBlocks));
		}
		if (shotDistanceBlocks >= 0) {
			line.append(" | shot ").append(DistanceUnit.BLOCKS.format(shotDistanceBlocks));
		}
		return line.toString();
	}

	/** Terminal-hole action bar: the scored result plus the running course total. */
	public static String complete(int holeNumber, int par, int strokes, int scoreToPar,
			String termLabel, int courseStrokes, int courseParPlayed, int courseTotalPar) {
		StringBuilder line = new StringBuilder("[golf] Hole ").append(holeNumber)
			.append(" Par ").append(par)
			.append(" | ").append(strokes).append(" strokes (").append(toPar(scoreToPar)).append(')');
		if (termLabel != null && !termLabel.isBlank()) {
			line.append(' ').append(termLabel);
		}
		if (courseTotalPar > 0) {
			line.append(" | course ").append(courseStrokes).append('/').append(courseTotalPar)
				.append(" (").append(toPar(courseStrokes - courseParPlayed)).append(')');
		}
		return line.toString();
	}

	/** Practice action bar; names the configured hole when there is one. */
	public static String practice(int holeNumber, int par, int shotDistanceBlocks) {
		if (holeNumber == 0 && shotDistanceBlocks < 0) {
			return "[golf] No course — /golf browse or /golf round list";
		}
		StringBuilder line = new StringBuilder("[golf] Practice");
		if (holeNumber > 0) {
			line.append(" — Hole ").append(holeNumber).append(" Par ").append(par);
		}
		if (shotDistanceBlocks >= 0) {
			line.append(" | shot ").append(DistanceUnit.BLOCKS.format(shotDistanceBlocks));
		}
		return line.toString();
	}

	/** Recovery action bar when the assigned ball is gone. */
	public static String missingBall() {
		return "[golf] Ball missing — /golf hole restart";
	}

	/** Shared score-to-par label, matching the modded HUD's short form. */
	public static String toPar(int scoreToPar) {
		if (scoreToPar == 0) {
			return "E";
		}
		return scoreToPar > 0 ? "+" + scoreToPar : Integer.toString(scoreToPar);
	}
}
