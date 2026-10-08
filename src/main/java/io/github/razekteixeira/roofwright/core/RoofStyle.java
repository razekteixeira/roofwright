package io.github.razekteixeira.roofwright.core;

import java.util.Locale;
import java.util.Optional;

/** The roof shapes Roofwright can build. */
public enum RoofStyle {
	/** Two slopes meeting at a ridge, with vertical gable walls at the ends. */
	GABLE,
	/** Slopes on every side, meeting at hips and a ridge (or a point). */
	HIP,
	/** A hip roof with a small gable (gablet) near the top of each end. */
	DUTCH_GABLE,
	/** A barn roof: a steep lower slope and a shallow upper slope on two sides. */
	GAMBREL,
	/** A gambrel on all four sides: steep lower slopes and a shallow top. */
	MANSARD,
	/** All slopes meet at one point. Exact on square footprints; other footprints get a hip. */
	PYRAMID,
	/** A single slope, rising towards one side. */
	SHED,
	/** A flat deck at the top of the walls with a parapet around it. */
	FLAT,
	/** A pointed tower roof for round or small footprints. */
	CONE,
	/** A rounded tower roof for round or small footprints. */
	DOME;

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static Optional<RoofStyle> parse(String text) {
		String key = text.toLowerCase(Locale.ROOT).replace('-', '_');
		for (RoofStyle style : values()) {
			if (style.id().equals(key)) {
				return Optional.of(style);
			}
		}
		return Optional.empty();
	}

	/** True for styles whose slope can be chosen; the others have a fixed profile. */
	public boolean usesPitch() {
		return this != GAMBREL && this != MANSARD && this != FLAT && this != DOME;
	}

	/** True for styles whose ridges follow wings of the footprint (and so honour a ridge axis). */
	public boolean usesRidgeAxis() {
		return this == GABLE || this == DUTCH_GABLE || this == GAMBREL;
	}
}
