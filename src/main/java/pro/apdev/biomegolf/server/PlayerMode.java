package pro.apdev.biomegolf.server;

/** M8.15: the server-owned player mode for a supported Java golfer. */
public enum PlayerMode {
	/** Creative-style flight and damage protection; shares the normal Survival inventory. */
	GOLF,
	/** Ordinary Survival/Peaceful world play outside protected landscapes. */
	WORLD,
	/** Operator-scoped course construction with flight and an isolated palette loadout. */
	BUILDER;

	public String displayName() {
		return switch (this) {
			case GOLF -> "Golf";
			case WORLD -> "World";
			case BUILDER -> "Builder";
		};
	}
}
