package pro.apdev.biomegolf.club;

import java.util.List;

import pro.apdev.biomegolf.ball.ShotPhysicsProfile;

/** Server-owned shot intent and the contextual choices available for each club. */
public enum ShotType {
	STANDARD("Standard"),
	CHIP("Chip"),
	STINGER("Stinger"),
	FLOP("Flop");

	private final String displayName;

	ShotType(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	public boolean allowedFor(ClubDefinition club) {
		return this == STANDARD || allowedTypes(club).contains(this);
	}

	public static List<ShotType> choicesFor(ClubDefinition club) {
		return List.copyOf(allowedTypes(club));
	}

	public static ShotType fromWire(int value) {
		return value >= 0 && value < values().length ? values()[value] : STANDARD;
	}

	/** Standard retains the existing club-specific loft profile. */
	public ShotPhysicsProfile profile(ClubDefinition club) {
		if (this == STANDARD) {
			return club.putting() ? ShotPhysicsProfile.STANDARD : ShotPhysicsProfile.LOFTED_CLUB;
		}
		return switch (this) {
			case CHIP -> ShotPhysicsProfile.CHIP;
			case STINGER -> ShotPhysicsProfile.STINGER;
			case FLOP -> ShotPhysicsProfile.FLOP;
			case STANDARD -> throw new AssertionError();
		};
	}

	private static List<ShotType> allowedTypes(ClubDefinition club) {
		return switch (club.id()) {
			case "driver", "fairway_wood", "long_iron" -> List.of(STINGER);
			case "mid_iron" -> List.of(CHIP);
			case "short_iron", "wedge" -> List.of(CHIP, FLOP);
			default -> List.of();
		};
	}
}
