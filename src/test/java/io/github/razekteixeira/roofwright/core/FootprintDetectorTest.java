package io.github.razekteixeira.roofwright.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class FootprintDetectorTest {
	private final Set<Long> solid = new HashSet<>();

	private void block(int x, int z) {
		solid.add(((long) x << 32) | (z & 0xffffffffL));
	}

	private void ring(int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x++) {
			block(x, z0);
			block(x, z1);
		}
		for (int z = z0; z <= z1; z++) {
			block(x0, z);
			block(x1, z);
		}
	}

	private Footprint detect(int x, int z) throws PlanException {
		return FootprintDetector.detect(x, 70, z, (bx, bz) -> solid.contains(((long) bx << 32) | (bz & 0xffffffffL)), 64);
	}

	@Test
	void hollowRectangleBecomesTheWholeRectangle() throws PlanException {
		ring(0, 0, 8, 5);
		Footprint f = detect(4, 0);
		assertEquals(Footprint.rectangle(0, 0, 8, 5, 70), f);
		assertTrue(f.isEdge(0, 3));
		assertFalse(f.isEdge(4, 3));
	}

	@Test
	void ellShapedWallsGiveAnEll() throws PlanException {
		for (int x = 0; x <= 10; x++) {
			block(x, 8);
		}
		for (int z = 0; z <= 8; z++) {
			block(0, z);
			block(10, z >= 4 ? z : 4);
		}
		for (int x = 0; x <= 5; x++) {
			block(x, 0);
		}
		for (int z = 0; z <= 4; z++) {
			block(5, z);
		}
		for (int x = 5; x <= 10; x++) {
			block(x, 4);
		}
		Footprint f = detect(0, 0);
		assertEquals(Roofs.ell(11, 9, 5, 4).atY(70), f);
	}

	@Test
	void wallsTouchingOnlyAtCornersStillClose() throws PlanException {
		// A ring with its north-east corner block missing: the walls meet diagonally.
		ring(0, 0, 6, 6);
		solid.remove(((long) 6 << 32) | 0L);
		Footprint f = detect(3, 6);
		assertTrue(f.contains(3, 3));
		assertFalse(f.contains(6, 0), "the missing corner stays outside");
	}

	@Test
	void aGapInTheWallTopIsReported() {
		ring(0, 0, 6, 6);
		solid.remove(((long) 3 << 32) | 0L);
		PlanException error = assertThrows(PlanException.class, () -> detect(0, 3));
		assertTrue(error.getMessage().contains("do not enclose"), error.getMessage());
	}

	@Test
	void courtyardsAreRoofedOver() throws PlanException {
		ring(0, 0, 12, 12);
		ring(4, 4, 8, 8);
		for (int z = 0; z <= 4; z++) {
			block(6, z);
		}
		Footprint f = detect(0, 0);
		assertEquals(13 * 13, f.cellCount());
	}

	@Test
	void solidPlatformIsItsOwnOutline() throws PlanException {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 4; z++) {
				block(x, z);
			}
		}
		assertEquals(Footprint.rectangle(0, 0, 4, 3, 70), detect(2, 2));
	}

	@Test
	void separateBuildingsAreNotMerged() throws PlanException {
		ring(0, 0, 5, 5);
		ring(7, 0, 12, 5);
		assertEquals(Footprint.rectangle(0, 0, 5, 5, 70), detect(0, 0));
	}

	@Test
	void airAndHugeWallsAreRefused() {
		assertThrows(PlanException.class, () -> detect(0, 0));
		for (int x = 0; x < 100; x++) {
			block(x, 0);
		}
		PlanException error = assertThrows(PlanException.class, () -> detect(0, 0));
		assertTrue(error.getMessage().contains("more than 64"), error.getMessage());
	}
}
