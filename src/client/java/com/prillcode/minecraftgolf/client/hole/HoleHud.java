package com.prillcode.minecraftgolf.client.hole;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.prillcode.minecraftgolf.net.HoleStatePayload;
import com.prillcode.minecraftgolf.net.HoleStatePayload.Phase;
import com.prillcode.minecraftgolf.golf.CupDirection;

/** Client-only HUD panel showing authoritative hole progress from server snapshots (S03). */
public final class HoleHud {

	private static final int WIDTH = 160;
	private static final int EDGE_MARGIN = 8;
	private static final int LINE_HEIGHT = 10;
	private static final int PAD_X = 8;
	private static final int PAD_Y = 6;

	private static final int PANEL = 0xB0101010;
	private static final int BORDER = 0xD0FFFFFF;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFFB8B8B8;
	private static final int WARN = 0xFFFF6644;
	private static final int GOOD = 0xFF55CC55;
	private static final int GOLD = 0xFFFFCC44;

	private HoleHud() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker ignored) {
		HoleStatePayload state = HoleHudState.get();
		if (state == null || state.phase() == Phase.PRACTICE) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		int lines = countLines(state);
		int height = PAD_Y * 2 + lines * LINE_HEIGHT - 2;
		int x = EDGE_MARGIN;
		int y = EDGE_MARGIN;

		graphics.fill(x, y, x + WIDTH, y + height, PANEL);
		outline(graphics, x, y, WIDTH, height, BORDER);

		int textY = y + PAD_Y;
		String holePar = "Hole " + state.holeNumber() + "  Par " + state.par();
		graphics.text(client.font, holePar, x + PAD_X, textY, TEXT, true);
		textY += LINE_HEIGHT;

		String strokes = "Strokes: " + state.strokes() + " / " + state.strokeLimit();
		graphics.text(client.font, strokes, x + PAD_X, textY, MUTED, false);
		textY += LINE_HEIGHT;
		if (state.courseTotalPar() > 0) {
			int courseScore = state.courseStrokes() - state.courseParPlayed();
			String cumulative = "Course: " + state.courseStrokes() + " strokes  "
				+ formatToPar(courseScore);
			graphics.text(client.font, cumulative, x + PAD_X, textY, GOLD, false);
			textY += LINE_HEIGHT;
		}

		if (state.phase() == Phase.ACTIVE) {
			String arrow = CupDirection.arrow(client.player.getX(), client.player.getZ(),
				client.player.getYRot(), state.cupX(), state.cupZ());
			String cupDirection = "Cup: " + arrow + "  " + state.distanceToCupBlocks() + " blocks";
			graphics.text(client.font, cupDirection, x + PAD_X, textY, GOLD, true);
			textY += LINE_HEIGHT;
		}

		if (state.penaltyCount() > 0) {
			String penalties = "Penalties: " + state.penaltyCount();
			graphics.text(client.font, penalties, x + PAD_X, textY, WARN, false);
			textY += LINE_HEIGHT;
		}

		if (state.strokes() > 0) {
			String score = "Score: " + formatToPar(state.scoreToPar());
			int scoreColor = state.scoreToPar() < 0 ? GOOD : state.scoreToPar() > 0 ? WARN : MUTED;
			graphics.text(client.font, score, x + PAD_X, textY, scoreColor, false);
			textY += LINE_HEIGHT;
		}

		if (state.phase() == Phase.MISSING_BALL) {
			graphics.text(client.font, "RECOVERY NEEDED", x + PAD_X, textY, WARN, true);
			textY += LINE_HEIGHT;
			graphics.text(client.font, "/golf hole restart", x + PAD_X, textY, MUTED, false);
		} else if (state.phase() == Phase.COMPLETE) {
			String termLabel = state.scoreTerm() != null
				? state.scoreTerm().name().replace('_', ' ')
				: "NO SCORE";
			graphics.text(client.font, "COMPLETE: " + termLabel, x + PAD_X, textY, GOLD, true);
			textY += LINE_HEIGHT;
			if (state.roundAdvanceAvailable()) {
				graphics.text(client.font, "NEXT TEE READY", x + PAD_X, textY, GOOD, true);
			}
		} else if (state.phase() == Phase.ROUND_COMPLETE) {
			graphics.text(client.font, "ROUND COMPLETE", x + PAD_X, textY, GOLD, true);
			textY += LINE_HEIGHT;
			graphics.text(client.font, "Replay: /golf round restart", x + PAD_X, textY, MUTED, false);
			textY += LINE_HEIGHT;
			graphics.text(client.font, "Exit: /golf round done", x + PAD_X, textY, MUTED, false);
		}
	}

	private static int countLines(HoleStatePayload state) {
		int lines = 2; // hole/par + strokes
		if (state.courseTotalPar() > 0) lines++;
		if (state.phase() == Phase.ACTIVE) lines++;
		if (state.penaltyCount() > 0) lines++;
		if (state.strokes() > 0) lines++;
		if (state.phase() == Phase.MISSING_BALL) lines += 2;
		else if (state.phase() == Phase.COMPLETE) {
			lines++;
			if (state.roundAdvanceAvailable()) lines++;
		} else if (state.phase() == Phase.ROUND_COMPLETE) lines += 3;
		return lines;
	}

	private static String formatToPar(int scoreToPar) {
		if (scoreToPar == 0) {
			return "E";
		}
		return scoreToPar > 0 ? "+" + scoreToPar : Integer.toString(scoreToPar);
	}

	private static void outline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}
}
