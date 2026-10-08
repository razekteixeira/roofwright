package io.github.razekteixeira.roofwright.core;

/** What a planned block is for; each role can map to its own material. */
public enum Role {
	/** The sloped surface. */
	ROOF,
	/** The cap on a ridge or peak. */
	RIDGE,
	/** Wall infill under a gable, gablet or gambrel end, matched to the walls. */
	GABLE,
	/** The deck of a flat roof. */
	DECK,
	/** The low wall around a flat roof. */
	PARAPET
}
