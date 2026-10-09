# Roofwright: gate log

Agentic SDLC central version `2026-09-16+2`. Specification: [spec.md](spec.md). Research:
[research.md](research.md). Every entry names its evidence; counts are quoted from real runs.

## Research item (Deep research team)

| Gate | Status | Evidence |
| --- | --- | --- |
| G0 Intake ready | passed 2026-10-08 | Questions 1 to 6 from the owner's brief locked as acceptance criteria. Tags: `decision-support` (the build decision). Effort M (breadth 6 sub-topics, low nesting, medium exploration). Budget: one wave of four R2 Search slices, about 40 tool calls each. |
| G1 Research brief ready / G2 Plan ready | passed 2026-10-08 | Four non-overlapping slices: S1 editors and plugins, S2 mod catalogues and name, S3 geometry and algorithms, S4 platform and safety. Saturation rule: stop after the wave if no gap is load-bearing. |
| G3 Draft report ready | passed 2026-10-08 | Evidence packs S1 (25 claims), S2 (32), S3 (35), S4 (37), plus a lead-verified addendum on roof-thing (GitHub API). Single writer (R5) drafted research.md from the packs only; every citation id resolves in References. |
| G4 Claims with teeth | passed after fixes 2026-10-08 | [research-ledger.md](research-ledger.md): R3 re-fetched the cited sources for 57 load-bearing claims without reading the packs: 40 supported, 14 partial, 1 unsupported, 1 contradicted. All 17 required fixes applied by R5 (R6 confirmed). The contradicted claim (Macaw's roof blocks "not stairs") led to a design decision and a GameTest. |
| G5 Sceptic clean | passed after fixes 2026-10-08 | [research-sceptic.md](research-sceptic.md): 19 disconfirming searches, 12 findings (5 major). Fixed: literal stop rule (F1), MCTools added (F2), roof-thing comparison corrected (F3), gable composition redesigned as main wing plus arms with property tests (F4), preview bundle cap (F6), claims after logoff (F8). No injection found. |
| G6 Independent review | passed with fixes 2026-10-08 | [research-review.md](research-review.md): fresh-context R6 spot-checked 22 claims (21 supported, 1 partial). Findings M1 (owner notification evidence), M2 (air-only default) and m3 to m8 applied; M2 is also a GameTest (`airOnlyByDefault`: stone, water, short grass left alone). |
| G7 Report published | passed 2026-10-08 | research.md committed with ledger, sceptic and review annexes. One later in-game confirmation added to section 5: GameTest `macawsRoofBlocksWorkAsStairs` (Macaw's Roofs 2.3.2 on the GameTest classpath only). |

### Owner notifications (stop rule)

The stop rule is triggered on a literal reading (roof-thing builds gable and hip roofs on 26.3, GitHub only).
Push notifications sent to the owner:

1. 2026-10-08, about 20:10 WEST: "Roofwright stop rule: only near-equal is an unpublished 2-day-old GitHub
   mod (3 styles, manual rects). Continuing with 'differentiate' unless you say stop."
2. 2026-10-08, 20:37 WEST: "Roofwright decision: roof-thing (GitHub, unpublished) triggers your stop rule.
   A=differentiate (doing this), B=stop, C=contribute to it. Reply B or C to change."

No reply as of the last update of this log; the build proceeds on option A under the owner's standing goal
instruction and is reversible if the owner chooses B or C.

## Delivery item

| Gate | Status | Evidence |
| --- | --- | --- |
| G0 Intake ready | passed 2026-10-08 | spec.md, G0 table: outcome case, tags `world-modification`, `permissions`, `config`; effort L; no fast lane; lessons from Throughput applied. Stop rule outcome reported to the owner (push notification) with the default "differentiate". |
| G1 Spec ready | passed 2026-10-08 | spec.md: decisions with rejected alternatives, AC1 to AC18 each mapped to a named test. |
| G2 Plan ready | passed 2026-10-08 | spec.md: phases P1 to P9. |
| G3 Build green | passed 2026-10-08 | `./gradlew build`: compile, 78 unit tests (JUnit), 17 server GameTests on a headless 26.3 server (`All 17 required tests passed`), jar with fabric-permissions-api 0.7.0 and Common Protection API 2.0.0 bundled. Client capture GameTest green (`runClientGameTest`, 40 screenshots). Planner benchmark: 256 x 256 hip median 16.77 ms, max 23.11 ms (15 runs after warm-up). In-game placement benchmark (`tools/benchmark.sh`): 16,900-block hip roof, 9 ticks, worst tick per run median 2.05 ms, max 2.16 ms (5 runs after a warm-up run), 2,000 blocks per tick. |
| G4 Tests with teeth | passed 2026-10-09 | [mutation-report.md](mutation-report.md): `tools/mutation.py` planted 20 mutants (geometry, stair shapes, fill, gable walls, arms, force, air-only, undo conflicts, tick budget, claims, history, detection, permissions, preview cap, config clamp, pitch, materials, shed, ridge caps). First run: 19 of 20 killed; M13 (walls joined only side by side) survived because `wallsTouchingOnlyAtCornersStillClose` removed one corner only and the ring stayed connected the long way. The test now removes all four corners; rerun: 20 of 20 killed, each by a test named in the log, none by a compile error. |
| G5 Security clean | passed with fixes 2026-10-09 | [security-review.md](security-review.md): fresh-context Security Agent found H1 (a far `/roof select` exhausts the heap), M1 (unsafe materials such as End portals, containers and fluids), M2 (no wand cooldown), L1 to L9 and I1 to I4; verdict "not clean". Resumed session (the previous one was lost): H1, M1, M2, L1, L2, L3, L7, L8, L9, I1, I2 fixed, L6 partly; L4, L5, I3 accepted (reasons in the resolution table). Six new GameTests, each failing without its fix: mutants M21 to M30 planted with `tools/mutation.py`, 10 of 10 killed, each by the test written for it (M22 by `FootprintTest`, the rest by their GameTests). `./gradlew build`: 87 unit tests, `All 23 required tests passed`, on Loom 1.18.3 (pinned from the snapshot). `actionlint` clean on the split release workflow; `./gradlew publishCurseforge` without a token prints its dry run. The committed `.pyc` and the local path quoted in the review were removed from every commit before the first push. |
