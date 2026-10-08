package io.github.razekteixeira.roofwright.core;

import java.util.HashMap;
import java.util.Map;

/** Test helpers: plans without checked exceptions, lookups by position and an ASCII view for failures. */
final class Roofs {
	static final int WALL_TOP = 64;

	private Roofs() {
	}

	static RoofPlan plan(Footprint footprint, RoofSpec spec) {
		try {
			return RoofPlanner.plan(footprint, spec);
		} catch (PlanException e) {
			throw new AssertionError(e.getMessage(), e);
		}
	}

	static RoofPlan plan(Footprint footprint, RoofStyle style) {
		return plan(footprint, RoofSpec.DEFAULTS.withStyle(style));
	}

	static Footprint rect(int width, int depth) {
		return Footprint.rectangle(0, 0, width - 1, depth - 1, WALL_TOP);
	}

	/** An L: a {@code w x d} block missing its north-east {@code cw x cd} corner. */
	static Footprint ell(int w, int d, int cw, int cd) {
		Footprint.Builder builder = new Footprint.Builder(WALL_TOP);
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				if (!(x >= w - cw && z < cd)) {
					builder.add(x, z);
				}
			}
		}
		return builder.build();
	}

	static Map<Long, PlannedBlock> byPosition(RoofPlan plan) {
		Map<Long, PlannedBlock> map = new HashMap<>();
		for (PlannedBlock block : plan.blocks()) {
			map.put(block.key(), block);
		}
		return map;
	}

	/** The highest block of each column. */
	static Map<Long, PlannedBlock> topOfColumns(RoofPlan plan) {
		Map<Long, PlannedBlock> tops = new HashMap<>();
		for (PlannedBlock block : plan.blocks()) {
			long column = ((long) block.x() << 32) | (block.z() & 0xffffffffL);
			PlannedBlock current = tops.get(column);
			if (current == null || block.y() > current.y()) {
				tops.put(column, block);
			}
		}
		return tops;
	}

	static PlannedBlock top(RoofPlan plan, int x, int z) {
		return topOfColumns(plan).get(((long) x << 32) | (z & 0xffffffffL));
	}

	/** Top view: one character per column (height digit for full/slab, arrow for stairs). */
	static String topView(RoofPlan plan) {
		Map<Long, PlannedBlock> tops = topOfColumns(plan);
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (PlannedBlock block : tops.values()) {
			minX = Math.min(minX, block.x());
			maxX = Math.max(maxX, block.x());
			minZ = Math.min(minZ, block.z());
			maxZ = Math.max(maxZ, block.z());
		}
		StringBuilder text = new StringBuilder();
		for (int z = minZ; z <= maxZ; z++) {
			for (int x = minX; x <= maxX; x++) {
				PlannedBlock block = tops.get(((long) x << 32) | (z & 0xffffffffL));
				text.append(block == null ? ' ' : symbol(block));
			}
			text.append('\n');
		}
		return text.toString();
	}

	private static char symbol(PlannedBlock block) {
		Piece piece = block.piece();
		return switch (piece.kind()) {
			case STAIR -> {
				char arrow = switch (piece.facing()) {
					case NORTH -> '^';
					case SOUTH -> 'v';
					case EAST -> '>';
					case WEST -> '<';
				};
				yield piece.shape().isOuter() ? 'o' : piece.shape().isInner() ? 'i' : arrow;
			}
			case BOTTOM_SLAB -> '_';
			case TOP_SLAB -> '-';
			case FULL -> '#';
			case WALL -> '|';
		};
	}
}
