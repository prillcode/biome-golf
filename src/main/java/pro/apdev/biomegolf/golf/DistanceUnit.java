package pro.apdev.biomegolf.golf;

/** Display units for golf distances; gameplay and network values remain blocks. */
public enum DistanceUnit {
	BLOCKS("blocks", 1.0),
	YARDS("yards", 1.75);

	private final String label;
	private final double blocksMultiplier;

	DistanceUnit(String label, double blocksMultiplier) {
		this.label = label;
		this.blocksMultiplier = blocksMultiplier;
	}

	public String label() {
		return label;
	}

	public int rounded(double blocks) {
		return (int) Math.round(blocks * blocksMultiplier);
	}

	public String format(double blocks) {
		return "%d %s".formatted(rounded(blocks), label);
	}

	public DistanceUnit toggled() {
		return this == YARDS ? BLOCKS : YARDS;
	}
}
