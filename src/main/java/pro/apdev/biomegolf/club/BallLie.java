package pro.apdev.biomegolf.club;

/**
 * Contextual lie of a resting ball, used for shot resolution (M8.12).
 *
 * <p>The first bounded slice of PRD §12 lie behavior: only tee-versus-deck matters
 * today, and only the Driver is tee-optimized, so a {@link #DECK} Driver loses its
 * tee-only flight. The type is Minecraft-free so lie resolution stays unit-testable.</p>
 */
public enum BallLie {
	/** On a tee (authored hole tee or practice tee): full club performance. */
	TEE("Tee"),
	/** On the ground, away from any tee: a Driver loses its tee-only flight. */
	DECK("Deck");

	private final String displayName;

	BallLie(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	/** Defensive wire decode; unknown values fall back to the neutral tee lie. */
	public static BallLie fromWire(int value) {
		return value >= 0 && value < values().length ? values()[value] : TEE;
	}
}
