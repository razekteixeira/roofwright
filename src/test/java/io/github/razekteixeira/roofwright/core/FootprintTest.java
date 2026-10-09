package io.github.razekteixeira.roofwright.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class FootprintTest {
	@Test
	void hugeRectanglesAreRefusedBeforeAnyColumnIsCollected() {
		// 100,000 x 100,000 would be ten billion boxed columns; it must fail at once, not run out of memory.
		assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
			Footprint.Builder builder = new Footprint.Builder(64);
			// The cheap case first: without the guard it fails at once instead of filling the heap.
			assertThrows(IllegalArgumentException.class, () -> builder.addRectangle(0, 0, Footprint.MAX_SIDE, 0));
			assertThrows(IllegalArgumentException.class, () -> builder.addRectangle(0, 0, 100_000, 100_000));
			assertEquals(0, builder.size(), "nothing was added");
			assertThrows(IllegalArgumentException.class, () -> Footprint.rectangle(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0, 64));
		});
	}

	@Test
	void theWidestAllowedRectangleStillWorks() {
		Footprint widest = Footprint.rectangle(0, 0, Footprint.MAX_SIDE - 1, 0, 64);
		assertEquals(Footprint.MAX_SIDE, widest.width());
	}

	@Test
	void spanNeverOverflows() {
		assertEquals(1, Footprint.span(5, 5));
		assertEquals(11, Footprint.span(10, 0));
		assertEquals(1L << 32, Footprint.span(Integer.MIN_VALUE, Integer.MAX_VALUE));
	}
}
