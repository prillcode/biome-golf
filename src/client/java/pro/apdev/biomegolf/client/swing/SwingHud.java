package pro.apdev.biomegolf.client.swing;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import pro.apdev.biomegolf.club.SwingMeter;
import pro.apdev.biomegolf.client.input.HudVisibility;

/** Lightweight M3 HUD for club, ball distance, meters, and swing state. */
public final class SwingHud {

	private static final int WIDTH = 180;
	private static final int HEIGHT = 90;
	private static final int BAR_X_OFFSET = 10;
	private static final int BAR_WIDTH = 160;
	private static final int BAR_HEIGHT = 7;
	private static final int EDGE_MARGIN = 8;

	private static final int PANEL = 0xB0101010;
	private static final int BORDER = 0xD0FFFFFF;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFFB8B8B8;
	private static final int POWER = 0xFF55CC55;
	private static final int ACCURACY = 0xFFFFCC44;
	private static final int PERFECT = 0xFF55FF88;
	private static final int TRACK = 0xFF333333;

	private final SwingController controller;

	public SwingHud(SwingController controller) {
		this.controller = controller;
	}

	public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!HudVisibility.isVisible()) return;
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		String clubName = controller.clubName(client.player);
		if (clubName.isEmpty()) {
			return;
		}

		// Keep chat/status text and the centered aiming line clear.
		int x = graphics.guiWidth() - WIDTH - EDGE_MARGIN;
		int y = EDGE_MARGIN;
		graphics.fill(x, y, x + WIDTH, y + HEIGHT, PANEL);
		outline(graphics, x, y, WIDTH, HEIGHT, BORDER);

		graphics.text(client.font, clubName, x + 8, y + 6, TEXT, true);
		String distance = controller.distanceText(client.player);
		int distanceX = x + WIDTH - 8 - client.font.width(distance);
		graphics.text(client.font, distance, distanceX, y + 6, MUTED, true);

		graphics.text(client.font, "Power", x + 8, y + 20, MUTED, false);
		drawPowerBar(graphics, x + BAR_X_OFFSET, y + 31, controller.power());

		graphics.text(client.font, controller.shotTypeText(client.player), x + 8, y + 41, TEXT, true);
		graphics.text(client.font, "Accuracy", x + 8, y + 54, MUTED, false);
		drawAccuracyBar(graphics, x + BAR_X_OFFSET, y + 65, controller.accuracy());

		String state = controller.stateText();
		int stateX = Math.max(x + 8, x + WIDTH - 8 - client.font.width(state));
		graphics.text(client.font, state, stateX, y + 77, TEXT, true);
	}

	private static void drawPowerBar(GuiGraphicsExtractor graphics, int x, int y, float value) {
		graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, TRACK);
		int filled = Math.round(BAR_WIDTH * clamp01(value));
		if (filled > 0) {
			graphics.fill(x, y, x + filled, y + BAR_HEIGHT, POWER);
		}
		outline(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, BORDER);
	}

	private static void drawAccuracyBar(GuiGraphicsExtractor graphics, int x, int y, float value) {
		graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, TRACK);
		// Draw the zone from the same band the resolver treats as zero-deviation.
		int perfectWidth = (int) Math.round(BAR_WIDTH * 2 * SwingMeter.PERFECT_BAND);
		int center = x + BAR_WIDTH / 2;
		graphics.fill(center - perfectWidth / 2, y, center + perfectWidth / 2, y + BAR_HEIGHT, PERFECT);
		int markerCenter = x + Math.round(BAR_WIDTH * clamp01(value));
		graphics.fill(markerCenter - 1, y - 2, markerCenter + 2, y + BAR_HEIGHT + 2, ACCURACY);
		outline(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, BORDER);
	}

	private static void outline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}

	private static float clamp01(float value) {
		return Math.max(0.0f, Math.min(1.0f, value));
	}
}
