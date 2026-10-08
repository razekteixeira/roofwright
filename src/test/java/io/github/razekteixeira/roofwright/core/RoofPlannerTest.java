package io.github.razekteixeira.roofwright.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RoofPlannerTest {
	private static final int TOP = Roofs.WALL_TOP;

	/** Outlines every style is checked on: rectangles (odd, even), square, L, T, U and a plus. */
	static List<Footprint> outlines() {
		List<Footprint> list = new ArrayList<>();
		list.add(Roofs.rect(7, 5));
		list.add(Roofs.rect(8, 6));
		list.add(Roofs.rect(9, 9));
		list.add(Roofs.ell(11, 9, 5, 4));
		Footprint.Builder tee = new Footprint.Builder(TOP).addRectangle(0, 0, 12, 4).addRectangle(4, 5, 8, 11);
		list.add(tee.build());
		Footprint.Builder you = new Footprint.Builder(TOP).addRectangle(0, 0, 12, 4).addRectangle(0, 5, 3, 10).addRectangle(9, 5, 12, 10);
		list.add(you.build());
		Footprint.Builder plus = new Footprint.Builder(TOP).addRectangle(0, 5, 14, 9).addRectangle(5, 0, 9, 14);
		list.add(plus.build());
		return list;
	}

	// --- hip: Chebyshev distance and corners ---------------------------------------------------------

	@Test
	void hipOnRectangleRisesOneBlockPerRingWithOuterCornersOnTheHips() {
		RoofPlan plan = Roofs.plan(Roofs.rect(7, 5), RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP).withOverhang(0));
		// Ring 0 sits right above the wall tops, ring 1 one higher, the ridge (3 columns) on ring 2.
		assertEquals(TOP + 1, Roofs.top(plan, 3, 0).y());
		assertEquals(TOP + 2, Roofs.top(plan, 3, 1).y());
		assertEquals(TOP + 3, Roofs.top(plan, 3, 2).y());
		for (int x = 2; x <= 4; x++) {
			PlannedBlock ridge = Roofs.top(plan, x, 2);
			assertEquals(Role.RIDGE, ridge.role(), Roofs.topView(plan));
			assertEquals(Piece.Kind.BOTTOM_SLAB, ridge.piece().kind());
		}
		// The four corners of each ring are outer corner stairs; every other ring column is a straight stair.
		for (int[] corner : new int[][] {{0, 0}, {6, 0}, {0, 4}, {6, 4}, {1, 1}, {5, 1}, {1, 3}, {5, 3}}) {
			Piece piece = Roofs.top(plan, corner[0], corner[1]).piece();
			assertTrue(piece.isStair() && piece.shape().isOuter(), "corner " + corner[0] + "," + corner[1] + " is " + piece + "\n" + Roofs.topView(plan));
		}
		Piece side = Roofs.top(plan, 3, 0).piece();
		assertEquals(Piece.stair(Dir.SOUTH), side, "north side faces uphill (south)");
		assertEquals(Piece.stair(Dir.WEST), Roofs.top(plan, 6, 2).piece(), "east side faces uphill (west)");
	}

	@Test
	void hipFieldIsTheChebyshevDistanceToTheOutside() throws PlanException {
		Random random = new Random(26_3);
		for (int round = 0; round < 40; round++) {
			Footprint.Builder builder = new Footprint.Builder(TOP);
			for (int r = 0; r < 4; r++) {
				int x = random.nextInt(14);
				int z = random.nextInt(14);
				builder.addRectangle(x, z, x + 2 + random.nextInt(9), z + 2 + random.nextInt(9));
			}
			Footprint f = builder.build();
			int o = round % 3;
			RoofPlanner.Grid grid = RoofPlanner.field(f, RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP).withOverhang(o));
			for (int z = f.minZ() - o; z <= f.maxZ() + o; z++) {
				for (int x = f.minX() - o; x <= f.maxX() + o; x++) {
					int toFootprint = bruteChebyshev(f, x, z, true);
					boolean inArea = toFootprint <= o;
					assertEquals(inArea, grid.inside(x, z), "area at " + x + "," + z);
					if (inArea) {
						// Distance to the nearest column whose distance to the footprint exceeds o.
						int expected = Integer.MAX_VALUE;
						for (int zz = f.minZ() - o - 1; zz <= f.maxZ() + o + 1; zz++) {
							for (int xx = f.minX() - o - 1; xx <= f.maxX() + o + 1; xx++) {
								if (bruteChebyshev(f, xx, zz, true) > o) {
									expected = Math.min(expected, Math.max(Math.abs(xx - x), Math.abs(zz - z)));
								}
							}
						}
						assertEquals(expected - 1, grid.at(x, z), "step at " + x + "," + z + " in round " + round);
					}
				}
			}
		}
	}

	@Test
	void hipEqualsTheHighestOfTheWingHips() throws PlanException {
		for (Footprint f : outlines()) {
			RoofPlanner.Grid grid = RoofPlanner.field(f, RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP).withOverhang(1));
			List<Rect> wings = Rect.maximal(f, 100);
			for (int z = f.minZ(); z <= f.maxZ(); z++) {
				for (int x = f.minX(); x <= f.maxX(); x++) {
					if (!f.contains(x, z)) {
						continue;
					}
					int best = -1;
					for (Rect wing : wings) {
						Rect area = wing.expand(1);
						if (area.contains(x, z)) {
							best = Math.max(best, Math.min(Math.min(x - area.x0(), area.x1() - x), Math.min(z - area.z0(), area.z1() - z)));
						}
					}
					assertEquals(best, grid.at(x, z), "hip vs wings at " + x + "," + z + " of " + f);
				}
			}
		}
	}

	private static int bruteChebyshev(Footprint f, int x, int z, boolean toInside) {
		int best = Integer.MAX_VALUE;
		for (int zz = f.minZ(); zz <= f.maxZ(); zz++) {
			for (int xx = f.minX(); xx <= f.maxX(); xx++) {
				if (f.contains(xx, zz) == toInside) {
					best = Math.min(best, Math.max(Math.abs(xx - x), Math.abs(zz - z)));
				}
			}
		}
		return best;
	}

	@Test
	void ellShapedHipHasAValleyOfInnerCorners() {
		RoofPlan plan = Roofs.plan(Roofs.ell(11, 9, 5, 4), RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP).withOverhang(0));
		// The reflex corner of the outline is at (6, 3)/(5, 4); the valley runs diagonally inwards from it.
		int inner = 0;
		for (PlannedBlock block : plan.blocks()) {
			if (block.piece().isStair() && block.piece().shape().isInner()) {
				inner++;
				assertTrue(block.x() - 5 == 4 - block.z() || block.x() - 6 == 3 - block.z() || block.x() - 6 == 4 - block.z(),
						"inner corner off the valley at " + block + "\n" + Roofs.topView(plan));
			}
		}
		assertTrue(inner >= 2, "expected a valley of inner corners\n" + Roofs.topView(plan));
	}

	// --- gable: wings, rakes and gable walls -------------------------------------------------------

	@Test
	void gableHasStraightRakesAndATriangularGableWall() {
		RoofPlan plan = Roofs.plan(Roofs.rect(7, 5), RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE).withOverhang(1));
		Map<Long, PlannedBlock> at = Roofs.byPosition(plan);
		// Rake columns (x = -1 and x = 7) are straight stairs facing the ridge, never corners.
		for (int z = -1; z <= 5; z++) {
			for (int x : new int[] {-1, 7}) {
				Piece piece = Roofs.top(plan, x, z).piece();
				if (piece.isStair()) {
					assertEquals(StairShape.STRAIGHT, piece.shape(), "rake at " + x + "," + z + "\n" + Roofs.topView(plan));
				}
			}
		}
		// Gable wall on the west wall (x = 0): steps 1, 2, 1 above the wall line need 0, 1, 2, 1, 0 blocks.
		int[] expected = {0, 1, 2, 1, 0};
		for (int z = 0; z < 5; z++) {
			int count = 0;
			for (int y = TOP + 1; y < TOP + 5; y++) {
				PlannedBlock block = at.get(PlannedBlock.key(0, y, z));
				if (block != null && block.role() == Role.GABLE) {
					count++;
				}
			}
			assertEquals(expected[z], count, "gable wall height at z=" + z);
		}
		// Eave sides have no gable wall at all.
		for (PlannedBlock block : plan.blocks()) {
			if (block.role() == Role.GABLE) {
				assertTrue(block.x() == 0 || block.x() == 6, "gable wall only on the end walls: " + block);
			}
		}
	}

	@Test
	void gableRidgeFollowsTheLongSideUnlessAnAxisIsForced() {
		RoofPlan auto = Roofs.plan(Roofs.rect(9, 5), RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE).withOverhang(0));
		assertEquals(Role.RIDGE, Roofs.top(auto, 4, 2).role(), "ridge along x through the middle row\n" + Roofs.topView(auto));
		assertEquals(Piece.stair(Dir.SOUTH), Roofs.top(auto, 4, 0).piece());

		RoofPlan forced = Roofs.plan(Roofs.rect(9, 5), RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE).withOverhang(0).withRidge(RidgeAxis.Z));
		assertEquals(Role.RIDGE, Roofs.top(forced, 4, 2).role());
		assertEquals(Piece.stair(Dir.EAST), Roofs.top(forced, 0, 2).piece(), "west slope faces east\n" + Roofs.topView(forced));
	}

	@Test
	void evenWidthGableMeetsBackToBackWithoutARidgeCap() {
		RoofPlan plan = Roofs.plan(Roofs.rect(8, 6), RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE).withOverhang(0));
		assertEquals(Piece.stair(Dir.SOUTH), Roofs.top(plan, 3, 2).piece());
		assertEquals(Piece.stair(Dir.NORTH), Roofs.top(plan, 3, 3).piece());
		assertEquals(Roofs.top(plan, 3, 2).y(), Roofs.top(plan, 3, 3).y());
		assertTrue(plan.blocks().stream().noneMatch(b -> b.role() == Role.RIDGE), Roofs.topView(plan));
	}

	@Test
	void aBumpGetsASmallCrossGableThatStopsAtTheMainRidge() {
		Footprint plain = Roofs.rect(20, 10);
		Footprint bumped = new Footprint.Builder(TOP).addAll(plain).addRectangle(8, -1, 10, -1).build();
		RoofPlan without = Roofs.plan(plain, RoofStyle.GABLE);
		RoofPlan with = Roofs.plan(bumped, RoofStyle.GABLE);
		// The far (south) half of the roof is untouched by the bump on the north side.
		for (int x = -1; x <= 20; x++) {
			for (int z = 5; z <= 10; z++) {
				assertEquals(Roofs.top(without, x, z), Roofs.top(with, x, z), "south half changed at " + x + "," + z + "\n" + Roofs.topView(with));
			}
		}
		// The bump has its own little gable: a ridge cap above the bump.
		assertEquals(Role.RIDGE, Roofs.top(with, 9, -1).role(), Roofs.topView(with));
	}

	@Test
	void ellArmEndsInAValleyInsteadOfASecondGableAtTheCorner() {
		// Main wing 11 x 5 along the south (ridge east-west), arm 6 x 4 to the north-west.
		RoofPlan plan = Roofs.plan(Roofs.ell(11, 9, 5, 4), RoofStyle.GABLE);
		for (PlannedBlock block : plan.blocks()) {
			if (block.role() == Role.GABLE) {
				assertTrue(block.z() < 4 ? block.z() == 0 : block.x() == 0 || block.x() == 10,
						"gable walls only at the arm's free end and the main wing's ends: " + block + "\n" + Roofs.topView(plan));
			}
		}
		// The south wall is all eave: same as a plain 11 x 5 roof there.
		RoofPlan main = Roofs.plan(Footprint.rectangle(0, 4, 10, 8, TOP), RoofStyle.GABLE);
		for (int x = -1; x <= 11; x++) {
			assertEquals(Roofs.top(main, x, 9), Roofs.top(plan, x, 9), "south eave at x=" + x);
			assertEquals(Roofs.top(main, x, 8), Roofs.top(plan, x, 8), "south wall line at x=" + x);
		}
		assertTrue(plan.blocks().stream().anyMatch(b -> b.piece().isStair() && b.piece().shape().isInner()), "valley\n" + Roofs.topView(plan));
	}

	@Test
	void armsNeverRiseAboveTheWingTheyJoin() {
		for (Footprint f : outlines()) {
			RoofPlan gable = Roofs.plan(f, RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE).withOverhang(1));
			RoofPlan hip = Roofs.plan(f, RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP).withOverhang(1));
			// A gable is never taller than the hip roof's peak plus the ridge cap row.
			assertTrue(gable.maxY() <= hip.maxY(), "gable peaks above the hip on " + f + "\n" + Roofs.topView(gable));
		}
		Footprint step = new Footprint.Builder(TOP).addRectangle(0, 0, 9, 9).addRectangle(10, 1, 19, 8).build();
		RoofPlan plan = Roofs.plan(step, RoofStyle.GABLE);
		RoofPlan main = Roofs.plan(Footprint.rectangle(0, 1, 19, 8, TOP), RoofStyle.GABLE);
		assertEquals(main.maxY(), plan.maxY(), "a wider block beside a narrower wing stays under its ridge\n" + Roofs.topView(plan));
	}

	@Test
	void fullBlockRidgeWhenSlabRidgeIsOff() {
		RoofPlan plan = Roofs.plan(Roofs.rect(7, 5), RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE).withOverhang(0).withSlabRidge(false));
		assertEquals(Piece.FULL, Roofs.top(plan, 3, 2).piece());
		assertEquals(Role.RIDGE, Roofs.top(plan, 3, 2).role());
	}

	// --- pitch -------------------------------------------------------------------------------------

	@Test
	void lowPitchIsAllSlabsAndRisesHalfABlockPerColumn() {
		RoofPlan plan = Roofs.plan(Roofs.rect(9, 9), RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP).withPitch(Pitch.LOW).withOverhang(0));
		assertTrue(plan.blocks().stream().noneMatch(b -> b.piece().isStair()), Roofs.topView(plan));
		// Centre column: step 4, 5 halves above the eave, so a bottom slab two blocks up.
		PlannedBlock centre = Roofs.top(plan, 4, 4);
		assertEquals(TOP + 3, centre.y());
		assertEquals(Piece.Kind.BOTTOM_SLAB, centre.piece().kind());
		assertEquals(Piece.Kind.TOP_SLAB, Roofs.top(plan, 4, 1).piece().kind());
	}

	@Test
	void steepPitchPutsAFullBlockUnderEveryStair() {
		RoofPlan plan = Roofs.plan(Roofs.rect(9, 7), RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE).withPitch(Pitch.STEEP).withOverhang(1));
		Map<Long, PlannedBlock> at = Roofs.byPosition(plan);
		int stairs = 0;
		for (PlannedBlock block : plan.blocks()) {
			if (block.piece().isStair()) {
				stairs++;
				PlannedBlock below = at.get(PlannedBlock.key(block.x(), block.y() - 1, block.z()));
				assertNotNull(below, "nothing under " + block);
				assertEquals(Piece.Kind.FULL, below.piece().kind(), "under " + block);
			}
		}
		assertTrue(stairs > 0);
		// Two blocks of rise per column: wall line at y+1..y+2, next column at y+3..y+4.
		assertEquals(TOP + 2, Roofs.top(plan, 4, 0).y());
		assertEquals(TOP + 4, Roofs.top(plan, 4, 1).y());
	}

	@Test
	void overhangHangsBesideTheWallTop() {
		for (Pitch pitch : Pitch.values()) {
			for (int o = 0; o <= RoofSpec.MAX_OVERHANG; o++) {
				RoofPlan plan = Roofs.plan(Roofs.rect(9, 9), RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP).withPitch(pitch).withOverhang(o));
				int lowestOnWall = Integer.MAX_VALUE;
				for (PlannedBlock block : plan.blocks()) {
					if (block.x() == 4 && block.z() == 0) {
						lowestOnWall = Math.min(lowestOnWall, block.y());
					}
				}
				assertEquals(TOP + 1, lowestOnWall, "wall line starts above the wall top, pitch " + pitch + " overhang " + o);
				int reach = -o;
				assertTrue(plan.blocks().stream().anyMatch(b -> b.z() == reach), "overhang reaches " + o + " columns out");
				assertTrue(plan.blocks().stream().noneMatch(b -> b.z() < reach), "and no further");
			}
		}
	}

	// --- other styles ------------------------------------------------------------------------------

	@Test
	void dutchGableHasHippedEndsAndAGabletFace() {
		Footprint house = Roofs.rect(13, 9);
		RoofPlan dutch = Roofs.plan(house, RoofSpec.DEFAULTS.withStyle(RoofStyle.DUTCH_GABLE).withOverhang(0));
		RoofPlan gable = Roofs.plan(house, RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE).withOverhang(0));
		// The end wall itself is sloped (hipped), so it gets no gable wall, unlike the gable roof.
		assertTrue(dutch.blocks().stream().noneMatch(b -> b.role() == Role.GABLE), Roofs.topView(dutch));
		assertTrue(gable.blocks().stream().anyMatch(b -> b.role() == Role.GABLE));
		// At the top, the gablet is a vertical face: a column two or more blocks taller than its end-side neighbour.
		Map<Long, PlannedBlock> tops = Roofs.topOfColumns(dutch);
		boolean face = false;
		for (PlannedBlock block : tops.values()) {
			PlannedBlock west = Roofs.top(dutch, block.x() - 1, block.z());
			if (west != null && block.y() - west.y() >= 2) {
				face = true;
			}
		}
		assertTrue(face, "gablet face expected\n" + Roofs.topView(dutch));
	}

	@Test
	void shedRisesTowardsTheChosenSide() {
		for (Dir rise : Dir.values()) {
			RoofPlan plan = Roofs.plan(Roofs.rect(7, 7), RoofSpec.DEFAULTS.withStyle(RoofStyle.SHED).withOverhang(0).withShedRise(rise));
			PlannedBlock low = Roofs.top(plan, 3 - 3 * rise.dx, 3 - 3 * rise.dz);
			PlannedBlock high = Roofs.top(plan, 3 + 3 * rise.dx, 3 + 3 * rise.dz);
			assertEquals(6, high.y() - low.y(), "shed rising " + rise);
			assertEquals(Piece.stair(rise), Roofs.top(plan, 3, 3).piece());
		}
	}

	@Test
	void gambrelAndMansardAreSteepBelowAndShallowAbove() {
		for (RoofStyle style : new RoofStyle[] {RoofStyle.GAMBREL, RoofStyle.MANSARD}) {
			RoofPlan plan = Roofs.plan(Roofs.rect(17, 17), RoofSpec.DEFAULTS.withStyle(style).withOverhang(1));
			// Lower slope: full block under a stair. Upper slope: slabs only.
			assertEquals(Piece.Kind.FULL, Roofs.byPosition(plan).get(PlannedBlock.key(8, Roofs.top(plan, 8, 0).y() - 1, 0)).piece().kind(), style.id());
			assertTrue(Roofs.top(plan, 8, 8).piece().kind() == Piece.Kind.BOTTOM_SLAB || Roofs.top(plan, 8, 8).piece().kind() == Piece.Kind.TOP_SLAB,
					style + "\n" + Roofs.topView(plan));
		}
		RoofPlan mansard = Roofs.plan(Roofs.rect(17, 17), RoofSpec.DEFAULTS.withStyle(RoofStyle.MANSARD).withOverhang(1));
		assertTrue(mansard.blocks().stream().noneMatch(b -> b.role() == Role.GABLE), "a mansard is hipped on all four sides");
	}

	@Test
	void coneIsRoundAndPeaksInTheMiddle() {
		Footprint.Builder disk = new Footprint.Builder(TOP);
		for (int x = -6; x <= 6; x++) {
			for (int z = -6; z <= 6; z++) {
				if (x * x + z * z <= 40) {
					disk.add(x, z);
				}
			}
		}
		Footprint tower = disk.build();
		for (RoofStyle style : new RoofStyle[] {RoofStyle.CONE, RoofStyle.DOME}) {
			RoofPlan plan = Roofs.plan(tower, RoofSpec.DEFAULTS.withStyle(style).withPitch(Pitch.STEEP));
			Map<Long, PlannedBlock> tops = Roofs.topOfColumns(plan);
			int peak = plan.maxY();
			assertEquals(peak, Roofs.top(plan, 0, 0).y(), style + " peaks in the middle");
			// Quarter turns of the tower give the same roof heights.
			for (PlannedBlock block : tops.values()) {
				PlannedBlock turned = Roofs.top(plan, -block.z(), block.x());
				assertNotNull(turned, "rotated column missing for " + block);
				assertEquals(block.y(), turned.y(), style + " symmetric at " + block.x() + "," + block.z());
			}
		}
		RoofPlan cone = Roofs.plan(tower, RoofSpec.DEFAULTS.withStyle(RoofStyle.CONE).withPitch(Pitch.STEEP));
		RoofPlan dome = Roofs.plan(tower, RoofSpec.DEFAULTS.withStyle(RoofStyle.DOME));
		assertTrue(cone.maxY() > dome.maxY(), "a steep cone is taller than a dome");
	}

	@Test
	void pyramidOnARectangleExplainsItBuildsAHip() {
		assertTrue(Roofs.plan(Roofs.rect(9, 9), RoofStyle.PYRAMID).notes().isEmpty());
		assertFalse(Roofs.plan(Roofs.rect(9, 7), RoofStyle.PYRAMID).notes().isEmpty());
	}

	@Test
	void flatRoofHasADeckAtWallTopAndAParapetOnTheWalls() {
		RoofPlan plan = Roofs.plan(Roofs.ell(11, 9, 5, 4), RoofStyle.FLAT);
		Footprint f = plan.footprint();
		for (PlannedBlock block : plan.blocks()) {
			if (block.role() == Role.PARAPET) {
				assertTrue(f.isEdge(block.x(), block.z()));
				assertEquals(TOP + 1, block.y());
			} else {
				assertEquals(Role.DECK, block.role());
				assertFalse(f.isEdge(block.x(), block.z()));
				assertEquals(TOP, block.y());
			}
		}
		assertEquals(f.cellCount(), plan.size(), "one block per column");
	}

	// --- properties over every style and outline --------------------------------------------------

	@ParameterizedTest
	@EnumSource(RoofStyle.class)
	void everyStairHasTheShapeVanillaWouldGiveIt(RoofStyle style) {
		for (Footprint f : outlines()) {
			RoofPlan plan = Roofs.plan(f, RoofSpec.DEFAULTS.withStyle(style));
			StairShapes.StairLookup lookup = StairShapes.lookup(Roofs.byPosition(plan));
			for (PlannedBlock block : plan.blocks()) {
				if (block.piece().isStair()) {
					assertEquals(StairShapes.shapeOf(block.piece().facing(), block.x(), block.y(), block.z(), lookup), block.piece().shape(),
							"unstable stair " + block);
				}
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = RoofStyle.class, names = "FLAT", mode = EnumSource.Mode.EXCLUDE)
	void everyRoofIsWatertight(RoofStyle style) {
		for (Footprint f : outlines()) {
			for (Pitch pitch : Pitch.values()) {
				for (int o = 0; o <= 2; o++) {
					RoofPlan plan = Roofs.plan(f, RoofSpec.DEFAULTS.withStyle(style).withPitch(pitch).withOverhang(o));
					Watertight.check(plan);
				}
			}
		}
	}

	@ParameterizedTest
	@EnumSource(RoofStyle.class)
	void everyFootprintColumnIsCoveredAndNothingIsBelowTheEaves(RoofStyle style) {
		for (Footprint f : outlines()) {
			RoofPlan plan = Roofs.plan(f, RoofSpec.DEFAULTS.withStyle(style).withOverhang(2));
			for (int z = f.minZ(); z <= f.maxZ(); z++) {
				for (int x = f.minX(); x <= f.maxX(); x++) {
					if (f.contains(x, z)) {
						PlannedBlock top = Roofs.top(plan, x, z);
						assertNotNull(top, style + " leaves " + x + "," + z + " open\n" + Roofs.topView(plan));
						assertTrue(top.y() >= TOP, style + " roof inside the walls at " + top);
					}
				}
			}
			for (PlannedBlock block : plan.blocks()) {
				if (f.contains(block.x(), block.z())) {
					assertTrue(block.y() >= (style == RoofStyle.FLAT ? TOP : TOP + 1), "block inside the house below the wall top: " + block);
				}
			}
		}
	}

	@Test
	void planIsDeterministicAndSortedBottomUp() {
		RoofPlan a = Roofs.plan(Roofs.ell(11, 9, 5, 4), RoofStyle.HIP);
		RoofPlan b = Roofs.plan(Roofs.ell(11, 9, 5, 4), RoofStyle.HIP);
		assertEquals(a.blocks(), b.blocks());
		for (int i = 1; i < a.size(); i++) {
			assertTrue(a.blocks().get(i - 1).y() <= a.blocks().get(i).y());
		}
	}

	@Test
	void oversizedOutlinesAreRefused() {
		Footprint huge = Footprint.rectangle(0, 0, RoofPlanner.MAX_SPAN, 3, TOP);
		assertThrows(PlanException.class, () -> RoofPlanner.plan(huge, RoofSpec.DEFAULTS));
		Footprint wideTower = Footprint.rectangle(0, 0, RoofPlanner.MAX_ROUND_SPAN, 3, TOP);
		assertThrows(PlanException.class, () -> RoofPlanner.plan(wideTower, RoofSpec.DEFAULTS.withStyle(RoofStyle.CONE)));
	}

	@Test
	void singleColumnStillGetsARoof() {
		RoofPlan plan = Roofs.plan(Roofs.rect(1, 1), RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP).withOverhang(0));
		assertEquals(1, plan.size());
		assertNull(plan.blocks().getFirst().piece().isStair() ? plan.blocks().getFirst() : null);
	}
}
