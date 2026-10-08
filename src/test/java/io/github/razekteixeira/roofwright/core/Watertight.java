package io.github.razekteixeira.roofwright.core;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks a roof has no holes, looking only at the planned blocks: wherever a column's surface is higher
 * than its neighbour's, the column itself must fill the side face between the two surfaces. The open
 * upper half of a stair on its low side is the step itself and is allowed.
 */
final class Watertight {
	private Watertight() {
	}

	private record Column(BitSet halves, PlannedBlock top) {
		int surface() {
			return halves.length();
		}
	}

	static void check(RoofPlan plan) {
		Map<Long, Column> columns = new HashMap<>();
		int base = plan.minY();
		for (PlannedBlock block : plan.blocks()) {
			long key = ((long) block.x() << 32) | (block.z() & 0xffffffffL);
			Column column = columns.computeIfAbsent(key, k -> new Column(new BitSet(), block));
			int lower = 2 * (block.y() - base);
			switch (block.piece().kind()) {
				case BOTTOM_SLAB -> column.halves().set(lower);
				case TOP_SLAB -> column.halves().set(lower + 1);
				default -> column.halves().set(lower, lower + 2);
			}
			if (block.y() > column.top().y()) {
				columns.put(key, new Column(column.halves(), block));
			}
		}
		for (Map.Entry<Long, Column> entry : columns.entrySet()) {
			int x = (int) (entry.getKey() >> 32);
			int z = (int) (long) entry.getKey();
			Column column = entry.getValue();
			for (Dir dir : Dir.values()) {
				Column neighbour = columns.get(((long) (x + dir.dx) << 32) | ((z + dir.dz) & 0xffffffffL));
				if (neighbour == null || neighbour.surface() >= column.surface()) {
					continue;
				}
				int to = column.surface() - (openTowards(column.top().piece(), dir) ? 1 : 0);
				for (int half = neighbour.surface(); half < to; half++) {
					if (!column.halves().get(half)) {
						fail("hole in the side of column " + x + "," + z + " towards " + dir + " at half " + half
								+ " (" + plan.spec().describe() + ")\n" + Roofs.topView(plan));
					}
				}
			}
		}
	}

	/** Whether the top piece leaves its upper half open on that side (the tread of a stair). */
	private static boolean openTowards(Piece piece, Dir side) {
		if (!piece.isStair()) {
			return false;
		}
		Dir front = piece.facing().opposite();
		return switch (piece.shape()) {
			case STRAIGHT -> side == front;
			case OUTER_LEFT -> side == front || side == piece.facing().clockwise();
			case OUTER_RIGHT -> side == front || side == piece.facing().counterClockwise();
			case INNER_LEFT, INNER_RIGHT -> false;
		};
	}
}
