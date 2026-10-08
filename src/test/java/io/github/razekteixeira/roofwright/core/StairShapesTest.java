package io.github.razekteixeira.roofwright.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Hand-worked cases of vanilla's stair corner rule (StairBlock.getStairsShape, read from the 26.3 jar).
 * The GameTests check the same rule against the real game.
 */
class StairShapesTest {
	private final Map<Long, PlannedBlock> world = new HashMap<>();

	private void stair(int x, int z, Dir facing) {
		PlannedBlock block = new PlannedBlock(x, 0, z, Piece.stair(facing), Role.ROOF);
		world.put(block.key(), block);
	}

	private StairShape shape(int x, int z, Dir facing) {
		return StairShapes.shapeOf(facing, x, 0, z, StairShapes.lookup(world));
	}

	@Test
	void aloneIsStraight() {
		assertEquals(StairShape.STRAIGHT, shape(0, 0, Dir.NORTH));
	}

	@Test
	void perpendicularStairInFrontMakesAnOuterCorner() {
		// Facing north means the tall side is north; the block in front is at z - 1.
		stair(0, -1, Dir.WEST);
		assertEquals(StairShape.OUTER_LEFT, shape(0, 0, Dir.NORTH), "west is north's counter-clockwise");
		world.clear();
		stair(0, -1, Dir.EAST);
		assertEquals(StairShape.OUTER_RIGHT, shape(0, 0, Dir.NORTH));
	}

	@Test
	void perpendicularStairBehindMakesAnInnerCorner() {
		stair(0, 1, Dir.WEST);
		assertEquals(StairShape.INNER_LEFT, shape(0, 0, Dir.NORTH));
		world.clear();
		stair(0, 1, Dir.EAST);
		assertEquals(StairShape.INNER_RIGHT, shape(0, 0, Dir.NORTH));
	}

	@Test
	void parallelNeighboursNeverMakeCorners() {
		stair(0, -1, Dir.SOUTH);
		stair(0, 1, Dir.NORTH);
		assertEquals(StairShape.STRAIGHT, shape(0, 0, Dir.NORTH));
	}

	@Test
	void outerCornerIsRefusedWhenTheSideContinuesTheRun() {
		// In front faces west, so the side checked is east (its opposite): a stair there facing north blocks it.
		stair(0, -1, Dir.WEST);
		stair(1, 0, Dir.NORTH);
		assertEquals(StairShape.STRAIGHT, shape(0, 0, Dir.NORTH));
		// A stair on the east side facing another way does not block it.
		stair(1, 0, Dir.SOUTH);
		assertEquals(StairShape.OUTER_LEFT, shape(0, 0, Dir.NORTH));
	}

	@Test
	void innerCornerIsRefusedWhenTheSideContinuesTheRun() {
		// Behind faces west, so the side checked is west itself.
		stair(0, 1, Dir.WEST);
		stair(-1, 0, Dir.NORTH);
		assertEquals(StairShape.STRAIGHT, shape(0, 0, Dir.NORTH));
	}

	@Test
	void outerWinsOverInner() {
		stair(0, -1, Dir.EAST);
		stair(0, 1, Dir.WEST);
		assertEquals(StairShape.OUTER_RIGHT, shape(0, 0, Dir.NORTH));
	}

	@Test
	void onlyTheSameHeightCounts() {
		PlannedBlock above = new PlannedBlock(0, 1, -1, Piece.stair(Dir.WEST), Role.ROOF);
		world.put(above.key(), above);
		assertEquals(StairShape.STRAIGHT, shape(0, 0, Dir.NORTH));
	}

	@Test
	void directionsFollowMinecraft() {
		assertEquals(Dir.EAST, Dir.NORTH.clockwise());
		assertEquals(Dir.WEST, Dir.NORTH.counterClockwise());
		assertEquals(Dir.EAST, Dir.SOUTH.counterClockwise());
		assertEquals(Dir.SOUTH, Dir.NORTH.opposite());
		assertEquals(-1, Dir.NORTH.dz);
		assertEquals(1, Dir.EAST.dx);
	}
}
