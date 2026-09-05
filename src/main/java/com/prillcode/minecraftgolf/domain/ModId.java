package com.prillcode.minecraftgolf.domain;

/**
 * Validation utilities for Fabric mod identifiers.
 *
 * <p>Fabric mod ids must match {@code [a-z][a-z0-9-_]{1,63}}: lowercase letters,
 * digits, hyphens and underscores, starting with a letter, 2-64 characters long.
 */
public final class ModId {
	private static final int MIN_LENGTH = 2;
	private static final int MAX_LENGTH = 64;

	private ModId() {
	}

	public static boolean isValidModId(String id) {
		if (id == null || id.length() < MIN_LENGTH || id.length() > MAX_LENGTH) {
			return false;
		}

		char first = id.charAt(0);
		if (first < 'a' || first > 'z') {
			return false;
		}

		for (int i = 1; i < id.length(); i++) {
			char c = id.charAt(i);
			boolean valid = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
			if (!valid) {
				return false;
			}
		}

		return true;
	}
}
