package pro.apdev.biomegolf.course;

import java.util.Objects;

/** Stable identity for an authored generated layout and its rebuild version. */
public record GeneratedLayoutIdentity(String id, int version) {

	public GeneratedLayoutIdentity {
		Objects.requireNonNull(id, "id");
		if (id.isBlank()) {
			throw new IllegalArgumentException("layout id must not be blank");
		}
		if (version <= 0) {
			throw new IllegalArgumentException("layout version must be positive");
		}
	}
}
