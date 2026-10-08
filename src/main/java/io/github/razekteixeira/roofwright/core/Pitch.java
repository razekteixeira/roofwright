package io.github.razekteixeira.roofwright.core;

import java.util.Locale;
import java.util.Optional;

/** How fast a roof rises: half-blocks of rise per block of run. */
public enum Pitch {
	/** 1:2, about 27 degrees, built from slabs. */
	LOW("low", "1:2", 1),
	/** 1:1, 45 degrees, one stair per step: the classic Minecraft roof. */
	NORMAL("normal", "1:1", 2),
	/** 2:1, about 63 degrees, a full block under every stair. */
	STEEP("steep", "2:1", 4);

	private final String id;
	private final String ratio;
	private final int halves;

	Pitch(String id, String ratio, int halves) {
		this.id = id;
		this.ratio = ratio;
		this.halves = halves;
	}

	public String id() {
		return id;
	}

	public String ratio() {
		return ratio;
	}

	/** Rise per column in half blocks. */
	public int halves() {
		return halves;
	}

	/** Accepts the name ({@code steep}) or the ratio ({@code 2:1}). */
	public static Optional<Pitch> parse(String text) {
		String key = text.toLowerCase(Locale.ROOT);
		for (Pitch pitch : values()) {
			if (pitch.id.equals(key) || pitch.ratio.equals(key)) {
				return Optional.of(pitch);
			}
		}
		return Optional.empty();
	}
}
