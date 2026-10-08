package io.github.razekteixeira.roofwright.core;

import java.util.Locale;
import java.util.Optional;

/** Which way ridges run for gable-type roofs. {@link #AUTO} follows the long side of each wing. */
public enum RidgeAxis {
	AUTO,
	/** Ridge runs east-west. */
	X,
	/** Ridge runs north-south. */
	Z;

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static Optional<RidgeAxis> parse(String text) {
		String key = text.toLowerCase(Locale.ROOT);
		for (RidgeAxis axis : values()) {
			if (axis.id().equals(key)) {
				return Optional.of(axis);
			}
		}
		return Optional.empty();
	}
}
