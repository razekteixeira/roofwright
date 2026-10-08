package io.github.razekteixeira.roofwright;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.MinecraftServer;

import io.github.razekteixeira.roofwright.core.Settings;

/**
 * Runs placement jobs at the end of every server tick. All jobs together share one budget per tick
 * ({@code blocksPerTick} and {@code millisPerTick}), oldest first, so a big roof never stalls the server.
 */
public final class Placements {
	private static final List<PlacementJob> JOBS = new ArrayList<>();

	private Placements() {
	}

	public static void start(PlacementJob job) {
		JOBS.add(job);
	}

	public static @Nullable PlacementJob activeFor(UUID owner) {
		for (PlacementJob job : JOBS) {
			if (owner.equals(job.owner())) {
				return job;
			}
		}
		return null;
	}

	public static List<PlacementJob> active() {
		return List.copyOf(JOBS);
	}

	static void onServerTick(MinecraftServer server) {
		if (JOBS.isEmpty()) {
			return;
		}
		Settings settings = RoofwrightConfig.get();
		int budget = settings.blocksPerTick();
		long deadline = System.nanoTime() + settings.millisPerTick() * 1_000_000L;
		Iterator<PlacementJob> it = JOBS.iterator();
		while (it.hasNext()) {
			PlacementJob job = it.next();
			if (!job.isDone() && budget > 0) {
				budget -= job.tick(budget, deadline);
			}
			if (job.isDone()) {
				it.remove();
				job.finish();
			}
		}
	}

	/** Runs a job to the end right now, ignoring the budget (GameTests and the benchmark command). */
	public static void runNow(PlacementJob job) {
		while (!job.isDone()) {
			job.tick(Integer.MAX_VALUE, Long.MAX_VALUE);
		}
		JOBS.remove(job);
		job.finish();
	}

	static void clear() {
		JOBS.clear();
	}
}
