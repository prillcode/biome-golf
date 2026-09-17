package com.prillcode.minecraftgolf.client.hole;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.prillcode.minecraftgolf.net.CourseListPayload;
import com.prillcode.minecraftgolf.net.RoundLobbyActionPayload;

/** Client-only rendering of the server-owned round browser projection. */
public final class CourseBrowserScreen extends Screen {
	private static final int PAGE_SIZE = 6;
	private int page;

	public CourseBrowserScreen() { super(Component.literal("Play a Round")); }

	@Override
	protected void init() {
		CourseListPayload projection = CourseBrowserState.get();
		List<BrowserRow> rows = rows(projection);
		if (rows.isEmpty()) {
			addRenderableWidget(Button.builder(Component.literal("No finalized courses"), ignored -> {})
				.bounds(width / 2 - 110, 48, 220, 20).build());
		} else {
			int pages = Math.max(1, (rows.size() + PAGE_SIZE - 1) / PAGE_SIZE);
			page = Math.min(page, pages - 1);
			int start = page * PAGE_SIZE;
			for (int index = start; index < Math.min(rows.size(), start + PAGE_SIZE); index++) {
				BrowserRow row = rows.get(index);
				int y = 48 + (index - start) * 24;
				addRenderableWidget(Button.builder(Component.literal(row.label()), ignored -> send(row))
					.bounds(width / 2 - 150, y, 300, 20).build());
			}
			if (page > 0) {
				addRenderableWidget(Button.builder(Component.literal("Previous"), ignored -> changePage(-1))
					.bounds(width / 2 - 150, 48 + PAGE_SIZE * 24, 96, 20).build());
			}
			if (page + 1 < pages) {
				addRenderableWidget(Button.builder(Component.literal("Next"), ignored -> changePage(1))
					.bounds(width / 2 + 54, 48 + PAGE_SIZE * 24, 96, 20).build());
			}
		}
		addRenderableWidget(Button.builder(Component.literal("Cancel"), ignored -> onClose())
			.bounds(width / 2 - 60, height - 32, 120, 20).build());
	}

	private static List<BrowserRow> rows(CourseListPayload projection) {
		if (projection == null) return List.of();
		List<BrowserRow> rows = new ArrayList<>();
		for (CourseListPayload.LobbyEntry lobby : projection.lobbies()) {
			rows.add(new BrowserRow(RoundLobbyActionPayload.Action.JOIN, lobby.roundId().toString(),
				"Join " + lobby.courseName() + " | " + lobby.coordinatorName() + " | "
					+ lobby.participantCount() + "/" + lobby.capacity()));
		}
		for (CourseListPayload.CourseEntry course : projection.courses()) {
			rows.add(new BrowserRow(RoundLobbyActionPayload.Action.CREATE, course.id(),
				"Create " + course.displayName() + " (" + course.holeCount() + " holes, par "
					+ course.totalPar() + ")"));
		}
		return rows;
	}

	private void changePage(int delta) {
		page += delta;
		rebuildWidgets();
	}

	private void send(BrowserRow row) {
		if (ClientPlayNetworking.canSend(RoundLobbyActionPayload.TYPE)) {
			ClientPlayNetworking.send(new RoundLobbyActionPayload(row.action(), row.targetId()));
		}
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractTransparentBackground(graphics);
		graphics.centeredText(font, "Open lobbies and new rounds", width / 2, 20, 0xFFFFFFFF);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override public boolean isPauseScreen() { return false; }
	@Override public boolean isInGameUi() { return true; }

	private record BrowserRow(RoundLobbyActionPayload.Action action, String targetId, String label) {}
}
