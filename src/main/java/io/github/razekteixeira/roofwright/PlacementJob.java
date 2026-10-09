package io.github.razekteixeira.roofwright;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places a list of block changes over as many ticks as the budget needs. Each change is re-checked when
 * its turn comes, so a block someone put there in the meantime is never overwritten. After the last
 * change, blocks marked {@code settle} (walls and stairs) are recomputed from their real neighbours, also
 * within the budget, so a stair beside a skipped block gets the shape Minecraft would give it there.
 */
public final class PlacementJob {
	/**
	 * Stairs and slabs are placed with the shapes Roofwright already computed, so neighbours need no shape
	 * update ({@code UPDATE_KNOWN_SHAPE}) and no block update storm reaches redstone or sand nearby.
	 */
	static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

	public enum Kind {
		PLACE("Roof built"),
		UNDO("Undone"),
		REDO("Redone");

		final String done;

		Kind(String done) {
			this.done = done;
		}
	}

	/**
	 * One change. With {@code expected} null the replace policy decides (air, or plain blocks with
	 * force); otherwise the block must still be exactly {@code expected}.
	 *
	 * @param settle recompute the state from its neighbours after everything is placed (walls, stairs)
	 */
	public record Step(BlockPos pos, @Nullable BlockState expected, BlockState target, boolean settle) {
	}

	private final Kind kind;
	private final UUID owner;
	private final @Nullable ServerPlayer player;
	private final @Nullable NameAndId builder;
	private final ServerLevel level;
	private final String label;
	private final List<Step> steps;
	private final boolean force;
	private final Consumer<PlacementJob> onDone;
	private final java.util.Set<BlockPos> settle = new java.util.HashSet<>();
	private final List<Journal.Change> changes = new ArrayList<>();
	private final Map<Protection.Verdict, Integer> skipped = new EnumMap<>(Protection.Verdict.class);
	private int next;
	/** Next entry of {@link #changes} to settle, once every step has run. */
	private int nextSettle;
	private int lastTickWrites;
	private int conflicts;
	private int ticks;
	private long totalNanos;
	private long maxTickNanos;
	private int maxTickBlocks;
	private boolean cancelled;

	/**
	 * @param owner  who started it (the console has its own id), for undo history and "one job at a time"
	 * @param player the builder, or {@code null} for the console; claims keep being checked against the
	 *               builder's name and id after they log off
	 */
	public PlacementJob(Kind kind, UUID owner, @Nullable ServerPlayer player, ServerLevel level, String label, List<Step> steps,
			boolean force, Consumer<PlacementJob> onDone) {
		this(kind, owner, player, level, label, steps, force, Map.of(), onDone);
	}

	/** @param skippedBefore blocks the plan already left out, so the final report counts them too */
	public PlacementJob(Kind kind, UUID owner, @Nullable ServerPlayer player, ServerLevel level, String label, List<Step> steps,
			boolean force, Map<Protection.Verdict, Integer> skippedBefore, Consumer<PlacementJob> onDone) {
		skipped.putAll(skippedBefore);
		this.kind = kind;
		this.owner = owner;
		this.player = player;
		this.builder = player == null ? null : player.nameAndId();
		this.level = level;
		this.label = label;
		this.steps = List.copyOf(steps);
		for (Step step : steps) {
			if (step.settle()) {
				settle.add(step.pos());
			}
		}
		this.force = force;
		this.onDone = onDone;
	}

	/**
	 * Handles up to {@code maxBlocks} steps and settle writes, or stops when {@code deadline}
	 * (System.nanoTime) passes, whichever comes first. At least one unit of work is done per call, so a job
	 * always progresses.
	 *
	 * @return steps handled (placed or skipped) plus settle writes in this call
	 */
	int tick(int maxBlocks, long deadline) {
		long start = System.nanoTime();
		@Nullable ServerPlayer player = this.player != null && !this.player.hasDisconnected() ? this.player : null;
		int done = 0;
		lastTickWrites = 0;
		while (done < maxBlocks && (done == 0 || System.nanoTime() < deadline)) {
			if (next < steps.size()) {
				apply(steps.get(next++), player);
				done++;
			} else if (settleNext()) {
				done++;
			} else {
				break;
			}
		}
		long nanos = System.nanoTime() - start;
		ticks++;
		totalNanos += nanos;
		maxTickNanos = Math.max(maxTickNanos, nanos);
		maxTickBlocks = Math.max(maxTickBlocks, done);
		return done;
	}

	private void apply(Step step, @Nullable ServerPlayer player) {
		BlockPos pos = step.pos();
		if (!level.isLoaded(pos)) {
			skip(Protection.Verdict.NOT_LOADED);
			return;
		}
		BlockState current = level.getBlockState(pos);
		if (step.expected() != null) {
			if (current != step.expected()) {
				conflicts++;
				return;
			}
			// Undo and redo pass every check placing does (world border, claims, game mode), with force
			// because the block they replace is exactly the one the roof journal recorded.
			Protection.Verdict verdict = Protection.check(level, pos, player, builder, true);
			if (verdict != Protection.Verdict.NONE) {
				skip(verdict);
				return;
			}
		} else {
			Protection.Verdict verdict = Protection.check(level, pos, player, builder, force);
			if (verdict != Protection.Verdict.NONE) {
				skip(verdict);
				return;
			}
		}
		if (level.setBlock(pos, step.target(), FLAGS)) {
			lastTickWrites++;
			changes.add(new Journal.Change(pos.immutable(), current, step.target()));
		}
	}

	/**
	 * Settles the next change that needs it: walls connect and stairs take their corner shape from the
	 * blocks really around them. Never reads an unloaded neighbour.
	 *
	 * @return whether a block was written; false when nothing is left to settle
	 */
	private boolean settleNext() {
		while (nextSettle < changes.size()) {
			int i = nextSettle++;
			Journal.Change change = changes.get(i);
			BlockPos pos = change.pos();
			if (!needsSettling(change) || !level.hasChunksAt(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))
					|| level.getBlockState(pos) != change.after()) {
				continue;
			}
			BlockState settled = Block.updateFromNeighbourShapes(change.after(), level, pos);
			if (settled != change.after()) {
				level.setBlock(pos, settled, FLAGS);
				lastTickWrites++;
				changes.set(i, new Journal.Change(pos, change.before(), settled));
				return true;
			}
		}
		return false;
	}

	private boolean needsSettling(Journal.Change change) {
		return settle.contains(change.pos());
	}

	private void skip(Protection.Verdict verdict) {
		skipped.merge(verdict, 1, Integer::sum);
	}

	void finish() {
		onDone.accept(this);
	}

	public boolean isDone() {
		return cancelled || next >= steps.size() && nextSettle >= changes.size();
	}

	/** Blocks written in the last {@link #tick} call, settle writes included. */
	public int lastTickWrites() {
		return lastTickWrites;
	}

	public void cancel() {
		cancelled = true;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public Kind kind() {
		return kind;
	}

	public UUID owner() {
		return owner;
	}

	/** The builder while they are still connected. */
	public @Nullable ServerPlayer player() {
		return player != null && !player.hasDisconnected() ? player : null;
	}

	public ServerLevel level() {
		return level;
	}

	public String label() {
		return label;
	}

	public int total() {
		return steps.size();
	}

	public int progress() {
		return next;
	}

	public Journal journal() {
		return new Journal(level.dimension(), label, force, changes);
	}

	public int placed() {
		return changes.size();
	}

	public int conflicts() {
		return conflicts;
	}

	public Map<Protection.Verdict, Integer> skipped() {
		return skipped;
	}

	public int ticks() {
		return ticks;
	}

	public long maxTickNanos() {
		return maxTickNanos;
	}

	public long totalNanos() {
		return totalNanos;
	}

	public int maxTickBlocks() {
		return maxTickBlocks;
	}
}
