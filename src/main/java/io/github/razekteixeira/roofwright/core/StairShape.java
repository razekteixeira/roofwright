package io.github.razekteixeira.roofwright.core;

import java.util.Locale;

/** Mirrors Minecraft's {@code StairsShape}. */
public enum StairShape {
	STRAIGHT,
	INNER_LEFT,
	INNER_RIGHT,
	OUTER_LEFT,
	OUTER_RIGHT;

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public boolean isOuter() {
		return this == OUTER_LEFT || this == OUTER_RIGHT;
	}

	public boolean isInner() {
		return this == INNER_LEFT || this == INNER_RIGHT;
	}
}
