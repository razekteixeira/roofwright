package io.github.razekteixeira.roofwright.core;

import java.util.Map;

/**
 * Minecraft's own rule for stair corners ({@code StairBlock.getStairsShape} in 26.3), applied to planned
 * blocks. Roofwright plans every stair with the shape vanilla would give it, so placing the roof never
 * triggers a shape change and the preview shows exactly what will be built.
 *
 * <p>Only stairs at the same height and with the same half join. The block in front of a stair (on its
 * tall side) is checked first and makes an outer corner; then the block behind makes an inner corner.
 * All planned stairs are bottom-half, so the half check always passes between them.
 */
public final class StairShapes {
	private StairShapes() {
	}

	/** Looks up a stair facing at a position, or {@code null} when there is no stair there. */
	@FunctionalInterface
	public interface StairLookup {
		Dir facingAt(int x, int y, int z);
	}

	public static StairShape shapeOf(Dir facing, int x, int y, int z, StairLookup stairs) {
		Dir front = stairs.facingAt(x + facing.dx, y, z + facing.dz);
		if (front != null && !front.sameAxis(facing) && canTakeShape(facing, x, y, z, front.opposite(), stairs)) {
			return front == facing.counterClockwise() ? StairShape.OUTER_LEFT : StairShape.OUTER_RIGHT;
		}
		Dir back = stairs.facingAt(x - facing.dx, y, z - facing.dz);
		if (back != null && !back.sameAxis(facing) && canTakeShape(facing, x, y, z, back, stairs)) {
			return back == facing.counterClockwise() ? StairShape.INNER_LEFT : StairShape.INNER_RIGHT;
		}
		return StairShape.STRAIGHT;
	}

	/** A corner is refused when the side neighbour is a stair facing the same way (a straight run continues). */
	private static boolean canTakeShape(Dir facing, int x, int y, int z, Dir side, StairLookup stairs) {
		Dir neighbour = stairs.facingAt(x + side.dx, y, z + side.dz);
		return neighbour == null || neighbour != facing;
	}

	/** Planned stairs by position, as a lookup. */
	public static StairLookup lookup(Map<Long, PlannedBlock> byPosition) {
		return (x, y, z) -> {
			PlannedBlock block = byPosition.get(PlannedBlock.key(x, y, z));
			return block != null && block.piece().isStair() ? block.piece().facing() : null;
		};
	}
}
