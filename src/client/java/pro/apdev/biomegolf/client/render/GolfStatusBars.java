package pro.apdev.biomegolf.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;

/**
 * M8.15 client presentation: hide the vanilla survival status bars (hearts, hunger,
 * and experience) while the server has the player in a Survival flight mode (Golf or
 * Builder). Flight-only Golf keeps the normal Survival inventory, so the client still
 * draws the Survival status bars; they are noise during a round and the player is
 * damage-protected.
 *
 * <p>No custom Mixin: this uses Fabric's HUD element registry to wrap the vanilla
 * elements. Detection is ability-based — the Biome Golf mode service is the only reason a
 * non-creative, non-spectator player has mayfly.</p>
 */
public final class GolfStatusBars {

	private GolfStatusBars() {
	}

	public static void register() {
		hideWhileFlying(VanillaHudElements.HEALTH_BAR);
		hideWhileFlying(VanillaHudElements.FOOD_BAR);
		hideWhileFlying(VanillaHudElements.INFO_BAR);
		hideWhileFlying(VanillaHudElements.EXPERIENCE_LEVEL);
	}

	private static void hideWhileFlying(Identifier element) {
		HudElementRegistry.replaceElement(element, original -> (graphics, delta) -> {
			if (!isSurvivalFlightMode()) {
				original.extractRenderState(graphics, delta);
			}
		});
	}

	private static boolean isSurvivalFlightMode() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null
			&& !player.isCreative()
			&& !player.isSpectator()
			&& player.getAbilities().mayfly;
	}
}
