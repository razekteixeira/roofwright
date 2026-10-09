package io.github.razekteixeira.roofwright.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class HistoryTest {
	@Test
	void undoThenRedoWalksBackAndForth() {
		History<String> history = new History<>();
		history.record("a", 10);
		history.record("b", 10);
		assertEquals(Optional.of("b"), history.undo());
		assertEquals(Optional.of("a"), history.undo());
		assertEquals(Optional.empty(), history.undo());
		assertEquals(Optional.of("a"), history.redo());
		assertEquals(Optional.of("b"), history.redo());
		assertEquals(Optional.empty(), history.redo());
	}

	@Test
	void aNewEntryClearsRedo() {
		History<String> history = new History<>();
		history.record("a", 10);
		history.undo();
		assertEquals(1, history.redoCount());
		history.record("b", 10);
		assertEquals(0, history.redoCount());
		assertEquals(Optional.of("b"), history.undo());
	}

	@Test
	void oldestFallOffAtTheLimit() {
		History<String> history = new History<>();
		for (String entry : new String[] {"a", "b", "c", "d"}) {
			history.record(entry, 3);
		}
		assertEquals(3, history.undoCount());
		history.undo();
		history.undo();
		assertEquals(Optional.of("b"), history.undo());
		assertTrue(history.undo().isEmpty(), "a was dropped");
	}

	@Test
	void limitZeroKeepsNothing() {
		History<String> history = new History<>();
		history.record("a", 0);
		assertEquals(0, history.undoCount());
	}

	@Test
	void peekRedoLeavesTheEntryInPlace() {
		History<String> history = new History<>();
		history.record("a", 3);
		assertTrue(history.peekRedo().isEmpty());
		history.undo();
		assertEquals(Optional.of("a"), history.peekRedo());
		assertEquals(1, history.redoCount(), "peeking takes nothing");
		assertEquals(Optional.of("a"), history.redo());
	}

	@Test
	void finishUndoSplitsAnInterruptedUndo() {
		History<String> history = new History<>();
		history.record("roof", 3);
		history.finishUndo("roof", "front half", "back half", 3);
		assertEquals(Optional.of("front half"), history.peekUndo(), "the part never reached stays undoable");
		assertEquals(Optional.of("back half"), history.peekRedo(), "the undone part can be redone");
		history.finishUndo("someone else", null, "x", 3);
		assertEquals(1, history.undoCount(), "only the latest entry can be finished");
	}

	@Test
	void finishRedoSplitsAnInterruptedRedo() {
		History<String> history = new History<>();
		history.record("roof", 3);
		history.finishUndo("roof", null, "roof", 3);
		history.finishRedo("roof", "rest", "start", 3);
		assertEquals(Optional.of("start"), history.peekUndo());
		assertEquals(Optional.of("rest"), history.peekRedo());
	}

	@Test
	void splitEntriesStillRespectTheLimit() {
		History<String> history = new History<>();
		history.record("roof", 1);
		history.finishUndo("roof", "rest", "part 1", 1);
		history.finishUndo("rest", "rest 2", "part 2", 1);
		assertEquals(1, history.undoCount(), "undo keeps at most one entry");
		assertEquals(1, history.redoCount(), "redo keeps at most one entry");
		assertEquals(Optional.of("part 2"), history.peekRedo(), "the newest split stays");
	}
}
