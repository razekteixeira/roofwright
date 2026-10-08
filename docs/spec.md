# Roofwright: specification and plan

Agentic SDLC central version `2026-09-16+2`. Gate log with evidence: [sdlc-log.md](sdlc-log.md). Research
behind every decision here: [research.md](research.md).

## G0 Intake ready

| Field | Value |
| --- | --- |
| Outcome case | Builders on Fabric 26.3 (creative players, server builders, map makers) spend most of a build's tedium on roofs: hundreds of stairs and slabs, with hips, valleys and corners easy to get wrong. Roofwright gives them a complete, correct roof over an existing build in one click, with a preview and undo. Evidence of the job is indirect (research.md, Question 1): roof block mods exist because roofs matter, several web roof designers exist, and no published in-game generator exists. |
| Beneficiary | Players who build in creative or on build servers, and server operators who want a safe, permissioned tool. |
| Outcome measure | A roof over a real house in one wand click plus one confirm, with zero stair shapes changed by the game afterwards and zero blocks lost on undo. After release: downloads and issues on CurseForge (owner reviews at 30 days). |
| Stop or pivot rule | The owner's stop rule, read literally, is triggered: roof-thing (unpublished, GitHub only, 2026-10-06) already builds gable and hip roofs on 26.3 (research.md, 2.4). The owner was notified on 2026-10-08 with options A differentiate, B stop, C contribute upstream, and the default A. No explicit reply has been received; the build proceeds under the owner's standing goal instruction and is reversible if the owner picks B or C. |
| Objective | A server-side Fabric mod for Minecraft Java 26.3 that detects a building outline from its wall tops and generates a complete roof in ten styles, with preview, place across ticks, undo and redo, limits, permissions and claim protection. |
| Scope | Everything in the acceptance criteria below, the repository (docs, workflows, scripts), visuals, the GitHub Pages site and the CurseForge texts. |
| Non-goals (v1) | Survival material cost; multi-height wall tops in one roof; courtyards left open; dormers, chimneys and other roof features; client-side mod or rendering; persistence of undo history across restarts; Forge or NeoForge. |
| Sensitive tags | `world-modification` (writes many blocks into a world: data loss risk), `permissions` (who may do it), `config` (operator file). No secrets, network services or personal data. |
| Effort level | L (new product, several subsystems, release preparation). |
| Fast lane | No: sensitive tags present. |
| Governance calibration | Full gate run G0 to G8 with G7 Release prepared (no tag, no upload, by the owner's instruction). |
| Delivery posture | Incremental: pure core first with unit tests, then platform layer with GameTests, then visuals and site. |
| Team | Software delivery composition, solo operator in series (Architect, Implementation, QA, Security, Release) with fresh-context subagents for research slices (R2), claim verification (R3), sceptic (R4) and the G6 Independent review. UX profile joins for the site and in-game messages (`uiSurface: yes`). |
| Lessons applied (from Throughput) | Lenient read and strict write config codecs with clamping and warnings, never overwrite a broken config, `/roof reload`. Bundle fabric-permissions-api 0.7.0 with `PermissionLevel` overloads and resend command trees after reload. Measure the operation itself, warmed up, repeated readings. GameTests start from a fresh world. Mutants must be killed by a named test. Visual check of real captures in review. Hide hand and hotbar in chat shots. No saved data in v1, so the saved-data lessons are recorded as not applicable (history is in memory, documented). |
| Repeatability | Committed scripts for every repeated step: `./gradlew benchmark`, `tools/benchmark.sh` (in-game), `tools/mutation.sh` (G4), `branding/make_*.py`, `branding/render_logo3d.py`, `branding/make_media.sh`. |
| Stop rules (build) | Stop and report if a GameTest shows vanilla changes planned stair shapes and the cause cannot be fixed in the planner; stop before any release tag or CurseForge upload (owner only). |

## G1 Spec ready

### Context and decision

See research.md for sources. The decisions:

1. **Geometry.** Every sloped roof is a step-distance field over the roof area (footprint plus
   overhang). Hip-type roofs (hip, pyramid, mansard) use the Chebyshev distance to the outside, which
   equals the straight skeleton on right-angled outlines. Gable, dutch gable and gambrel roofs split
   the outline into non-overlapping rectangles, largest first: the largest is the main wing, and every
   piece attached by one side is an arm whose ridge runs into the wing it joins and stops at that
   wing's ridge line, so wings meet in valleys (an arm that would rise above the wing it joins keeps
   its ridge along the joint). Sheds, and gables with a forced ridge direction, use every maximal
   rectangle. All wings merge by taking the highest. Cone and dome use the straight-line distance. A
   profile turns
   the step distance into a height in half blocks (pitch 1:2, 1:1, 2:1; gambrel and mansard steep then
   shallow; dome circular). Columns are filled from their lowest neighbour up, with a stair, slab or
   full block on top. Gable ends get wall infill. Rejected: an event-based straight skeleton
   (degenerate events dominate on grid outlines), Euclidean distance for rectilinear outlines (rounds
   valleys), hand-marked rectangles only (roof-thing's model).
2. **Stair shapes.** Facing points uphill; the shape is computed with vanilla's own 26.3 rule over the
   planned blocks, so the plan is what the world keeps. Placement uses `UPDATE_CLIENTS` plus
   `UPDATE_KNOWN_SHAPE`, so neighbours do not cascade.
3. **Interaction.** A wand from a vanilla stick with a `custom_data` marker (vanilla clients see a
   named, glinting item), plus `/roof` commands. The preview uses block display entities sent as packets
   to the builder only: never saved, never seen by others, capped in size.
4. **Safety.** Air only by default; `force` replaces other blocks but never block entities or
   unbreakable blocks. Limits per operation (blocks, span), placement spread over ticks under a time
   and block budget, undo and redo per player that skip blocks someone changed since. Permission nodes
   with vanilla level fallbacks. Claims via Common Protection API (bundled), plus vanilla checks
   (`mayInteract`, world border, build height, adventure mode).
5. **Materials.** Vanilla block families (`BlockFamilies` in the common jar) give stairs, slab, full
   block and wall from any one of them; any modded block with stair properties works as the stair
   (Macaw's Roofs included), with the slab and full block found by name or given explicitly. Gable
   infill matches the clicked wall block by default.

### Acceptance criteria, each with its discriminating test

| AC | Criterion | Test(s) that fail when it breaks |
| --- | --- | --- |
| AC1 | One click on a wall top finds rectangle, L and other right-angled outlines, closes diagonal corners, roofs courtyards, keeps separate buildings apart, reports gaps and oversize walls. | `FootprintDetectorTest` (8 cases); GameTest `detectsAnEllFromRealWalls`. |
| AC2 | Hip roofs equal the Chebyshev distance field; hip equals the highest of wing hips; outer corners on hips, inner corners on valleys. | `RoofPlannerTest.hipFieldIsTheChebyshevDistanceToTheOutside`, `hipEqualsTheHighestOfTheWingHips`, `hipOnRectangleRisesOneBlockPerRingWithOuterCornersOnTheHips`, `ellShapedHipHasAValleyOfInnerCorners`. |
| AC3 | Gable roofs: straight rakes, triangular gable walls of the right height, ridge along the long side or forced axis, even widths back to back; a bump gets a small cross gable that leaves the far side alone; an L arm ends in a valley, not a second gable at the corner; arms never rise above the wing they join. | `gableHasStraightRakesAndATriangularGableWall`, `gableRidgeFollowsTheLongSideUnlessAnAxisIsForced`, `evenWidthGableMeetsBackToBackWithoutARidgeCap`, `fullBlockRidgeWhenSlabRidgeIsOff`, `aBumpGetsASmallCrossGableThatStopsAtTheMainRidge`, `ellArmEndsInAValleyInsteadOfASecondGableAtTheCorner`, `armsNeverRiseAboveTheWingTheyJoin`. |
| AC4 | Pitch and overhang: 1:2 is slabs only, 2:1 has a full block under each stair, the wall line always starts just above the wall top, overhang reaches exactly its distance. | `lowPitchIsAllSlabsAndRisesHalfABlockPerColumn`, `steepPitchPutsAFullBlockUnderEveryStair`, `overhangHangsBesideTheWallTop`. |
| AC5 | Every style behaves as documented (dutch gablet, shed direction, gambrel and mansard break, cone and dome round, pyramid note, flat deck and parapet). | `dutchGableHasHippedEndsAndAGabletFace`, `shedRisesTowardsTheChosenSide`, `gambrelAndMansardAreSteepBelowAndShallowAbove`, `coneIsRoundAndPeaksInTheMiddle`, `pyramidOnARectangleExplainsItBuildsAHip`, `flatRoofHasADeckAtWallTopAndAParapetOnTheWalls`. |
| AC6 | Every roof is watertight and covers every footprint column, for all styles, seven outlines, three pitches and three overhangs. | `everyRoofIsWatertight`, `everyFootprintColumnIsCoveredAndNothingIsBelowTheEaves`. |
| AC7 | Every stair keeps its shape in a real world: the planned shape equals what vanilla computes. | `everyStairHasTheShapeVanillaWouldGiveIt`, `StairShapesTest`; GameTest `placedStairsKeepTheirShapes` (compares every placed stair with `Block.updateFromNeighbourShapes`). |
| AC8 | Non-air blocks are never replaced without consent; `force` never replaces block entities or unbreakable blocks. | GameTests `airOnlyByDefault`, `forceReplacesOnlyPlainBlocks`. |
| AC9 | Undo restores exactly what was there and skips blocks changed since; redo reapplies the same way; a new roof clears redo. | GameTests `undoRestoresAndSkipsEditedBlocks`, `redoReappliesAndNewRoofClearsRedo`. |
| AC10 | Limits: too many blocks or too wide refused before anything is placed; placement never exceeds the per-tick block budget and spreads over ticks. | GameTests `oversizedRoofIsRefused`, `placementSpreadsAcrossTicks`. |
| AC11 | Protection: a claim (Common Protection API provider) keeps its blocks; adventure-mode players cannot build roofs. | GameTests `claimedBlocksAreSkipped`, `adventureModeCannotBuild`. |
| AC12 | Permissions: players below the configured level cannot use `/roof`; `force` needs its own node; reload resends command trees. | GameTests `commandsNeedPermission`, `forceNeedsItsOwnPermission`. |
| AC13 | Materials: vanilla families resolve stairs, slab, full block, wall; any stair-like block works; non-building blocks are refused with a message. | GameTest `materialsResolveFromAnyFamilyMember`. |
| AC14 | Preview: only the builder receives it, packet entities (none added to the world), capped at the limit, blocked blocks highlighted, cleared on place and cancel. | GameTest `previewIsPacketOnlyCappedAndMarksBlocked`. |
| AC15 | Wand: a stick with the Roofwright marker is the wand, a plain stick is not. | GameTest `wandIsRecognisedByItsMarker`. |
| AC16 | Config: missing fields default, out-of-range values clamp with warnings, a broken file is never overwritten, defaults are written in full. | `RoofwrightSettingsTest`, `RoofwrightConfigTest`. |
| AC17 | Performance: planning a 256 x 256 hip roof is fast enough to run on the server thread (target under 50 ms warmed), and placement stays within the per-tick time budget. | `./gradlew benchmark` and `tools/benchmark.sh` readings recorded in sdlc-log.md. |
| AC18 | Vanilla clients: works with no client mod (server-only registration, vanilla items and entities only). | Dedicated-server GameTests; client capture uses only vanilla rendering. |

### Scope boundary

In: the mod, its tests and harnesses, docs, visuals, site, CurseForge texts, CI. Out: release tag,
CurseForge upload, other repositories (owner only).

## G2 Plan ready

| Phase | Deliverable | Gate evidence |
| --- | --- | --- |
| P1 | Research report, ledger, sceptic pass, independent review | G4/G5/G6 research gates in sdlc-log.md |
| P2 | Pure core: planner, detector, shapes, with unit tests | `./gradlew test` green |
| P3 | Platform: config, materials, sessions, wand, commands, preview, placement, undo, protection, permissions | `./gradlew build` with GameTests green |
| P4 | Benchmarks (planner and in-game placement) | readings in sdlc-log.md |
| P5 | G4 Tests with teeth: mutation harness with named killers | `tools/mutation.sh` log |
| P6 | G5 Security clean and G6 Independent review, findings fixed and re-verified | sdlc-log.md |
| P7 | Visuals: icon, 3D logo, client captures, GIF, banners, social preview | committed scripts and outputs |
| P8 | Docs, site, CurseForge texts, workflows; push, CI green, Pages live | links and run ids in sdlc-log.md |
| P9 | G7 release preparation (no tag) and G8 lessons | sdlc-log.md |
