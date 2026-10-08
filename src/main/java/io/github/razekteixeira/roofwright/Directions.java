package io.github.razekteixeira.roofwright;

import net.minecraft.core.Direction;

import io.github.razekteixeira.roofwright.core.Dir;

/** Converts between the core's {@link Dir} and Minecraft's {@link Direction}. */
public final class Directions {
	private Directions() {
	}

	public static Direction of(Dir dir) {
		return switch (dir) {
			case NORTH -> Direction.NORTH;
			case EAST -> Direction.EAST;
			case SOUTH -> Direction.SOUTH;
			case WEST -> Direction.WEST;
		};
	}

	/** The horizontal direction, or north for up and down. */
	public static Dir of(Direction direction) {
		return switch (direction) {
			case EAST -> Dir.EAST;
			case SOUTH -> Dir.SOUTH;
			case WEST -> Dir.WEST;
			default -> Dir.NORTH;
		};
	}
}
