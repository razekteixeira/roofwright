package io.github.razekteixeira.roofwright.core;

import java.util.Arrays;
import java.util.BitSet;

/**
 * The outline a roof is built over: a set of columns (x, z) in world coordinates, including the wall
 * columns themselves, plus the y of the top of the walls. Immutable.
 */
public final class Footprint {
	private final int minX;
	private final int minZ;
	private final int width;
	private final int depth;
	private final BitSet cells;
	private final int wallTopY;
	private final int cellCount;

	private Footprint(int minX, int minZ, int width, int depth, BitSet cells, int wallTopY) {
		this.minX = minX;
		this.minZ = minZ;
		this.width = width;
		this.depth = depth;
		this.cells = cells;
		this.wallTopY = wallTopY;
		this.cellCount = cells.cardinality();
	}

	/** Every column from (x0, z0) to (x1, z1) inclusive, in any corner order. */
	public static Footprint rectangle(int x0, int z0, int x1, int z1, int wallTopY) {
		Builder builder = new Builder(wallTopY);
		builder.addRectangle(x0, z0, x1, z1);
		return builder.build();
	}

	public int minX() {
		return minX;
	}

	public int minZ() {
		return minZ;
	}

	public int maxX() {
		return minX + width - 1;
	}

	public int maxZ() {
		return minZ + depth - 1;
	}

	public int width() {
		return width;
	}

	public int depth() {
		return depth;
	}

	public int wallTopY() {
		return wallTopY;
	}

	public int cellCount() {
		return cellCount;
	}

	public boolean contains(int x, int z) {
		int lx = x - minX;
		int lz = z - minZ;
		return lx >= 0 && lz >= 0 && lx < width && lz < depth && cells.get(lz * width + lx);
	}

	/** A footprint column with at least one horizontal neighbour outside: the wall line. */
	public boolean isEdge(int x, int z) {
		if (!contains(x, z)) {
			return false;
		}
		for (Dir dir : Dir.values()) {
			if (!contains(x + dir.dx, z + dir.dz)) {
				return true;
			}
		}
		return false;
	}

	/** The same columns with a different wall top. */
	public Footprint atY(int y) {
		return new Footprint(minX, minZ, width, depth, cells, y);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Footprint f && f.minX == minX && f.minZ == minZ && f.width == width && f.depth == depth
				&& f.wallTopY == wallTopY && f.cells.equals(cells);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new int[] {minX, minZ, width, depth, wallTopY, cells.hashCode()});
	}

	@Override
	public String toString() {
		return "Footprint[" + cellCount + " columns, x " + minX + ".." + maxX() + ", z " + minZ + ".." + maxZ() + ", wall top y " + wallTopY + "]";
	}

	/** Collects columns, then builds the smallest grid that holds them. */
	public static final class Builder {
		private final int wallTopY;
		private final java.util.HashSet<Long> columns = new java.util.HashSet<>();

		public Builder(int wallTopY) {
			this.wallTopY = wallTopY;
		}

		public Builder add(int x, int z) {
			columns.add(((long) x << 32) | (z & 0xffffffffL));
			return this;
		}

		public Builder addRectangle(int x0, int z0, int x1, int z1) {
			for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					add(x, z);
				}
			}
			return this;
		}

		public Builder addAll(Footprint other) {
			for (int x = other.minX; x <= other.maxX(); x++) {
				for (int z = other.minZ; z <= other.maxZ(); z++) {
					if (other.contains(x, z)) {
						add(x, z);
					}
				}
			}
			return this;
		}

		public int size() {
			return columns.size();
		}

		public Footprint build() {
			if (columns.isEmpty()) {
				throw new IllegalStateException("a footprint needs at least one column");
			}
			int minX = Integer.MAX_VALUE;
			int minZ = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxZ = Integer.MIN_VALUE;
			for (long column : columns) {
				int x = (int) (column >> 32);
				int z = (int) column;
				minX = Math.min(minX, x);
				minZ = Math.min(minZ, z);
				maxX = Math.max(maxX, x);
				maxZ = Math.max(maxZ, z);
			}
			int width = maxX - minX + 1;
			int depth = maxZ - minZ + 1;
			BitSet cells = new BitSet(width * depth);
			for (long column : columns) {
				int x = (int) (column >> 32);
				int z = (int) column;
				cells.set((z - minZ) * width + (x - minX));
			}
			return new Footprint(minX, minZ, width, depth, cells, wallTopY);
		}
	}
}
