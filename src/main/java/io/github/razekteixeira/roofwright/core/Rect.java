package io.github.razekteixeira.roofwright.core;

import java.util.ArrayList;
import java.util.List;

/** An axis-aligned rectangle of columns, bounds inclusive. */
public record Rect(int x0, int z0, int x1, int z1) {
	public int width() {
		return x1 - x0 + 1;
	}

	public int depth() {
		return z1 - z0 + 1;
	}

	public int area() {
		return width() * depth();
	}

	public Rect expand(int by) {
		return new Rect(x0 - by, z0 - by, x1 + by, z1 + by);
	}

	public boolean contains(int x, int z) {
		return x >= x0 && x <= x1 && z >= z0 && z <= z1;
	}

	/**
	 * Every maximal rectangle inside the footprint: rectangles of footprint columns that cannot grow in any
	 * direction. Together they cover the footprint, and any rectangle inside it (in particular any square)
	 * lies inside one of them. Runs in O(depth^2 * width).
	 *
	 * @throws PlanException when there are more than {@code limit} of them
	 */
	public static List<Rect> maximal(Footprint f, int limit) throws PlanException {
		int w = f.width();
		int d = f.depth();
		boolean[][] in = new boolean[d][w];
		for (int z = 0; z < d; z++) {
			for (int x = 0; x < w; x++) {
				in[z][x] = f.contains(f.minX() + x, f.minZ() + z);
			}
		}
		List<Rect> result = new ArrayList<>();
		boolean[] columns = new boolean[w];
		for (int top = 0; top < d; top++) {
			java.util.Arrays.fill(columns, true);
			for (int bottom = top; bottom < d; bottom++) {
				boolean any = false;
				for (int x = 0; x < w; x++) {
					columns[x] &= in[bottom][x];
					any |= columns[x];
				}
				if (!any) {
					break;
				}
				int x = 0;
				while (x < w) {
					if (!columns[x]) {
						x++;
						continue;
					}
					int start = x;
					while (x < w && columns[x]) {
						x++;
					}
					int end = x - 1;
					if (!rowCovers(in, top - 1, start, end) && !rowCovers(in, bottom + 1, start, end)) {
						if (result.size() >= limit) {
							throw new PlanException("This outline is too irregular for this roof style (more than " + limit
									+ " wings). Try a hip roof, or simplify the outline.");
						}
						result.add(new Rect(f.minX() + start, f.minZ() + top, f.minX() + end, f.minZ() + bottom));
					}
				}
			}
		}
		return result;
	}

	/**
	 * Splits the footprint into non-overlapping rectangles, largest first: each round takes the biggest
	 * maximal rectangle of what is left. The first one is the main wing of the building.
	 *
	 * @throws PlanException when it takes more than {@code limit} pieces
	 */
	public static List<Rect> partition(Footprint f, int limit) throws PlanException {
		List<Rect> pieces = new ArrayList<>();
		Footprint remaining = f;
		while (remaining != null) {
			Rect best = null;
			for (Rect rect : maximal(remaining, limit)) {
				if (best == null || rect.area() > best.area()
						|| (rect.area() == best.area() && Math.min(rect.width(), rect.depth()) > Math.min(best.width(), best.depth()))) {
					best = rect;
				}
			}
			if (pieces.size() >= limit) {
				throw new PlanException("This outline is too irregular for this roof style (more than " + limit
						+ " wings). Try a hip roof, or simplify the outline.");
			}
			pieces.add(best);
			Footprint.Builder rest = new Footprint.Builder(f.wallTopY());
			for (int z = remaining.minZ(); z <= remaining.maxZ(); z++) {
				for (int x = remaining.minX(); x <= remaining.maxX(); x++) {
					if (remaining.contains(x, z) && !best.contains(x, z)) {
						rest.add(x, z);
					}
				}
			}
			remaining = rest.size() == 0 ? null : rest.build();
		}
		return pieces;
	}

	private static boolean rowCovers(boolean[][] in, int row, int start, int end) {
		if (row < 0 || row >= in.length) {
			return false;
		}
		for (int x = start; x <= end; x++) {
			if (!in[row][x]) {
				return false;
			}
		}
		return true;
	}
}
