package io.github.razekteixeira.roofwright.core;

import java.util.Arrays;
import java.util.Locale;

/**
 * Times the planner on its own, the way the server runs it (one thread, warmed up). For every style and
 * size it plans the roof repeatedly and prints the median and worst of the timed runs.
 *
 * <p>Run with {@code ./gradlew benchmark}; optional {@code -Pbench="sizes repeats"}, e.g. {@code "32,128 20"}.
 * Square footprints test the plain case; "L" footprints (the north-east quarter missing) add valleys.
 */
public final class PlannerBenchmark {
	private PlannerBenchmark() {
	}

	public static void main(String[] args) throws PlanException {
		int[] sizes = args.length > 0 ? Arrays.stream(args[0].split(",")).mapToInt(Integer::parseInt).toArray() : new int[] {16, 32, 64, 128, 256};
		int repeats = args.length > 1 ? Integer.parseInt(args[1]) : 15;
		RoofStyle[] styles = {RoofStyle.GABLE, RoofStyle.HIP, RoofStyle.DUTCH_GABLE, RoofStyle.MANSARD, RoofStyle.CONE, RoofStyle.FLAT};
		System.out.println("Roofwright planner benchmark, " + repeats + " timed runs after warm-up, Java " + Runtime.version());
		System.out.printf(Locale.ROOT, "%-12s %-5s %9s %9s %9s %9s%n", "style", "shape", "size", "blocks", "median ms", "max ms");
		for (int size : sizes) {
			for (boolean ell : new boolean[] {false, true}) {
				Footprint footprint = footprint(size, ell);
				for (RoofStyle style : styles) {
					if ((style == RoofStyle.CONE) && size > RoofPlanner.MAX_ROUND_SPAN) {
						continue;
					}
					RoofSpec spec = RoofSpec.DEFAULTS.withStyle(style);
					int blocks = 0;
					for (int i = 0; i < 5; i++) {
						blocks = RoofPlanner.plan(footprint, spec).size();
					}
					double[] millis = new double[repeats];
					for (int i = 0; i < repeats; i++) {
						long start = System.nanoTime();
						RoofPlanner.plan(footprint, spec);
						millis[i] = (System.nanoTime() - start) / 1e6;
					}
					Arrays.sort(millis);
					System.out.printf(Locale.ROOT, "%-12s %-5s %9s %9d %9.2f %9.2f%n", style.id(), ell ? "L" : "rect",
							size + "x" + size, blocks, millis[repeats / 2], millis[repeats - 1]);
				}
			}
		}
	}

	private static Footprint footprint(int size, boolean ell) {
		Footprint.Builder builder = new Footprint.Builder(64);
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				if (!ell || x < size / 2 || z >= size / 2) {
					builder.add(x, z);
				}
			}
		}
		return builder.build();
	}
}
