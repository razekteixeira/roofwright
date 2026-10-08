"""Plants one mutant at a time and checks that a named test kills it (gate G4 Tests with teeth).

Each mutant is an exact text replacement in one source file. For every mutant the script applies it, runs
the unit tests and the server GameTests, records which tests failed (from the JUnit XML reports and the
GameTest log), and restores the file. A mutant counts as killed only when a test fails by name; a build
that does not compile is reported as INVALID, never as a kill.

Usage: python3 -P tools/mutation.py [mutant ids...]   (needs JDK 25 as JAVA_HOME)
Writes build/mutation/report.md and exits 1 if any mutant survives or is invalid.
"""

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MAIN = ROOT / "src/main/java/io/github/razekteixeira/roofwright"

MUTANTS = [
    ("M01", "core/RoofPlanner.java", "best = Math.min(best, dist[i - width - 1] + 1);",
     "best = Math.min(best, dist[i - width - 1] + 2);", "chessboard distance loses its north-west diagonal"),
    ("M02", "core/RoofPlanner.java", "if (lower[dir.ordinal()]) {\n\t\t\t\t\treturn Piece.stair(dir.opposite());",
     "if (lower[dir.ordinal()]) {\n\t\t\t\t\treturn Piece.stair(dir);", "stairs face downhill"),
    ("M03", "core/StairShapes.java", "return front == facing.counterClockwise() ? StairShape.OUTER_LEFT : StairShape.OUTER_RIGHT;",
     "return front == facing.counterClockwise() ? StairShape.OUTER_RIGHT : StairShape.OUTER_LEFT;", "outer corners mirrored"),
    ("M04", "core/RoofPlanner.java", "bottom = Math.min(bottom, surface[grid.index(x + dir.dx, z + dir.dz)]);",
     "bottom = Math.min(bottom, bottom);", "columns no longer fill down to their lowest neighbour"),
    ("M05", "core/RoofPlanner.java", "if (lowest <= f.wallTopY() + 1) {", "if (lowest <= f.wallTopY() + 2) {",
     "gable walls one block short"),
    ("M06", "core/RoofPlanner.java", "if ((armAcross - 1) / 2 > (joinedAcross - 1) / 2) {", "if (true) {",
     "attached pieces never become arms"),
    ("M07", "Protection.java", "return force && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0;",
     "return force && state.getDestroySpeed(level, pos) >= 0;", "force replaces block entities"),
    ("M08", "Protection.java", "if (state.isAir()) {", "if (state.isAir() || state.canBeReplaced()) {",
     "replaceable blocks and fluids overwritten without consent"),
    ("M09", "PlacementJob.java", "if (current != step.expected()) {", "if (false) {",
     "undo and redo overwrite blocks changed since"),
    ("M10", "Placements.java", "budget -= job.tick(budget, deadline);", "budget -= job.tick(Integer.MAX_VALUE, deadline);",
     "per-tick block budget ignored"),
    ("M11", "Protection.java", "if (builder != null && !CommonProtection.canPlaceBlock(level, pos, builder, player)) {",
     "if (false) {", "claims ignored"),
    ("M12", "core/History.java", "\t\tundone.clear();\n\t\tif (limit <= 0) {", "\t\tif (limit <= 0) {",
     "a new roof keeps the redo stack"),
    ("M13", "core/FootprintDetector.java", "if ((dx == 0 && dz == 0) || walls.contains(key(x, z))",
     "if ((dx == 0 && dz == 0) || (dx != 0 && dz != 0) || walls.contains(key(x, z))", "walls joined only side by side"),
    ("M14", "RoofwrightCommands.java", "\"roofwright.use\", level(RoofwrightConfig.get().permissionLevels().use())",
     "\"roofwright.use\", level(0)", "everyone may use /roof"),
    ("M15", "Preview.java", "if (targets.size() > limit) {", "if (false) {", "preview cap ignored"),
    ("M16", "core/Settings.java", "clamp(\"blocksPerTick\", blocksPerTick, 1, 100_000, warnings)",
     "clamp(\"blocksPerTick\", blocksPerTick, 0, 100_000, warnings)", "a zero block budget is accepted"),
    ("M17", "core/Pitch.java", "LOW(\"low\", \"1:2\", 1),", "LOW(\"low\", \"1:2\", 2),", "low pitch rises a full block"),
    ("M18", "Materials.java", "f.get(BlockFamily.Variant.WALL)));", "null));", "families lose their wall block"),
    ("M19", "core/RoofPlanner.java", "case SHED -> switch (spec.shedRise()) {\n\t\t\t\t\t\t\tcase NORTH -> area.z1() - z;",
     "case SHED -> switch (spec.shedRise()) {\n\t\t\t\t\t\t\tcase NORTH -> z - area.z0();", "shed rises the wrong way"),
    ("M20", "core/RoofPlanner.java", "ridge[i] = height % 2 == 0 && surfacePiece(grid, top, x, z, height) == null;",
     "ridge[i] = false;", "ridges never capped"),
]


def run_suites():
    """Runs unit tests and GameTests; returns (compiled, failing test names)."""
    for report in (ROOT / "build/test-results/test").glob("*.xml"):
        report.unlink()
    log = subprocess.run(["./gradlew", "test", "runGameTest", "--continue", "--console=plain"], cwd=ROOT,
                         capture_output=True, text=True).stdout
    if "compilation failed" in log.lower() or "Compilation failed" in log:
        return False, []
    failing = []
    for report in (ROOT / "build/test-results/test").glob("*.xml"):
        text = report.read_text()
        cls = re.search(r'<testsuite name="[^"]*\.([^".]+)"', text).group(1)
        for name in re.findall(r'<testcase name="([^"]+)"[^>]*>\s*<failure', text):
            failing.append(f"{cls}.{name}")
    for name in re.findall(r"roofwright-gametest:roofwright_game_tests_(\w+) failed", log):
        failing.append("GameTest " + name)
    return True, sorted(set(failing))


def main():
    wanted = set(sys.argv[1:])
    rows = []
    bad = 0
    for mid, rel, original, mutated, what in MUTANTS:
        if wanted and mid not in wanted:
            continue
        path = MAIN / rel
        source = path.read_text()
        if source.count(original) != 1:
            rows.append((mid, what, "INVALID", f"pattern found {source.count(original)} times in {rel}"))
            bad += 1
            continue
        try:
            path.write_text(source.replace(original, mutated))
            compiled, failing = run_suites()
        finally:
            path.write_text(source)
        if not compiled:
            verdict, detail = "INVALID", "does not compile"
            bad += 1
        elif failing:
            verdict, detail = "KILLED", ", ".join(failing[:4]) + (f" (+{len(failing) - 4} more)" if len(failing) > 4 else "")
        else:
            verdict, detail = "SURVIVED", "no test failed"
            bad += 1
        rows.append((mid, what, verdict, detail))
        print(f"{mid} {verdict}: {what} -> {detail}", flush=True)
    out = ROOT / "build/mutation/report.md"
    out.parent.mkdir(parents=True, exist_ok=True)
    lines = ["| Mutant | Change | Verdict | Killed by |", "| --- | --- | --- | --- |"]
    lines += [f"| {m} | {w} | {v} | {d} |" for m, w, v, d in rows]
    out.write_text("\n".join(lines) + "\n")
    killed = sum(1 for r in rows if r[2] == "KILLED")
    print(f"{killed} of {len(rows)} mutants killed by a named test; report in {out.relative_to(ROOT)}")
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
