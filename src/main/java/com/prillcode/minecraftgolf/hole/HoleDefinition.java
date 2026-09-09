package com.prillcode.minecraftgolf.hole;

import java.util.Objects;

import com.prillcode.minecraftgolf.course.GeneratedLayoutIdentity;
import com.prillcode.minecraftgolf.course.HoleTransition;
import com.prillcode.minecraftgolf.golf.Vec3;

/** Immutable, Minecraft-free metadata for one configured golf hole. */
public record HoleDefinition(
	String id,
	int number,
	String dimension,
	Vec3 tee,
	Vec3 cup,
	int par,
	HoleBoundary boundary,
	GeneratedLayoutIdentity generatedLayout,
	HoleTransition transition
) {
	/** Compatibility constructor for the pre-course M4/M4.5 configured hole. */
	public HoleDefinition(
		String id,
		int number,
		String dimension,
		Vec3 tee,
		Vec3 cup,
		int par,
		HoleBoundary boundary
	) {
		this(id, number, dimension, tee, cup, par, boundary,
			new GeneratedLayoutIdentity("legacy:" + id, 1), HoleTransition.at(tee));
	}

	public HoleDefinition {
		requireNonBlank(id, "id");
		requireNonBlank(dimension, "dimension");
		Objects.requireNonNull(tee, "tee");
		Objects.requireNonNull(cup, "cup");
		Objects.requireNonNull(boundary, "boundary");
		Objects.requireNonNull(generatedLayout, "generatedLayout");
		Objects.requireNonNull(transition, "transition");
		if (number <= 0) {
			throw new IllegalArgumentException("hole number must be positive");
		}
		if (par <= 0) {
			throw new IllegalArgumentException("par must be positive");
		}
		try {
			Math.addExact(Math.multiplyExact(par, 2), 2);
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException("par is too large to calculate Double Par plus two", exception);
		}
		if (!boundary.contains(tee)) {
			throw new IllegalArgumentException("tee must be inside the playable boundary");
		}
		if (!boundary.contains(cup)) {
			throw new IllegalArgumentException("cup must be inside the playable boundary");
		}
		if (!boundary.contains(transition.playerPosition())) {
			throw new IllegalArgumentException("transition position must be inside the playable boundary");
		}
	}

	/** Default M4 stroke limit: Double Par plus two. */
	public int strokeLimit() {
		return par * 2 + 2;
	}

	private static void requireNonBlank(String value, String name) {
		Objects.requireNonNull(value, name);
		if (value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
	}
}
