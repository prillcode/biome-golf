package com.prillcode.minecraftgolf.client.hole;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small non-pausing screen with the player-initiated next-tee action. */
final class NextHolePromptScreen extends Screen {

	private final Runnable advanceAction;

	NextHolePromptScreen(Runnable advanceAction) {
		super(Component.literal("Hole complete"));
		this.advanceAction = advanceAction;
	}

	@Override
	protected void init() {
		int buttonWidth = 150;
		int centerX = width / 2;
		int centerY = height / 2;
		addRenderableWidget(Button.builder(Component.literal("Go to next tee"), button -> {
			advanceAction.run();
			onClose();
		}).bounds(centerX - buttonWidth / 2, centerY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Not now"), button -> onClose())
			.bounds(centerX - buttonWidth / 2, centerY + 26, buttonWidth, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float partialTick) {
		extractTransparentBackground(graphics);
		graphics.centeredText(font, "All golfers are complete", width / 2, height / 2 - 36,
			0xFFFFFFFF);
		graphics.centeredText(font, "Advance everyone together?", width / 2, height / 2 - 22,
			0xFFCCCCCC);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}
}
