package io.github.razekteixeira.roofwright.core;

import java.util.Locale;

/**
 * One planned block, independent of material. Stairs carry a facing (the side of the tall back, as in
 * Minecraft, so a roof stair faces uphill) and a shape; the other kinds ignore both.
 */
public record Piece(Kind kind, Dir facing, StairShape shape) {
	public enum Kind {
		FULL,
		STAIR,
		BOTTOM_SLAB,
		TOP_SLAB,
		/** A wall post (parapets); a full block when the material has no wall. */
		WALL
	}

	public static final Piece FULL = new Piece(Kind.FULL, Dir.NORTH, StairShape.STRAIGHT);
	public static final Piece BOTTOM_SLAB = new Piece(Kind.BOTTOM_SLAB, Dir.NORTH, StairShape.STRAIGHT);
	public static final Piece TOP_SLAB = new Piece(Kind.TOP_SLAB, Dir.NORTH, StairShape.STRAIGHT);
	public static final Piece WALL = new Piece(Kind.WALL, Dir.NORTH, StairShape.STRAIGHT);

	public static Piece stair(Dir facing) {
		return new Piece(Kind.STAIR, facing, StairShape.STRAIGHT);
	}

	public Piece withShape(StairShape value) {
		return new Piece(kind, facing, value);
	}

	public boolean isStair() {
		return kind == Kind.STAIR;
	}

	@Override
	public String toString() {
		return isStair() ? "stair[" + facing.id() + "," + shape.id() + "]" : kind.name().toLowerCase(Locale.ROOT);
	}
}
