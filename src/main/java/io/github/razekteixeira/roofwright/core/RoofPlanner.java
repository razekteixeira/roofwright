package io.github.razekteixeira.roofwright.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a footprint and a {@link RoofSpec} into blocks. Pure logic, no Minecraft classes.
 *
 * <p>The roof is described by a <em>step distance</em> {@code d} for every column of the roof area: 0 on
 * the outermost ring of the overhang, growing towards the ridge. A profile turns {@code d} into the height
 * of the roof surface in half blocks, and every column is then filled from the height of its lowest
 * neighbour up to its own height with full blocks, a stair or a slab on top.
 *
 * <ul>
 * <li>Hip-type roofs use the Chebyshev (chessboard) distance to the outside of the overhang area. For
 * outlines made of right angles this is exactly the straight skeleton of the outline, so hips, valleys
 * and ridges land where a carpenter would put them.</li>
 * <li>Gable-type roofs split the outline into its maximal rectangles ("wings"). Each wing gets its own
 * roof with the ridge along its long side, and the wings are merged by taking the highest roof at every
 * column, which produces the valleys where wings meet. Hip roofs computed this way give the same
 * distances as the Chebyshev transform of the whole outline.</li>
 * <li>Cone and dome roofs use the straight-line (Euclidean) distance, so round towers stay round.</li>
 * </ul>
 */
public final class RoofPlanner {
	/** Hard ceiling on the footprint size, whatever the server config says. */
	public static final int MAX_SPAN = 512;
	/** Cone and dome use an exact but quadratic distance; they are meant for towers. */
	public static final int MAX_ROUND_SPAN = 96;
	public static final int MAX_WINGS = 256;

	private static final int OUTSIDE = -1;

	private RoofPlanner() {
	}

	public static RoofPlan plan(Footprint footprint, RoofSpec spec) throws PlanException {
		if (footprint.width() > MAX_SPAN || footprint.depth() > MAX_SPAN) {
			throw new PlanException("The outline is " + footprint.width() + " x " + footprint.depth()
					+ " blocks; Roofwright handles up to " + MAX_SPAN + " x " + MAX_SPAN + ".");
		}
		List<String> notes = new ArrayList<>();
		List<PlannedBlock> blocks = spec.style() == RoofStyle.FLAT
				? flat(footprint)
				: sloped(footprint, spec, notes);
		blocks.sort(Comparator.comparingInt(PlannedBlock::y).thenComparingInt(PlannedBlock::z).thenComparingInt(PlannedBlock::x));
		return new RoofPlan(footprint, spec, blocks, notes);
	}

	// --- flat --------------------------------------------------------------------------------------

	/** A deck filling the inside at the height of the wall tops, and a one block parapet on the walls. */
	private static List<PlannedBlock> flat(Footprint f) {
		List<PlannedBlock> blocks = new ArrayList<>();
		int y = f.wallTopY();
		for (int z = f.minZ(); z <= f.maxZ(); z++) {
			for (int x = f.minX(); x <= f.maxX(); x++) {
				if (f.isEdge(x, z)) {
					blocks.add(new PlannedBlock(x, y + 1, z, Piece.WALL, Role.PARAPET));
				} else if (f.contains(x, z)) {
					blocks.add(new PlannedBlock(x, y, z, Piece.FULL, Role.DECK));
				}
			}
		}
		return blocks;
	}

	// --- sloped ------------------------------------------------------------------------------------

	/**
	 * The roof area as a local grid with one column of outside padding all around. Columns outside the
	 * area keep a <em>virtual</em> step distance: what the roof next to them would continue as. Beyond an
	 * eave that is -1 (lower), beyond a gable rake it equals the rake's own step (level), so a rake reads
	 * as a straight edge and not as a hip.
	 */
	record Grid(int originX, int originZ, int width, int depth, int[] distance, boolean[] area) {
		int index(int x, int z) {
			return (z - originZ) * width + (x - originX);
		}

		boolean inside(int x, int z) {
			int lx = x - originX;
			int lz = z - originZ;
			return lx >= 0 && lz >= 0 && lx < width && lz < depth && area[lz * width + lx];
		}

		/** Step distance, or the virtual step distance outside the area ({@link #OUTSIDE} when unknown). */
		int at(int x, int z) {
			int lx = x - originX;
			int lz = z - originZ;
			return lx >= 0 && lz >= 0 && lx < width && lz < depth ? distance[lz * width + lx] : OUTSIDE;
		}

		int maxDistance() {
			int max = 0;
			for (int i = 0; i < distance.length; i++) {
				if (area[i]) {
					max = Math.max(max, distance[i]);
				}
			}
			return max;
		}
	}

	/** The step distance field the planner builds a roof from; for tests and the preview. */
	static Grid field(Footprint f, RoofSpec spec) throws PlanException {
		return switch (spec.style()) {
			case HIP, MANSARD, PYRAMID -> chebyshevField(f, spec.overhang());
			case GABLE, GAMBREL, DUTCH_GABLE, SHED -> wingField(f, spec);
			case CONE, DOME -> {
				if (f.width() > MAX_ROUND_SPAN || f.depth() > MAX_ROUND_SPAN) {
					throw new PlanException("Cone and dome roofs are for towers up to " + MAX_ROUND_SPAN + " blocks across.");
				}
				yield euclideanField(f, spec.overhang());
			}
			case FLAT -> throw new IllegalStateException("flat roofs have no slope");
		};
	}

	private static List<PlannedBlock> sloped(Footprint f, RoofSpec spec, List<String> notes) throws PlanException {
		if (spec.style() == RoofStyle.PYRAMID && (f.width() != f.depth() || f.cellCount() != f.width() * f.depth())) {
			notes.add("A pyramid needs a square outline, so this one gets a hip roof.");
		}
		Grid grid = field(f, spec);
		int[] top = profile(spec, grid.maxDistance());
		return render(f, spec, grid, top);
	}

	/** Height of the roof surface, in half blocks above the lowest eave, for each step distance. */
	static int[] profile(RoofSpec spec, int maxD) {
		int o = spec.overhang();
		int[] top = new int[maxD + 1];
		int steepSteps = o + Math.clamp((maxD - o) / 3, 1, 3);
		int radius = maxD + 1;
		for (int d = 0; d <= maxD; d++) {
			int rise = switch (spec.style()) {
				case GAMBREL, MANSARD -> d < steepSteps ? Pitch.STEEP.halves() : Pitch.LOW.halves();
				case DOME -> {
					double t = 1.0 - (d + 1.0) / radius;
					int height = (int) Math.round(2.0 * radius * Math.sqrt(Math.max(0, 1.0 - t * t)));
					yield Math.max(1, height - (d == 0 ? 0 : top[d - 1]));
				}
				default -> spec.pitch().halves();
			};
			top[d] = (d == 0 ? 0 : top[d - 1]) + rise;
		}
		return top;
	}

	// --- distance fields ---------------------------------------------------------------------------

	private static Grid emptyGrid(Footprint f, int margin) {
		int width = f.width() + 2 * margin + 2;
		int depth = f.depth() + 2 * margin + 2;
		int[] distance = new int[width * depth];
		Arrays.fill(distance, OUTSIDE);
		return new Grid(f.minX() - margin - 1, f.minZ() - margin - 1, width, depth, distance, new boolean[width * depth]);
	}

	/** Roof height in half blocks for a step distance; 0 below the eaves, capped at the top. */
	private static int heightAt(int[] top, int d) {
		return d < 0 ? 0 : top[Math.min(d, top.length - 1)];
	}

	/** Chessboard distance from every cell to the nearest target cell, by two raster passes. */
	static int[] chessboard(boolean[] target, int width, int depth) {
		int far = width + depth + 1;
		int[] dist = new int[width * depth];
		for (int i = 0; i < dist.length; i++) {
			dist[i] = target[i] ? 0 : far;
		}
		for (int z = 0; z < depth; z++) {
			for (int x = 0; x < width; x++) {
				int i = z * width + x;
				if (dist[i] == 0) {
					continue;
				}
				int best = dist[i];
				if (x > 0) {
					best = Math.min(best, dist[i - 1] + 1);
				}
				if (z > 0) {
					best = Math.min(best, dist[i - width] + 1);
					if (x > 0) {
						best = Math.min(best, dist[i - width - 1] + 1);
					}
					if (x < width - 1) {
						best = Math.min(best, dist[i - width + 1] + 1);
					}
				}
				dist[i] = best;
			}
		}
		for (int z = depth - 1; z >= 0; z--) {
			for (int x = width - 1; x >= 0; x--) {
				int i = z * width + x;
				if (dist[i] == 0) {
					continue;
				}
				int best = dist[i];
				if (x < width - 1) {
					best = Math.min(best, dist[i + 1] + 1);
				}
				if (z < depth - 1) {
					best = Math.min(best, dist[i + width] + 1);
					if (x < width - 1) {
						best = Math.min(best, dist[i + width + 1] + 1);
					}
					if (x > 0) {
						best = Math.min(best, dist[i + width - 1] + 1);
					}
				}
				dist[i] = best;
			}
		}
		return dist;
	}

	/** Hip-type field: the overhang area is the outline grown by {@code o} in chessboard distance. */
	private static Grid chebyshevField(Footprint f, int o) {
		Grid grid = emptyGrid(f, o);
		int size = grid.width() * grid.depth();
		boolean[] inFootprint = new boolean[size];
		for (int z = f.minZ(); z <= f.maxZ(); z++) {
			for (int x = f.minX(); x <= f.maxX(); x++) {
				inFootprint[grid.index(x, z)] = f.contains(x, z);
			}
		}
		int[] toFootprint = chessboard(inFootprint, grid.width(), grid.depth());
		boolean[] outsideArea = new boolean[size];
		for (int i = 0; i < size; i++) {
			outsideArea[i] = toFootprint[i] > o;
		}
		int[] toOutside = chessboard(outsideArea, grid.width(), grid.depth());
		for (int i = 0; i < size; i++) {
			if (!outsideArea[i]) {
				grid.area()[i] = true;
				grid.distance()[i] = toOutside[i] - 1;
			}
		}
		return grid;
	}

	/** Gable-type field: one roof per maximal rectangle, merged by taking the highest. */
	private static Grid wingField(Footprint f, RoofSpec spec) throws PlanException {
		int o = spec.overhang();
		Grid grid = emptyGrid(f, o);
		int size = grid.width() * grid.depth();
		int[] inArea = new int[size];
		int[] beside = new int[size];
		Arrays.fill(inArea, OUTSIDE);
		Arrays.fill(beside, OUTSIDE);
		for (Wing wing : wings(f, spec)) {
			Rect area = wing.area();
			boolean ridgeAlongX = wing.ridgeAlongX();
			int across = ridgeAlongX ? area.depth() : area.width();
			int highest = (across - 1) / 2;
			int gablet = o + Math.max(1, (highest - o) / 2);
			// One column beyond the wing as well: the same formulas give the virtual distances there.
			for (int z = area.z0() - 1; z <= area.z1() + 1; z++) {
				for (int x = area.x0() - 1; x <= area.x1() + 1; x++) {
					int toEaves = ridgeAlongX ? Math.min(z - area.z0(), area.z1() - z) : Math.min(x - area.x0(), area.x1() - x);
					int toEnds = ridgeAlongX ? Math.min(x - area.x0(), area.x1() - x) : Math.min(z - area.z0(), area.z1() - z);
					int d = switch (spec.style()) {
						case DUTCH_GABLE -> toEnds < gablet ? Math.min(toEaves, toEnds) : toEaves;
						case SHED -> switch (spec.shedRise()) {
							case NORTH -> area.z1() - z;
							case SOUTH -> z - area.z0();
							case EAST -> x - area.x0();
							case WEST -> area.x1() - x;
						};
						default -> toEaves;
					};
					int i = grid.index(x, z);
					if (area.contains(x, z)) {
						inArea[i] = Math.max(inArea[i], d);
					} else {
						beside[i] = Math.max(beside[i], d);
					}
				}
			}
		}
		for (int i = 0; i < size; i++) {
			grid.area()[i] = inArea[i] != OUTSIDE;
			grid.distance()[i] = grid.area()[i] ? inArea[i] : beside[i];
		}
		return grid;
	}

	/** One rectangular roof of a gable-type building: its area (with overhang) and its ridge direction. */
	record Wing(Rect area, boolean ridgeAlongX) {
	}

	/**
	 * The wings of a gable-type roof.
	 *
	 * <p>Sheds, and gables with a forced ridge direction, use every maximal rectangle: they cover the
	 * outline and each runs the full length it can. Gable, dutch gable and gambrel roofs instead split the
	 * outline into non-overlapping rectangles, largest first. The largest is the main wing. Every other
	 * piece attached by one side is an <em>arm</em>: its ridge runs towards the wing it is attached to and
	 * continues into it up to that wing's ridge line, so the two roofs meet in valleys and the arm's ridge
	 * ends inside the main roof instead of cutting through the whole building. An arm that would rise above
	 * the wing it joins keeps its ridge along the joint instead.
	 */
	static List<Wing> wings(Footprint f, RoofSpec spec) throws PlanException {
		int o = spec.overhang();
		List<Wing> wings = new ArrayList<>();
		if (spec.style() == RoofStyle.SHED || spec.ridge() != RidgeAxis.AUTO) {
			for (Rect rect : Rect.maximal(f, MAX_WINGS)) {
				boolean alongX = spec.ridge() == RidgeAxis.AUTO ? rect.width() >= rect.depth() : spec.ridge() == RidgeAxis.X;
				wings.add(new Wing(rect.expand(o), alongX));
			}
			return wings;
		}
		List<Rect> pieces = Rect.partition(f, MAX_WINGS);
		for (int i = 0; i < pieces.size(); i++) {
			Rect piece = pieces.get(i);
			boolean longAlongX = piece.width() >= piece.depth();
			Dir side = null;
			Rect joined = null;
			if (i > 0) {
				Dir[] contactSide = new Dir[1];
				joined = attachment(piece, pieces.subList(0, i), contactSide);
				side = contactSide[0];
			}
			if (joined == null) {
				wings.add(new Wing(piece.expand(o), longAlongX));
				continue;
			}
			boolean armAlongX = side == Dir.EAST || side == Dir.WEST;
			int armAcross = (armAlongX ? piece.depth() : piece.width()) + 2 * o;
			boolean joinedAlongX = wings.get(pieces.indexOf(joined)).ridgeAlongX();
			int joinedAcross = (joinedAlongX ? joined.depth() : joined.width()) + 2 * o;
			if ((armAcross - 1) / 2 > (joinedAcross - 1) / 2) {
				// Wider than the wing it joins: an arm would poke through the other roof's ridge.
				wings.add(new Wing(piece.expand(o), !armAlongX));
				continue;
			}
			Rect area = piece.expand(o);
			area = switch (side) {
				case NORTH -> new Rect(area.x0(), (joined.z0() + joined.z1() + 1) / 2, area.x1(), area.z1());
				case SOUTH -> new Rect(area.x0(), area.z0(), area.x1(), (joined.z0() + joined.z1()) / 2);
				case WEST -> new Rect((joined.x0() + joined.x1() + 1) / 2, area.z0(), area.x1(), area.z1());
				case EAST -> new Rect(area.x0(), area.z0(), (joined.x0() + joined.x1()) / 2, area.z1());
			};
			wings.add(new Wing(area, armAlongX));
		}
		return wings;
	}

	/**
	 * The earlier piece this one is attached to, when it touches earlier pieces on exactly one side
	 * (stored in {@code sideOut}); {@code null} when it touches none or several sides.
	 */
	private static Rect attachment(Rect piece, List<Rect> earlier, Dir[] sideOut) {
		Rect best = null;
		Dir bestSide = null;
		int sides = 0;
		int bestContact = 0;
		for (Dir side : Dir.values()) {
			boolean touched = false;
			for (Rect other : earlier) {
				int contact = contact(piece, other, side);
				if (contact > 0) {
					touched = true;
					if (contact > bestContact) {
						bestContact = contact;
						best = other;
						bestSide = side;
					}
				}
			}
			if (touched) {
				sides++;
			}
		}
		if (sides != 1) {
			return null;
		}
		sideOut[0] = bestSide;
		return best;
	}

	/** How many columns along {@code side} of {@code piece} have {@code other} right beyond them. */
	private static int contact(Rect piece, Rect other, Dir side) {
		return switch (side) {
			case NORTH -> other.z1() == piece.z0() - 1 ? overlap(piece.x0(), piece.x1(), other.x0(), other.x1()) : 0;
			case SOUTH -> other.z0() == piece.z1() + 1 ? overlap(piece.x0(), piece.x1(), other.x0(), other.x1()) : 0;
			case WEST -> other.x1() == piece.x0() - 1 ? overlap(piece.z0(), piece.z1(), other.z0(), other.z1()) : 0;
			case EAST -> other.x0() == piece.x1() + 1 ? overlap(piece.z0(), piece.z1(), other.z0(), other.z1()) : 0;
		};
	}

	private static int overlap(int a0, int a1, int b0, int b1) {
		return Math.max(0, Math.min(a1, b1) - Math.max(a0, b0) + 1);
	}

	/** Round field: the overhang area grows by {@code o} in straight-line distance, heights follow it too. */
	private static Grid euclideanField(Footprint f, int o) {
		Grid grid = emptyGrid(f, o);
		List<int[]> walls = new ArrayList<>();
		for (int z = f.minZ(); z <= f.maxZ(); z++) {
			for (int x = f.minX(); x <= f.maxX(); x++) {
				if (f.isEdge(x, z)) {
					walls.add(new int[] {x, z});
				}
			}
		}
		boolean[] area = new boolean[grid.width() * grid.depth()];
		double reach = (o + 0.25) * (o + 0.25);
		for (int z = grid.originZ() + 1; z < grid.originZ() + grid.depth() - 1; z++) {
			for (int x = grid.originX() + 1; x < grid.originX() + grid.width() - 1; x++) {
				boolean in = f.contains(x, z);
				for (int k = 0; !in && k < walls.size(); k++) {
					int dx = x - walls.get(k)[0];
					int dz = z - walls.get(k)[1];
					in = dx * dx + dz * dz <= reach;
				}
				area[grid.index(x, z)] = in;
			}
		}
		// Inside the outline: o plus the straight-line distance to the nearest column outside it, so the
		// wall line is step o everywhere. In the overhang: o minus the distance to the outline.
		List<int[]> outside = new ArrayList<>();
		for (int z = grid.originZ(); z < grid.originZ() + grid.depth(); z++) {
			for (int x = grid.originX(); x < grid.originX() + grid.width(); x++) {
				if (!f.contains(x, z) && touches(f, x, z)) {
					outside.add(new int[] {x, z});
				}
			}
		}
		for (int z = grid.originZ(); z < grid.originZ() + grid.depth(); z++) {
			for (int x = grid.originX(); x < grid.originX() + grid.width(); x++) {
				if (!area[grid.index(x, z)]) {
					continue;
				}
				int d;
				if (f.contains(x, z)) {
					d = o + (int) Math.floor(Math.sqrt(nearest(outside, x, z))) - 1;
				} else {
					d = Math.max(0, o - (int) Math.round(Math.sqrt(nearest(walls, x, z))));
				}
				grid.area()[grid.index(x, z)] = true;
				grid.distance()[grid.index(x, z)] = d;
			}
		}
		return grid;
	}

	private static boolean touches(Footprint f, int x, int z) {
		for (int dz = -1; dz <= 1; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				if (f.contains(x + dx, z + dz)) {
					return true;
				}
			}
		}
		return false;
	}

	/** Squared straight-line distance to the nearest of the cells. */
	private static long nearest(List<int[]> cells, int x, int z) {
		long best = Long.MAX_VALUE;
		for (int[] cell : cells) {
			long dx = x - cell[0];
			long dz = z - cell[1];
			best = Math.min(best, dx * dx + dz * dz);
		}
		return best;
	}

	// --- rendering ---------------------------------------------------------------------------------

	private static List<PlannedBlock> render(Footprint f, RoofSpec spec, Grid grid, int[] top) {
		int o = spec.overhang();
		// The wall line (step o) starts in the layer just above the wall tops.
		int base = f.wallTopY() + 1 - Math.floorDiv(o == 0 ? 0 : top[Math.min(o, top.length) - 1], 2);
		Map<Long, PlannedBlock> placed = new HashMap<>();
		int size = grid.width() * grid.depth();
		int[] lowestBlock = new int[size];

		// Ridges and peaks first: a slab cap lowers the column by half a block, and its neighbours must
		// fill their sides down to the capped height, not the planned one.
		boolean[] ridge = new boolean[size];
		int[] surface = new int[size];
		for (int z = grid.originZ(); z < grid.originZ() + grid.depth(); z++) {
			for (int x = grid.originX(); x < grid.originX() + grid.width(); x++) {
				if (grid.inside(x, z)) {
					int i = grid.index(x, z);
					int height = top[grid.at(x, z)];
					ridge[i] = height % 2 == 0 && surfacePiece(grid, top, x, z, height) == null;
					surface[i] = ridge[i] && spec.slabRidge() ? height - 1 : height;
				}
			}
		}

		for (int z = grid.originZ(); z < grid.originZ() + grid.depth(); z++) {
			for (int x = grid.originX(); x < grid.originX() + grid.width(); x++) {
				if (!grid.inside(x, z)) {
					continue;
				}
				int i = grid.index(x, z);
				int d = grid.at(x, z);
				int planned = top[d];
				int height = surface[i];
				int rise = planned - (d == 0 ? 0 : top[d - 1]);
				int bottom = planned - rise;
				for (Dir dir : Dir.values()) {
					if (grid.inside(x + dir.dx, z + dir.dz)) {
						bottom = Math.min(bottom, surface[grid.index(x + dir.dx, z + dir.dz)]);
					}
				}
				bottom = Math.min(bottom, height - 1);
				int firstBlock = Math.floorDiv(bottom, 2);
				int topBlock = Math.floorDiv(height - 1, 2);
				for (int by = firstBlock; by <= topBlock; by++) {
					boolean lower = 2 * by >= bottom && 2 * by < height;
					boolean upper = 2 * by + 1 >= bottom && 2 * by + 1 < height;
					Piece piece;
					Role role = ridge[i] && by == topBlock ? Role.RIDGE : Role.ROOF;
					if (lower && upper && by == topBlock) {
						piece = ridge[i] ? Piece.FULL : surfacePiece(grid, top, x, z, height);
					} else if (lower && upper) {
						piece = Piece.FULL;
					} else if (lower) {
						piece = Piece.BOTTOM_SLAB;
					} else {
						piece = Piece.TOP_SLAB;
					}
					PlannedBlock block = new PlannedBlock(x, base + by, z, piece, role);
					placed.put(block.key(), block);
				}
				lowestBlock[grid.index(x, z)] = base + firstBlock;
			}
		}

		// Gable walls: close the gap between the wall tops and a roof that starts higher up.
		for (int z = f.minZ(); z <= f.maxZ(); z++) {
			for (int x = f.minX(); x <= f.maxX(); x++) {
				if (!f.isEdge(x, z) || !grid.inside(x, z)) {
					continue;
				}
				int lowest = lowestBlock[grid.index(x, z)];
				if (lowest <= f.wallTopY() + 1) {
					continue;
				}
				for (int y = f.wallTopY() + 1; y < lowest; y++) {
					PlannedBlock block = new PlannedBlock(x, y, z, Piece.FULL, Role.GABLE);
					placed.put(block.key(), block);
				}
				PlannedBlock first = placed.get(PlannedBlock.key(x, lowest, z));
				if (first != null && first.piece().kind() == Piece.Kind.TOP_SLAB) {
					placed.put(first.key(), first.withPiece(Piece.FULL));
				}
			}
		}

		StairShapes.StairLookup stairs = StairShapes.lookup(placed);
		List<PlannedBlock> blocks = new ArrayList<>(placed.size());
		for (PlannedBlock block : placed.values()) {
			if (block.piece().isStair()) {
				Piece piece = block.piece();
				block = block.withPiece(piece.withShape(StairShapes.shapeOf(piece.facing(), block.x(), block.y(), block.z(), stairs)));
			}
			blocks.add(block);
		}
		return blocks;
	}

	/**
	 * The piece for the top block of a column whose top block is full height: a stair facing uphill, a
	 * full block on flat ground, or {@code null} for a ridge or peak (lower on two opposite sides, or on
	 * three or four sides). Neighbours outside the roof area use their virtual height.
	 */
	private static Piece surfacePiece(Grid grid, int[] top, int x, int z, int height) {
		int threshold = height - 2;
		boolean[] lower = new boolean[4];
		int count = 0;
		for (Dir dir : Dir.values()) {
			int nx = x + dir.dx;
			int nz = z + dir.dz;
			int neighbour = heightAt(top, grid.at(nx, nz));
			if (neighbour <= threshold) {
				lower[dir.ordinal()] = true;
				count++;
			}
		}
		if (count == 0) {
			// An inner corner (valley) when exactly one diagonal neighbour is lower.
			Dir corner = null;
			int diagonals = 0;
			for (Dir dir : Dir.values()) {
				Dir side = dir.clockwise();
				int nx = x + dir.dx + side.dx;
				int nz = z + dir.dz + side.dz;
				int neighbour = heightAt(top, grid.at(nx, nz));
				if (neighbour <= threshold) {
					diagonals++;
					corner = dir;
				}
			}
			return diagonals == 1 ? Piece.stair(corner.opposite()) : Piece.FULL;
		}
		if (count == 1) {
			for (Dir dir : Dir.values()) {
				if (lower[dir.ordinal()]) {
					return Piece.stair(dir.opposite());
				}
			}
		}
		if (count == 2) {
			for (Dir dir : Dir.values()) {
				if (lower[dir.ordinal()] && lower[dir.clockwise().ordinal()]) {
					// Outer corner (hip): face uphill away from one of the two low sides; vanilla's rule
					// turns it into the matching outer corner.
					return Piece.stair(dir.opposite());
				}
			}
		}
		return null;
	}
}
