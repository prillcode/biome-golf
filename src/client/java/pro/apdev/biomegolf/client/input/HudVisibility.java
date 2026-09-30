package pro.apdev.biomegolf.client.input;

/** Session-local visibility shared by the Swing and Hole HUDs. */
public final class HudVisibility {

	private static boolean visible = true;

	private HudVisibility() {
	}

	public static boolean isVisible() {
		return visible;
	}

	public static boolean toggle() {
		visible = !visible;
		return visible;
	}

	/** Reveal both panels after the player tries to take a golf shot. */
	public static void reveal() {
		visible = true;
	}
}
