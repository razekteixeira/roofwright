package io.github.razekteixeira.roofwright;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import io.github.razekteixeira.roofwright.core.Footprint;
import io.github.razekteixeira.roofwright.core.FootprintDetector;
import io.github.razekteixeira.roofwright.core.History;
import io.github.razekteixeira.roofwright.core.Piece;
import io.github.razekteixeira.roofwright.core.PlanException;
import io.github.razekteixeira.roofwright.core.PlannedBlock;
import io.github.razekteixeira.roofwright.core.Role;
import io.github.razekteixeira.roofwright.core.RoofPlan;
import io.github.razekteixeira.roofwright.core.RoofPlanner;
import io.github.razekteixeira.roofwright.core.Settings;

/**
 * What the wand and the commands do: detect or select an outline, plan and preview a roof, place it,
 * undo and redo. Runs on the server thread.
 */
public final class RoofService {
	private static final UUID CONSOLE = new UUID(0, 0);
	private static final Map<UUID, RoofSession> SESSIONS = new HashMap<>();

	private RoofService() {
	}

	/** A planned block with its final state and whether it may be placed now. */
	public record Target(BlockPos pos, BlockState state, Protection.Verdict verdict, boolean settle, boolean surface) {
	}

	/** A plan resolved against the world: states, verdicts and the steps to place. */
	public record Prepared(RoofPlan plan, ServerLevel level, List<Target> targets) {
		public List<PlacementJob.Step> steps() {
			List<PlacementJob.Step> steps = new ArrayList<>();
			for (Target target : targets) {
				if (target.verdict() == Protection.Verdict.NONE) {
					steps.add(new PlacementJob.Step(target.pos(), null, target.state(), target.settle()));
				}
			}
			return steps;
		}

		public Map<Protection.Verdict, Integer> blocked() {
			Map<Protection.Verdict, Integer> counts = new EnumMap<>(Protection.Verdict.class);
			for (Target target : targets) {
				if (target.verdict() != Protection.Verdict.NONE) {
					counts.merge(target.verdict(), 1, Integer::sum);
				}
			}
			return counts;
		}
	}

	public static RoofSession session(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return session(player == null ? CONSOLE : player.getUUID());
	}

	public static RoofSession session(UUID id) {
		return SESSIONS.computeIfAbsent(id, key -> new RoofSession());
	}

	/** A player left: their preview is gone, and a session with nothing to undo or redo is dropped. */
	static void disconnected(UUID id) {
		RoofSession session = SESSIONS.get(id);
		if (session == null) {
			return;
		}
		session.preview = null;
		if (session.history.undoCount() == 0 && session.history.redoCount() == 0) {
			SESSIONS.remove(id);
		}
	}

	static void clearAll() {
		SESSIONS.clear();
	}

	// --- outline ---------------------------------------------------------------------------------

	/** Detects the outline whose wall top contains {@code pos} and selects it. */
	public static Footprint detect(CommandSourceStack source, ServerLevel level, BlockPos pos) throws PlanException {
		// Unloaded columns count as open: detection must never load chunks.
		Footprint footprint = FootprintDetector.detect(pos.getX(), pos.getY(), pos.getZ(), (x, z) -> {
			BlockPos column = new BlockPos(x, pos.getY(), z);
			return level.isLoaded(column) && isWall(level.getBlockState(column));
		}, maxSpan(source));
		RoofSession session = session(source);
		BlockState clicked = level.getBlockState(pos);
		// Gable walls copy the clicked block only when it is safe to build with; otherwise they use the full block.
		session.wallSample = Materials.problem(clicked, true) == null ? clicked : null;
		select(session, level, footprint);
		return footprint;
	}

	/** The widest outline this builder may roof: {@code maxSpan}, or the planner's hard ceiling with {@code roofwright.unlimited}. */
	static int maxSpan(CommandSourceStack source) {
		return RoofwrightCommands.canExceedLimits(source) ? RoofPlanner.MAX_SPAN : RoofwrightConfig.get().maxSpan();
	}

	/** Solid enough to be part of a wall: anything but air, fluids and replaceable plants or snow. */
	static boolean isWall(BlockState state) {
		return !state.isAir() && !state.canBeReplaced() && state.getFluidState().isEmpty();
	}

	/** Selects every column of the rectangle, with the wall top at the higher of the two corners. */
	public static Footprint selectRectangle(CommandSourceStack source, ServerLevel level, BlockPos from, BlockPos to, boolean add)
			throws PlanException {
		RoofSession session = session(source);
		int y = Math.max(from.getY(), to.getY());
		Footprint previous = add && session.footprint != null && level.dimension().equals(session.level) ? session.footprint : null;
		// Check the size from the corners before collecting a single column: a far corner must not cost memory.
		int minX = Math.min(from.getX(), to.getX());
		int maxX = Math.max(from.getX(), to.getX());
		int minZ = Math.min(from.getZ(), to.getZ());
		int maxZ = Math.max(from.getZ(), to.getZ());
		if (previous != null) {
			minX = Math.min(minX, previous.minX());
			maxX = Math.max(maxX, previous.maxX());
			minZ = Math.min(minZ, previous.minZ());
			maxZ = Math.max(maxZ, previous.maxZ());
		}
		long width = Footprint.span(minX, maxX);
		long depth = Footprint.span(minZ, maxZ);
		int maxSpan = maxSpan(source);
		if (width > maxSpan || depth > maxSpan) {
			throw new PlanException("The selection is " + width + " x " + depth
					+ " blocks; the limit is " + maxSpan + " (maxSpan in config/roofwright.json).");
		}
		Footprint.Builder builder = new Footprint.Builder(add && session.footprint != null ? session.footprint.wallTopY() : y);
		if (previous != null) {
			builder.addAll(previous);
		}
		builder.addRectangle(from.getX(), from.getZ(), to.getX(), to.getZ());
		Footprint footprint = builder.build();
		if (!add) {
			session.wallSample = null;
		}
		select(session, level, footprint);
		return footprint;
	}

	private static void select(RoofSession session, ServerLevel level, Footprint footprint) {
		session.footprint = footprint;
		session.level = level.dimension();
	}

	// --- plan -------------------------------------------------------------------------------------

	/** Plans the selected roof and checks every block against the world. */
	public static Prepared prepare(CommandSourceStack source, boolean force) throws PlanException {
		RoofSession session = session(source);
		Footprint footprint = session.footprint;
		if (footprint == null || session.level == null) {
			throw new PlanException("Select a building first: right-click the top of a wall with the wand, or use /roof detect.");
		}
		ServerLevel level = source.getServer().getLevel(session.level);
		if (level == null) {
			throw new PlanException("The selected building is in a world that is not loaded.");
		}
		RoofPlan plan = RoofPlanner.plan(footprint, session.spec);
		requireWithinLimit(source, plan.size());
		// Checked again here, not only when chosen: defaults, and a data pack may have changed the tag since.
		String problem = session.materials.problem();
		BlockState gable = gableState(session);
		if (problem == null) {
			problem = Materials.problem(gable, true);
		}
		if (problem != null) {
			throw new PlanException("Cannot build with these blocks: " + problem + ". Choose others with /roof material or /roof gable.");
		}
		Map<Long, Integer> highest = new HashMap<>();
		for (PlannedBlock block : plan.blocks()) {
			highest.merge(((long) block.x() << 32) | (block.z() & 0xffffffffL), block.y(), Math::max);
		}
		ServerPlayer player = source.getPlayer();
		List<Target> targets = new ArrayList<>(plan.size());
		for (PlannedBlock block : plan.blocks()) {
			BlockPos pos = new BlockPos(block.x(), block.y(), block.z());
			BlockState state = block.role() == Role.GABLE ? gable : session.materials.state(block.piece());
			boolean surface = highest.get(((long) block.x() << 32) | (block.z() & 0xffffffffL)) == block.y();
			// Walls connect and stairs take their corner shape from what is really placed around them.
			boolean settle = block.piece().kind() == Piece.Kind.WALL || block.piece().kind() == Piece.Kind.STAIR;
			targets.add(new Target(pos, state, Protection.check(level, pos, player, force), settle, surface));
		}
		return new Prepared(plan, level, targets);
	}

	/**
	 * Refuses jobs over {@code maxBlocks}; {@code roofwright.unlimited} lifts that, but never past
	 * {@link Settings#BLOCK_CEILING}.
	 */
	private static void requireWithinLimit(CommandSourceStack source, int blocks) throws PlanException {
		int limit = RoofwrightConfig.get().maxBlocks();
		if (blocks > limit && !RoofwrightCommands.canExceedLimits(source)) {
			throw new PlanException("This roof needs " + blocks + " blocks; the limit is " + limit + " (maxBlocks in config/roofwright.json).");
		}
		if (blocks > Settings.BLOCK_CEILING) {
			throw new PlanException("This roof needs " + blocks + " blocks; Roofwright never places more than " + Settings.BLOCK_CEILING + " at once.");
		}
	}

	static BlockState gableState(RoofSession session) {
		if (session.gableBlock != null) {
			return session.gableBlock;
		}
		return session.wallSample != null ? session.wallSample : session.materials.full().defaultBlockState();
	}

	/** Plans and shows the roof to a player, with a one-line summary. */
	public static void preview(CommandSourceStack source) throws PlanException {
		ServerPlayer player = source.getPlayer();
		RoofSession session = session(source);
		if (player != null && session.level != null && !player.level().dimension().equals(session.level)) {
			throw new PlanException("Your selection is in " + session.level.identifier() + "; go back there or select a building here.");
		}
		Prepared prepared = prepare(source, false);
		Preview.Shown shown = player != null ? Preview.show(player, session, prepared, RoofwrightConfig.get()) : null;
		source.sendSuccess(() -> summary(session, prepared, shown), false);
	}

	private static Component summary(RoofSession session, Prepared prepared, Preview.@Nullable Shown shown) {
		RoofPlan plan = prepared.plan();
		StringBuilder text = new StringBuilder();
		text.append(capitalised(session.spec.describe())).append(" roof in ").append(session.materials.name())
				.append(": ").append(plan.size()).append(" blocks over ").append(plan.footprint().cellCount()).append(" columns");
		Map<Protection.Verdict, Integer> blocked = prepared.blocked();
		if (!blocked.isEmpty()) {
			text.append(", ").append(describe(blocked)).append(" (red in the preview, will be skipped)");
		}
		if (shown != null && shown.surfaceOnly()) {
			text.append(". Preview shows the top surface only (").append(shown.shown()).append(" of ").append(plan.size()).append(")");
		}
		text.append(".");
		plan.notes().forEach(note -> text.append(" ").append(note));
		return Component.literal(text.toString()).withStyle(ChatFormatting.AQUA).append(Component.literal(
				" Sneak + right-click with the wand or /roof place to build.").withStyle(ChatFormatting.GRAY));
	}

	static String describe(Map<Protection.Verdict, Integer> counts) {
		List<String> parts = new ArrayList<>();
		counts.forEach((verdict, count) -> parts.add(count + " " + verdict.reason()));
		return String.join(", ", parts);
	}

	private static String capitalised(String text) {
		return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}

	// --- place, undo, redo -----------------------------------------------------------------------

	public static PlacementJob place(CommandSourceStack source, boolean force) throws PlanException {
		RoofSession session = session(source);
		UUID owner = ownerOf(source);
		requireIdle(owner);
		Prepared prepared = prepare(source, force);
		List<PlacementJob.Step> steps = prepared.steps();
		if (steps.isEmpty()) {
			throw new PlanException("Nothing to place: every block of this roof is " + describe(prepared.blocked()) + ".");
		}
		ServerPlayer player = source.getPlayer();
		if (player != null) {
			Preview.clear(player, session);
		}
		String label = session.spec.describe() + " roof";
		PlacementJob job = new PlacementJob(PlacementJob.Kind.PLACE, owner, source.getPlayer(), prepared.level(), label, steps, force, prepared.blocked(),
				done -> finished(source.getServer(), done));
		Placements.start(job);
		int blocked = prepared.targets().size() - steps.size();
		source.sendSuccess(() -> Component.literal("Building " + label + ": " + steps.size() + " blocks"
				+ (blocked > 0 ? ", skipping " + describe(prepared.blocked()) : "") + ".").withStyle(ChatFormatting.GRAY), false);
		return job;
	}

	public static PlacementJob undo(CommandSourceStack source) throws PlanException {
		UUID owner = ownerOf(source);
		requireIdle(owner);
		Journal journal = session(source).history.peekUndo()
				.orElseThrow(() -> new PlanException("Nothing to undo."));
		List<PlacementJob.Step> steps = new ArrayList<>(journal.changes().size());
		for (int i = journal.changes().size() - 1; i >= 0; i--) {
			Journal.Change change = journal.changes().get(i);
			steps.add(new PlacementJob.Step(change.pos(), change.after(), change.before(), false));
		}
		return replay(source, owner, journal, steps, PlacementJob.Kind.UNDO);
	}

	public static PlacementJob redo(CommandSourceStack source) throws PlanException {
		UUID owner = ownerOf(source);
		requireIdle(owner);
		RoofSession session = session(source);
		Journal next = session.history.peekRedo().orElseThrow(() -> new PlanException("Nothing to redo."));
		// Redo places blocks again, so it needs the same rights as placing them did.
		if (next.force() && !RoofwrightCommands.canForce(source)) {
			throw new PlanException("That roof replaced other blocks; redoing it needs the roofwright.force permission.");
		}
		requireWithinLimit(source, next.changes().size());
		Journal journal = next;
		List<PlacementJob.Step> steps = new ArrayList<>(journal.changes().size());
		for (Journal.Change change : journal.changes()) {
			steps.add(new PlacementJob.Step(change.pos(), change.before(), change.after(), false));
		}
		return replay(source, owner, journal, steps, PlacementJob.Kind.REDO);
	}

	private static PlacementJob replay(CommandSourceStack source, UUID owner, Journal journal, List<PlacementJob.Step> steps,
			PlacementJob.Kind kind) throws PlanException {
		ServerLevel level = source.getServer().getLevel(journal.level());
		if (level == null) {
			throw new PlanException("That roof is in a world that is not loaded.");
		}
		// History moves only when the job ends, by what it really did, so a cancelled undo or redo leaves
		// the part it never reached where it was.
		PlacementJob job = new PlacementJob(kind, owner, source.getPlayer(), level, journal.label(), steps, false, done -> {
			settleHistory(session(owner).history, journal, done);
			finished(source.getServer(), done);
		});
		Placements.start(job);
		return job;
	}

	/** Splits the journal at the job's progress: undo runs from the end backwards, redo from the start. */
	static void settleHistory(History<Journal> history, Journal journal, PlacementJob job) {
		List<Journal.Change> changes = journal.changes();
		int n = changes.size();
		int processed = Math.min(job.progress(), n);
		if (processed == 0) {
			return;
		}
		if (job.kind() == PlacementJob.Kind.UNDO) {
			history.finishUndo(journal, processed == n ? null : part(journal, 0, n - processed), part(journal, n - processed, n));
		} else {
			history.finishRedo(journal, processed == n ? null : part(journal, processed, n), part(journal, 0, processed));
		}
	}

	private static Journal part(Journal journal, int from, int to) {
		return new Journal(journal.level(), journal.label(), journal.force(), journal.changes().subList(from, to));
	}

	private static UUID ownerOf(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player == null ? CONSOLE : player.getUUID();
	}

	private static void requireIdle(UUID owner) throws PlanException {
		PlacementJob running = Placements.activeFor(owner);
		if (running != null) {
			throw new PlanException("Still working on your last roof (" + running.progress() + " of " + running.total()
					+ " blocks). Wait, or /roof cancel.");
		}
	}

	/** Reports a finished job to its owner and records it for undo. */
	static void finished(MinecraftServer server, PlacementJob job) {
		UUID owner = job.owner();
		if (job.kind() == PlacementJob.Kind.PLACE && job.placed() > 0) {
			session(owner).history.record(job.journal(), RoofwrightConfig.get().historySize());
		}
		StringBuilder text = new StringBuilder(job.kind().done).append(": ").append(job.label()).append(", ")
				.append(job.placed()).append(" blocks");
		if (!job.skipped().isEmpty()) {
			text.append(", skipped ").append(describe(job.skipped()));
		}
		if (job.conflicts() > 0) {
			text.append(", left ").append(job.conflicts()).append(" changed since alone");
		}
		if (job.isCancelled()) {
			text.append(" (cancelled)");
		}
		text.append(".");
		if (job.kind() == PlacementJob.Kind.PLACE && job.placed() > 0) {
			text.append(" /roof undo takes it back.");
		}
		Roofwright.LOGGER.info("{} ({} by {}): {} placed, {} conflicts, skipped {}, {} ticks, max {} ms and {} blocks per tick",
				job.kind(), job.label(), owner, job.placed(), job.conflicts(), job.skipped(), job.ticks(),
				String.format(java.util.Locale.ROOT, "%.2f", job.maxTickNanos() / 1e6), job.maxTickBlocks());
		ServerPlayer player = job.player();
		if (player != null) {
			player.sendSystemMessage(Component.literal(text.toString()).withStyle(ChatFormatting.GREEN));
		}
	}
}
