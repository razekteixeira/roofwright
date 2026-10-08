package io.github.razekteixeira.roofwright.core;

import java.util.Locale;

/**
 * The four horizontal directions with Minecraft's conventions: north is -z, east is +x. Rotations are
 * seen from above, so {@link #clockwise()} of north is east, exactly like {@code Direction.getClockWise}.
 */
public enum Dir {
	NORTH(0, -1),
	EAST(1, 0),
	SOUTH(0, 1),
	WEST(-1, 0);

	public final int dx;
	public final int dz;

	Dir(int dx, int dz) {
		this.dx = dx;
		this.dz = dz;
	}

	public Dir opposite() {
		return values()[(ordinal() + 2) % 4];
	}

	public Dir clockwise() {
		return values()[(ordinal() + 1) % 4];
	}

	public Dir counterClockwise() {
		return values()[(ordinal() + 3) % 4];
	}

	public boolean sameAxis(Dir other) {
		return (ordinal() % 2) == (other.ordinal() % 2);
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
