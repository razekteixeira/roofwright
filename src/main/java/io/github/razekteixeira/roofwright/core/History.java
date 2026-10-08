package io.github.razekteixeira.roofwright.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Undo and redo stacks for one player. A new entry clears the redo stack, as in every editor; the oldest
 * entries fall off when the history is full.
 */
public final class History<T> {
	private final Deque<T> done = new ArrayDeque<>();
	private final Deque<T> undone = new ArrayDeque<>();

	/** Records a new operation; with {@code limit} 0 nothing is kept. */
	public void record(T entry, int limit) {
		undone.clear();
		if (limit <= 0) {
			done.clear();
			return;
		}
		done.push(entry);
		while (done.size() > limit) {
			done.removeLast();
		}
	}

	/** Takes the latest operation to undo it; it moves to the redo stack. */
	public Optional<T> undo() {
		T entry = done.poll();
		if (entry != null) {
			undone.push(entry);
		}
		return Optional.ofNullable(entry);
	}

	/** Takes the latest undone operation to redo it; it moves back to the undo stack. */
	public Optional<T> redo() {
		T entry = undone.poll();
		if (entry != null) {
			done.push(entry);
		}
		return Optional.ofNullable(entry);
	}

	public int undoCount() {
		return done.size();
	}

	public int redoCount() {
		return undone.size();
	}
}
