package pro.apdev.biomegolf.client.hole;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import pro.apdev.biomegolf.net.HoleStatePayload;
import pro.apdev.biomegolf.net.NextHoleRequestPayload;

/** Opens the M7 no-text-input advance prompt once per eligible hole barrier. */
public final class NextHolePrompt {

	private boolean offeredForCurrentEligibility;

	public void tick(Minecraft client) {
		HoleStatePayload state = HoleHudState.get();
		boolean eligible = state != null && state.roundAdvanceAvailable();
		if (!eligible) {
			offeredForCurrentEligibility = false;
			if (client.gui.screen() instanceof NextHolePromptScreen) {
				client.setScreenAndShow(null);
			}
			return;
		}
		if (!offeredForCurrentEligibility && client.gui.screen() == null) {
			offeredForCurrentEligibility = true;
			client.setScreenAndShow(new NextHolePromptScreen(this::requestAdvance));
		}
	}

	private void requestAdvance() {
		if (ClientPlayNetworking.canSend(NextHoleRequestPayload.TYPE)) {
			ClientPlayNetworking.send(NextHoleRequestPayload.INSTANCE);
		}
	}
}
