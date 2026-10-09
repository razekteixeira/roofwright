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
| G1 Spec ready | passed 2026-10-08 | spec.md: decisions with rejected alternatives, AC1 to AC18 each mapped to a named test (AC19 to AC24 added at G5). |
| G2 Plan ready | passed 2026-10-08 | spec.md: phases P1 to P9. |
| G3 Build green | passed 2026-10-08 | `./gradlew build`: compile, 78 unit tests (JUnit), 17 server GameTests on a headless 26.3 server (`All 17 required tests passed`), jar with fabric-permissions-api 0.7.0 and Common Protection API 2.0.0 bundled. Client capture GameTest green (`runClientGameTest`, 40 screenshots). Planner benchmark: 256 x 256 hip median 16.77 ms, max 23.11 ms (15 runs after warm-up). In-game placement benchmark (`tools/benchmark.sh`): 16,900-block hip roof, 9 ticks, worst tick per run median 2.05 ms, max 2.16 ms (5 runs after a warm-up run), 2,000 blocks per tick. |
| G4 Tests with teeth | passed 2026-10-09 | [mutation-report.md](mutation-report.md): `tools/mutation.py` planted 20 mutants (geometry, stair shapes, fill, gable walls, arms, force, air-only, undo conflicts, tick budget, claims, history, detection, permissions, preview cap, config clamp, pitch, materials, shed, ridge caps). First run: 19 of 20 killed; M13 (walls joined only side by side) survived because `wallsTouchingOnlyAtCornersStillClose` removed one corner only and the ring stayed connected the long way. The test now removes all four corners; rerun: 20 of 20 killed, each by a test named in the log, none by a compile error. |
| G4 Tests with teeth (rerun) | passed 2026-10-09 | After the G5 fixes, all 30 mutants rerun on a clean export of the commit: 30 of 30 killed, each by a named test ([mutation-report.md](mutation-report.md)), none by a compile error. |
| G5 Security clean | passed with fixes 2026-10-09 | [security-review.md](security-review.md): fresh-context Security Agent found H1 (a far `/roof select` exhausts the heap), M1 (unsafe materials such as End portals, containers and fluids), M2 (no wand cooldown), L1 to L9 and I1 to I4; verdict "not clean". Resumed session (the previous one was lost): H1, M1, M2, L1, L2, L3, L7, L8, L9, I1, I2 fixed, L6 partly; L4, L5, I3 accepted (reasons in the resolution table). Six new GameTests, each failing without its fix: mutants M21 to M30 planted with `tools/mutation.py`, 10 of 10 killed, each by the test written for it (M22 by `FootprintTest`, the rest by their GameTests). `./gradlew build`: 81 unit tests in 6 classes (JUnit XML), `All 23 required tests passed`, on Loom 1.18.3 (pinned from the snapshot). `actionlint` clean on the split release workflow; `./gradlew publishCurseforge` without a token prints its dry run. The committed `.pyc` and the local path quoted in the review were removed from every commit before the first push. |
| G6 Independent review | passed with fixes 2026-10-09 | [independent-review.md](independent-review.md). Part A, outsider (I3, Codex CLI, different model family, read-only, canonical input only): not approved, F1 to F8 (replay skipped world checks, settling loaded chunks and ignored the budget, wrong corners beside skipped blocks, history moved before replay, materials not rechecked, blank CurseForge token, cross-dimension previews). Part B, docs and claims (I2, fresh Sonnet subagent): approved with fixes, B-F1 to B-F21. All fixed or explained; fixing also found nondeterministic block-family lookup (stone bricks could build as stone) and, through the placement benchmark, unbudgeted settle scans. Part C, outsider re-verification: not approved, N1 (settle without a rights check), N2 (unbudgeted scan), F5 remainder (blocked undo dropped from history), N3 (split entries over `historySize`); all fixed. Final outsider check: **approved with fixes D1 (this log) and D2 (redo must recheck materials)**; D2 fixed with a test and mutant M42, D1 by this entry. The final check was asked to list test-coverage wishes as recorded gaps rather than blockers. Recorded gaps for a later item: scheduler deadline with several owners, cone and dome roundness oracle, flat roofs in the watertight matrix, reload command-tree resend, preview recipients and removal, no benchmark threshold, an unmodified client on a dedicated server, modded stair state variants, the 2,000,000 ceiling, late break-right, border and game-mode changes before settling, blocked or cancelled redo. Accepted: settling skips blocks whose neighbourhood is unloaded; redo restores journal states without settling; a queued job keeps the states planned for it. |
| G3 Build green (final) | passed 2026-10-09 | `./gradlew clean build`: 85 unit tests in 6 classes, 0 failures (JUnit XML); `All 33 required tests passed` (32 Roofwright GameTests plus vanilla's built-in `minecraft:always_pass`), stable over repeated runs. Client capture: `runClientGameTest` green, 40 screenshots, media and banners regenerated from that run. Planner (`./gradlew benchmark`, 15 runs after warm-up): hip 64 x 64 median 1.42 ms, 128 x 128 3.80 ms, 256 x 256 17.57 ms (max 24.83). Placement (`tools/benchmark.sh`, 5 runs after a warm-up): 16,900-block hip roof in 17 ticks, worst tick 5.00 ms in every run, the `millisPerTick` budget; before the settle rights recheck it was 3.60 ms median, and before charging settle checks to the budget 5.40 ms median (7.43 max), which is how that defect was found. Loom 1.18.3, no deprecation warnings in main code. |
| G4 Tests with teeth (final) | passed 2026-10-09 | [mutation-report.md](mutation-report.md): 42 of 42 mutants killed by named tests, none by a compile error. M01 to M39 on a clean export of `4841a00`; M40 to M42 on the final code. M41 survived once (its test never overflowed the redo stack); the test was rewritten and kills it. M20 replaced after the review found its kill was a crash. |
| G7 Release preparation | passed 2026-10-09, no tag | Version `1.0.0-beta.1` in `gradle.properties` and the jar's `fabric.mod.json`; CHANGELOG section `[1.0.0-beta.1] - 2026-10-09` (the release workflow extracts it). Jar checked: `data/roofwright/tags/block/forbidden.json`, nested `common-protection-api-2.0.0.jar` and `fabric-permissions-api-0.7.0.jar`, no GameTest classes, no Macaw's Roofs; THIRD_PARTY_NOTICES matches. `./gradlew publishCurseforge` prints a dry run with no token, an empty token, a blank token (with a project id) and with `-PreleaseJar` (no rebuild). Release workflow split into build (read-only), github-release (write, no build) and curseforge (read-only, token in one step), actions pinned by SHA, `actionlint` clean. CurseForge listing and description in `branding/`. Per the brief, no tag was pushed and nothing was uploaded; the owner tags when ready (CONTRIBUTING, "Releases"). |
| P8 Publish | done 2026-10-09 | Pushed `roofwright-build` to `main` (first push). CI `build` runs 37873374187 and 37873630495: success, `All 33 required tests passed` in the GitHub log. `pages` run 37873630537: success; https://razekteixeira.github.io/roofwright/ returns 200 and all 42 referenced assets (media, WebP, CSS, icons, social preview) return 200. Site rendered with headless Chrome at 1440 px and in a 390 px iframe before the push: one column on phones, no overflow. |

## G8 Learning captured (2026-10-09)

1. **A lost session cost minutes, not hours, because the gate log was current.** The resumed session read
   this log and `git log` and continued at G5. Keep evidence in the log as each gate closes, and keep a
   memory note that says where the work lives (here: an orphan worktree in an unborn repository).
2. **The outsider review found what the same-family reviews did not.** Codex (I3) found eight medium
   defects after the Security Agent and the build had passed; its re-verification found three more,
   two in the fixes themselves. For sensitive surfaces, run the I3 outsider and re-verify after fixing.
3. **Benchmarks are tests.** Charging settle checks to the budget was only noticed because the placement
   benchmark showed 5.4 ms ticks against a 5 ms budget. Re-run it after every change to placement, and
   give it a failing threshold (recorded gap).
4. **A flaky test is a bug until proven otherwise.** `settlingStaysWithinTheBlockBudget` failed in two of
   three runs; the cause was `BlockFamilies` iteration order, which made `stone_bricks` resolve to the
   stone family. Rank explicitly whenever a registry or map is searched for "the first match".
5. **Mutation results need reading, not counting.** A kill by a crash (old M20) proves nothing, and a
   survivor (M41) exposed a test that never reached the state it claimed to test.
6. **`./gradlew clean` deletes the client captures.** Generate site media in the same pass as the capture
   run, or never clean between capture and media.
7. **Look at the pictures.** The banner's bold pixel "W" read as a heart; only the visual check found it.
8. **GameTest server details:** operators default to level 0 (op mock players with an explicit level);
   vanilla adds `minecraft:always_pass` to the required count; out-of-height positions report "not loaded".
9. **GitHub:** a push that creates the default branch runs only workflows whose path filters match;
   `workflow_dispatch` returns 404 until the workflow is registered.
