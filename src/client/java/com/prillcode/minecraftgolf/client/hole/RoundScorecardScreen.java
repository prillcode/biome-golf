package com.prillcode.minecraftgolf.client.hole;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.prillcode.minecraftgolf.net.RoundActionPayload;
import com.prillcode.minecraftgolf.net.LobbyStatePayload;
import com.prillcode.minecraftgolf.net.RoundScorecardPayload;

/** Paginated end-round presentation; all scores and pars come from the server payload. */
public final class RoundScorecardScreen extends Screen {
	private static final int HOLES_PER_PAGE = 18;
	private int page;

	public RoundScorecardScreen() { super(Component.literal("Final scorecard")); }

	@Override
	protected void init() {
		RoundScorecardPayload payload = RoundScorecardState.get();
		int pageCount = payload == null ? 1 : Math.max(1,
			(payload.holes().size() + HOLES_PER_PAGE - 1) / HOLES_PER_PAGE);
		if (page > 0) addRenderableWidget(Button.builder(Component.literal("Previous"), ignored -> changePage(-1))
			.bounds(width / 2 - 155, height - 74, 140, 20).build());
		if (page + 1 < pageCount) addRenderableWidget(Button.builder(Component.literal("Next"), ignored -> changePage(1))
			.bounds(width / 2 + 15, height - 74, 140, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Replay"), ignored -> action(RoundActionPayload.Action.REPLAY))
			.bounds(width / 2 - 155, height - 48, 140, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Leave Round"), ignored -> action(RoundActionPayload.Action.LEAVE))
			.bounds(width / 2 + 15, height - 48, 140, 20).build());
	}

	private void changePage(int delta) { page += delta; rebuildWidgets(); }

	private void action(RoundActionPayload.Action action) {
		LobbyStatePayload lobby = LobbyHudState.get();
		if (lobby != null && lobby.phase() == LobbyStatePayload.Phase.COMPLETE
				&& ClientPlayNetworking.canSend(com.prillcode.minecraftgolf.net.RoundLobbyActionPayload.TYPE)) {
			var targetedAction = action == RoundActionPayload.Action.REPLAY
				? com.prillcode.minecraftgolf.net.RoundLobbyActionPayload.Action.REPLAY_ROUND
				: com.prillcode.minecraftgolf.net.RoundLobbyActionPayload.Action.LEAVE;
			ClientPlayNetworking.send(new com.prillcode.minecraftgolf.net.RoundLobbyActionPayload(
				targetedAction, lobby.roundId().toString()));
		} else if (ClientPlayNetworking.canSend(RoundActionPayload.TYPE)) {
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
		int start = page * HOLES_PER_PAGE;
		int end = Math.min(payload.holes().size(), start + HOLES_PER_PAGE);
		int left = Math.max(8, width / 2 - 220);
		graphics.centeredText(font, "FINAL SCORECARD", width / 2, 18, 0xFFFFCC44);
		graphics.centeredText(font, payload.courseId(), width / 2, 32, 0xFFCCCCCC);
		StringBuilder header = new StringBuilder("Hole       ");
		StringBuilder par = new StringBuilder("Par        ");
		for (int index = start; index < end; index++) {
			header.append(payload.holes().get(index).number()).append("  ");
			par.append(payload.holes().get(index).par()).append("  ");
		}
		graphics.text(font, header.toString(), left, 54, 0xFFFFFFFF, true);
		graphics.text(font, par.toString(), left, 70, 0xFFCCCCCC, false);
		int y = 88;
		for (RoundScorecardPayload.PlayerRow player : payload.players()) {
			StringBuilder row = new StringBuilder(player.name().substring(0, Math.min(player.name().length(), 8)));
			while (row.length() < 11) row.append(' ');
			for (int index = start; index < end; index++) {
				int strokes = player.strokes().get(index);
				row.append(strokes < 0 ? "--" : strokes).append("  ");
			}
			graphics.text(font, row.toString(), left, y, 0xFFFFFFFF, false);
			int total = player.strokes().stream().filter(value -> value >= 0).mapToInt(Integer::intValue).sum();
			int playedPar = java.util.stream.IntStream.range(0, payload.holes().size())
				.filter(index -> player.strokes().get(index) >= 0).map(index -> payload.holes().get(index).par()).sum();
			graphics.text(font, "Total " + total + " (" + formatToPar(total - playedPar) + ")", left, y + 14,
				0xFFFFCC44, true);
			y += 32;
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private static String formatToPar(int score) { return score == 0 ? "E" : score > 0 ? "+" + score : Integer.toString(score); }
	@Override public boolean isPauseScreen() { return false; }
	@Override public boolean isInGameUi() { return true; }
}
