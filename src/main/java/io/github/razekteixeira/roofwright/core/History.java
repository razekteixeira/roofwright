package io.github.razekteixeira.roofwright.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

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

	/** The operation {@link #undo} would take, left in place. */
	public Optional<T> peekUndo() {
		return Optional.ofNullable(done.peek());
	}

	/**
	 * Ends an undo of {@code entry}, which must still be the latest operation (otherwise nothing changes).
	 * {@code reverted}, the part that was taken back, moves to redo; {@code kept}, the part a cancelled undo
	 * never reached, stays undoable. Either may be {@code null}.
	 */
	public void finishUndo(T entry, @Nullable T kept, @Nullable T reverted) {
		if (done.peek() != entry) {
			return;
		}
		done.pop();
		if (kept != null) {
			done.push(kept);
		}
		if (reverted != null) {
			undone.push(reverted);
		}
	}

	/** The same for redo: {@code reapplied} becomes undoable again, {@code kept} stays to redo. */
	public void finishRedo(T entry, @Nullable T kept, @Nullable T reapplied) {
		if (undone.peek() != entry) {
			return;
		}
		undone.pop();
		if (kept != null) {
			undone.push(kept);
		}
		if (reapplied != null) {
			done.push(reapplied);
		}
	}

	/** The operation {@link #redo} would take, left in place. */
	public Optional<T> peekRedo() {
		return Optional.ofNullable(undone.peek());
	}

	public int undoCount() {
		return done.size();
	}

	public int redoCount() {
		return undone.size();
	}
}
