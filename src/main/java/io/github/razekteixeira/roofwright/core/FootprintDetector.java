package io.github.razekteixeira.roofwright.core;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Finds a building's outline from one block on top of its walls.
 *
 * <ol>
 * <li>Walls: every solid block in the same layer connected to the clicked one, corners included
 * (8-connected), so walls that only touch diagonally still count as closed.</li>
 * <li>Outside: everything in that layer reachable from beyond the walls by straight steps
 * (4-connected), so a diagonal wall corner does not leak.</li>
 * <li>The outline is everything inside the walls' bounding box that the outside cannot reach: the walls
 * plus the rooms they enclose. Courtyards are enclosed too, so they get roofed over.</li>
 * </ol>
 */
public final class FootprintDetector {
	private FootprintDetector() {
	}

	/** Answers whether the block at (x, z) in the wall-top layer is solid. */
	@FunctionalInterface
	public interface Layer {
		boolean solid(int x, int z);
	}

	public static Footprint detect(int startX, int y, int startZ, Layer layer, int maxSpan) throws PlanException {
		if (!layer.solid(startX, startZ)) {
			throw new PlanException("Click the top block of a wall.");
		}
		Set<Long> walls = new HashSet<>();
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		walls.add(key(startX, startZ));
		queue.add(new int[] {startX, startZ});
		int minX = startX;
		int maxX = startX;
		int minZ = startZ;
		int maxZ = startZ;
		while (!queue.isEmpty()) {
			int[] cell = queue.poll();
			for (int dz = -1; dz <= 1; dz++) {
				for (int dx = -1; dx <= 1; dx++) {
					int x = cell[0] + dx;
					int z = cell[1] + dz;
					if ((dx == 0 && dz == 0) || walls.contains(key(x, z)) || !layer.solid(x, z)) {
						continue;
					}
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minZ = Math.min(minZ, z);
					maxZ = Math.max(maxZ, z);
					if (maxX - minX + 1 > maxSpan || maxZ - minZ + 1 > maxSpan) {
						throw new PlanException("These walls continue for more than " + maxSpan
								+ " blocks. Select the outline by hand, or ask an admin to raise maxSpan.");
					}
					walls.add(key(x, z));
					queue.add(new int[] {x, z});
				}
			}
		}

		// Flood the outside over a box one column larger than the walls on every side.
		int width = maxX - minX + 3;
		int depth = maxZ - minZ + 3;
		int originX = minX - 1;
		int originZ = minZ - 1;
		BitSet outside = new BitSet(width * depth);
		queue.add(new int[] {0, 0});
		outside.set(0);
		while (!queue.isEmpty()) {
			int[] cell = queue.poll();
			for (Dir dir : Dir.values()) {
				int lx = cell[0] + dir.dx;
				int lz = cell[1] + dir.dz;
				if (lx < 0 || lz < 0 || lx >= width || lz >= depth) {
					continue;
				}
				int index = lz * width + lx;
				if (outside.get(index) || walls.contains(key(originX + lx, originZ + lz))) {
					continue;
				}
				outside.set(index);
				queue.add(new int[] {lx, lz});
			}
		}

		Footprint.Builder builder = new Footprint.Builder(y);
		int enclosed = 0;
		for (int lz = 1; lz < depth - 1; lz++) {
			for (int lx = 1; lx < width - 1; lx++) {
				if (!outside.get(lz * width + lx)) {
					int x = originX + lx;
					int z = originZ + lz;
					builder.add(x, z);
					if (!walls.contains(key(x, z))) {
						enclosed++;
					}
				}
			}
		}
		Footprint footprint = builder.build();
		if (enclosed == 0 && !hasInnerColumn(footprint)) {
			throw new PlanException("These walls do not enclose anything at y " + y
					+ ". Close the gaps in the top layer of the walls, or select the outline by hand.");
		}
		return footprint;
	}

	/** True when some column has all four neighbours in the outline, i.e. it is more than a thin line. */
	private static boolean hasInnerColumn(Footprint f) {
		for (int z = f.minZ(); z <= f.maxZ(); z++) {
			for (int x = f.minX(); x <= f.maxX(); x++) {
				if (f.contains(x, z) && !f.isEdge(x, z)) {
					return true;
				}
			}
		}
		return false;
	}

	private static long key(int x, int z) {
		return ((long) x << 32) | (z & 0xffffffffL);
	}
}
