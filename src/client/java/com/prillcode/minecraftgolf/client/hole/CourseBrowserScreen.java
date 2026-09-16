package com.prillcode.minecraftgolf.client.hole;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.prillcode.minecraftgolf.net.CourseListPayload;
import com.prillcode.minecraftgolf.net.RoundLobbyActionPayload;

/** Small client-only course browser; all choices are revalidated by the server. */
public final class CourseBrowserScreen extends Screen {
	public CourseBrowserScreen() { super(Component.literal("Play a Round")); }
	@Override protected void init() {
		CourseListPayload list = CourseBrowserState.get();
		int y = height / 2 - Math.min(80, (list == null ? 0 : list.courses().size() * 24) / 2);
		if (list == null || list.courses().isEmpty()) {
			addRenderableWidget(Button.builder(Component.literal("No finalized courses"), button -> {}).bounds(width / 2 - 110, y, 220, 20).build());
		} else {
			for (CourseListPayload.CourseEntry course : list.courses()) {
				int rowY = y;
				boolean sameLobby = LobbyHudState.get() != null
					&& LobbyHudState.get().phase() == com.prillcode.minecraftgolf.net.LobbyStatePayload.Phase.LOBBY
					&& LobbyHudState.get().courseId().equals(course.id());
				boolean anotherLobby = LobbyHudState.get() != null
					&& LobbyHudState.get().phase() == com.prillcode.minecraftgolf.net.LobbyStatePayload.Phase.LOBBY
					&& !sameLobby;
				String action = anotherLobby ? "Unavailable: " : sameLobby ? "Join " : "Play ";
				addRenderableWidget(Button.builder(Component.literal(action + course.displayName() + "  (" + course.holeCount() + " holes, par " + course.totalPar() + ")"),
					button -> { if (anotherLobby) return; if (sameLobby) join(course.id()); else create(course.id()); }).bounds(width / 2 - 140, rowY, 280, 20).build());
				y += 24;
			}
		}
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
			.bounds(width / 2 - 60, Math.min(height - 32, y + 8), 120, 20).build());
	}
	private void create(String courseId) {
		if (ClientPlayNetworking.canSend(RoundLobbyActionPayload.TYPE)) {
			ClientPlayNetworking.send(new RoundLobbyActionPayload(RoundLobbyActionPayload.Action.CREATE, courseId));
		}
		onClose();
	}
	private void join(String courseId) {
		if (ClientPlayNetworking.canSend(RoundLobbyActionPayload.TYPE))
			ClientPlayNetworking.send(new RoundLobbyActionPayload(RoundLobbyActionPayload.Action.JOIN, courseId));
		onClose();
	}
	@Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractTransparentBackground(graphics);
		graphics.centeredText(font, "Choose a finalized course", width / 2, 20, 0xFFFFFFFF);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}
	@Override public boolean isPauseScreen() { return false; }
	@Override public boolean isInGameUi() { return true; }
}
