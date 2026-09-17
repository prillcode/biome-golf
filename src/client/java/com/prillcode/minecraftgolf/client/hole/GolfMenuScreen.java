package com.prillcode.minecraftgolf.client.hole;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.prillcode.minecraftgolf.net.GolfMenuActionPayload;
import com.prillcode.minecraftgolf.net.HoleStatePayload;
import com.prillcode.minecraftgolf.net.LobbyStatePayload;
import com.prillcode.minecraftgolf.net.RoundLobbyActionPayload;

/** Context-sensitive, client-only action menu. The server remains authoritative. */
public final class GolfMenuScreen extends Screen {
	public GolfMenuScreen() { super(Component.literal("Golf Menu")); }

	@Override
	protected void init() {
		HoleStatePayload hole = HoleHudState.get();
		LobbyStatePayload lobby = LobbyHudState.get();
		int y = height / 2 - 30;
		if (hole != null && hole.phase() == HoleStatePayload.Phase.ROUND_COMPLETE) {
			if (lobby != null && lobby.phase() == LobbyStatePayload.Phase.COMPLETE) {
				roundButton("Replay Round", RoundLobbyActionPayload.Action.REPLAY_ROUND, lobby, y);
				roundButton("Leave Round", RoundLobbyActionPayload.Action.LEAVE, lobby, y + 26);
			} else {
				button("Replay Round", GolfMenuActionPayload.Action.REPLAY_ROUND, y);
				button("Leave Round", GolfMenuActionPayload.Action.LEAVE_ROUND, y + 26);
			}
		} else if (lobby != null && lobby.phase() == LobbyStatePayload.Phase.LOBBY
				&& lobby.participating()) {
			if (lobby.coordinator() && lobby.startable()) {
				roundButton("Start Round", RoundLobbyActionPayload.Action.START, lobby, y);
				roundButton("Leave", RoundLobbyActionPayload.Action.LEAVE, lobby, y + 26);
			} else {
				roundButton("Leave", RoundLobbyActionPayload.Action.LEAVE, lobby, y);
			}
		} else if (lobby != null && lobby.phase() == LobbyStatePayload.Phase.PLAYING
				&& lobby.participating()) {
			roundButton("Replay Hole", RoundLobbyActionPayload.Action.RESTART_HOLE, lobby, y);
			roundButton("Leave Round", RoundLobbyActionPayload.Action.LEAVE, lobby, y + 26);
		} else if (hole != null && hole.phase() != HoleStatePayload.Phase.PRACTICE) {
			button("Replay Hole", GolfMenuActionPayload.Action.REPLAY_HOLE, y);
			button("Leave Round", GolfMenuActionPayload.Action.LEAVE_ROUND, y + 26);
		} else {
			button("Play a Round", GolfMenuActionPayload.Action.OPEN_COURSE_BROWSER, y);
		}
		addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
			.bounds(width / 2 - 60, Math.min(height - 28, y + 58), 120, 20).build());
	}

	private void roundButton(String label, RoundLobbyActionPayload.Action action,
			LobbyStatePayload lobby, int y) {
		addRenderableWidget(Button.builder(Component.literal(label), ignored -> {
			if (ClientPlayNetworking.canSend(RoundLobbyActionPayload.TYPE)) {
				ClientPlayNetworking.send(new RoundLobbyActionPayload(action, lobby.roundId().toString()));
			}
			onClose();
		}).bounds(width / 2 - 100, y, 200, 20).build());
	}

	private void button(String label, GolfMenuActionPayload.Action action, int y) {
		addRenderableWidget(Button.builder(Component.literal(label), ignored -> {
			if (ClientPlayNetworking.canSend(GolfMenuActionPayload.TYPE)) {
				ClientPlayNetworking.send(new GolfMenuActionPayload(action));
			}
			onClose();
		}).bounds(width / 2 - 100, y, 200, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractTransparentBackground(graphics);
		graphics.centeredText(font, "Golf Menu", width / 2, 20, 0xFFFFCC44);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}
	@Override public boolean isPauseScreen() { return false; }
	@Override public boolean isInGameUi() { return true; }
}
