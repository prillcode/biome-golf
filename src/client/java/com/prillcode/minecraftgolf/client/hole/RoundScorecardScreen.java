package com.prillcode.minecraftgolf.client.hole;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.prillcode.minecraftgolf.net.RoundActionPayload;
import com.prillcode.minecraftgolf.net.RoundScorecardPayload;

/** End-round presentation; all scores and pars come from the server payload. */
public final class RoundScorecardScreen extends Screen {
	public RoundScorecardScreen() { super(Component.literal("Final scorecard")); }

	@Override
	protected void init() {
		int y = height - 48;
		addRenderableWidget(Button.builder(Component.literal("Replay"), button -> {
			action(RoundActionPayload.Action.REPLAY);
		}).bounds(width / 2 - 155, y, 140, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Leave Round"), button -> {
			action(RoundActionPayload.Action.LEAVE);
		}).bounds(width / 2 + 15, y, 140, 20).build());
	}

	private void action(RoundActionPayload.Action action) {
		if (ClientPlayNetworking.canSend(RoundActionPayload.TYPE)) {
			ClientPlayNetworking.send(new RoundActionPayload(action));
		}
		RoundScorecardState.clear();
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractTransparentBackground(graphics);
		RoundScorecardPayload payload = RoundScorecardState.get();
		if (payload == null) return;
		int left = Math.max(8, width / 2 - 220);
		graphics.centeredText(font, "FINAL SCORECARD", width / 2, 18, 0xFFFFCC44);
		graphics.centeredText(font, payload.courseId(), width / 2, 32, 0xFFCCCCCC);
		renderNine(graphics, payload, left, 52, 0, "Out");
		int backY = 52 + (payload.players().size() + 4) * 16;
		renderNine(graphics, payload, left, backY, 9, "In");
		int finalY = backY + (payload.players().size() + 4) * 16;
		for (RoundScorecardPayload.PlayerRow player : payload.players()) {
			int total = player.strokes().stream().filter(value -> value >= 0).mapToInt(Integer::intValue).sum();
			graphics.text(font, player.name() + "  Final: " + total, left, finalY, 0xFFFFCC44, true);
			finalY += 16;
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private void renderNine(GuiGraphicsExtractor graphics, RoundScorecardPayload payload,
		int x, int y, int start, String label) {
		StringBuilder header = new StringBuilder(label).append("   ");
		for (int hole = start; hole < start + 9; hole++) header.append(hole + 1).append("  ");
		header.append("Total");
		graphics.text(font, header.toString(), x, y, 0xFFFFFFFF, true);
		StringBuilder parRow = new StringBuilder("Par        ");
		for (int hole = start; hole < start + 9; hole++) {
			int par = payload.pars().get(hole);
			parRow.append(par == 0 ? "--" : Integer.toString(par)).append(" ");
		}
		graphics.text(font, parRow.toString(), x, y + 16, 0xFFCCCCCC, false);
		int rowY = y + 32;
		for (RoundScorecardPayload.PlayerRow player : payload.players()) {
			StringBuilder row = new StringBuilder(player.name().substring(0, Math.min(player.name().length(), 10)));
			while (row.length() < 11) row.append(' ');
			int playerTotal = 0;
			boolean playerPlayed = false;
			for (int hole = start; hole < start + 9; hole++) {
				int strokes = player.strokes().get(hole);
				if (strokes >= 0) { playerTotal += strokes; playerPlayed = true; }
				row.append(strokes < 0 ? "--" : Integer.toString(strokes)).append(" ");
			}
			row.append(playerPlayed ? playerTotal : "--");
			graphics.text(font, row.toString(), x, rowY, 0xFFFFFFFF, false);
			rowY += 16;
		}
		graphics.text(font, label + " total", x, rowY, 0xFFFFCC44, true);
	}

	@Override public boolean isPauseScreen() { return false; }
	@Override public boolean isInGameUi() { return true; }
}
