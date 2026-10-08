# Roofwright: research review (R6)

Gate: G6 Independent review (research track). Reviewer: R6 Research Reviewer, fresh context,
2026-10-08. Inputs: `docs/research.md` (647 lines, revised R5 report), `docs/research-ledger.md` (R3),
`docs/research-sceptic.md` (R4), `docs/sdlc-log.md`, the owner's brief, and live re-fetches of the cited
sources (Modrinth, CurseForge via api.curse.tools, GitHub API and raw files, Mojang piston-meta, Fabric
meta, Nucleoid maven, minecraft.wiki API, Wikipedia, mojira.dev, arctic-shift) plus `javap` on the local
`minecraft-common-deobf-26.3.jar`. Fetched content was treated as data only; no injection attempts
were seen.

## Verdict

**Pass with fixes.** All six research questions and the stop rule are answered with sources; every one
of the 17 R3 required fixes and all 12 R4 findings were applied (one, R3 fix 5, was deliberately
superseded by a labelled design decision). Every citation id in the body resolves in References and
every reference is cited. All 22 spot-checks below came back supported except one (Arnis roof types,
understated). Style rules hold: British English, no em or en dashes, a Limitations section. Two medium
findings (the stop-rule record in the gate log, and the overwrite default against the brief's "never
overwrite non-air without consent") must be fixed or explicitly accepted by the owner before the spec
is treated as final; the rest are minor.

## Coverage per question

| # | Question | Answered? | Evidence quality |
|---|---|---|---|
| 1 | Demand and gaps: WorldEdit/FAWE, Axiom, VoxelSniper, Bukkit plugins, Arceon, Macaw's Roofs, Fabric builder tools; is there a real generator; name free | Yes (2.1 to 2.4) | Strong. Catalogue searches on Modrinth, CurseForge (mods, plugins, data packs), Spiget, Hangar, Polymart and GitHub, plus code reads of roof-thing and MCTools. Unchecked sources (BuiltByBit 403, Discord, paid Arceon) are stated. Demand honestly reported as unmeasured. Name check re-verified. |
| 2 | Roof architecture: all ten styles, pitch on the grid, eaves, overhang, ridge, corners, stair facing and shape | Yes (3.1 to 3.3) | Strong for stairs: the 26.3 `getStairsShape` rule is read from bytecode and re-checked. Style definitions rest on Wikipedia (secondary) and the Minecraft wiki, adequate for definitions. Cone, dome and flat with parapet are covered thinly (finding m5). |
| 3 | Algorithms: rectangles, L/T/U, arbitrary rectilinear; skeleton or decomposition; footprint detection | Yes (4.1 to 4.5) | Good. Chebyshev hip result correctly labelled as author inference with the 3D paper as support, and backed by the R4 simulation (300 of 300). Gable composition is a labelled design decision with named property tests; some cases are not yet specified (m6). Dutch gable and gambrel rules are promised but not given (m5). |
| 4 | Interaction for vanilla clients: wand from a vanilla item with components, commands, display-entity preview, block families, Macaw's Roofs | Yes (5) | Strong. Components, private setters, bundle limit and `BlockFamilies` verified against the 26.3 jar. Packet-only ghost obligations listed. Macaw's handling is a labelled design decision with its risk stated (m8). |
| 5 | Safety and scale: limits, placement across ticks, undo and redo, permission nodes, protection mods, no overwrite of non-air without consent | Mostly (6.1 to 6.6) | Strong on limits, history, permissions and CPA (all re-verified). The consent rule is not met as written: the default replaces "replaceable" blocks, which are non-air (M2). |
| 6 | Performance plan and benchmark for big footprints | Yes (7) | Adequate for a research stage: planner and placement benchmarks with warm-up, repeated readings, median and p95, mod-side per-tick logging instead of `/tick query`, and a client preview benchmark. Targets correctly labelled as inference until measured. |
| Stop rule | Report to the owner with options if an existing tool already generates proper roofs on current versions | Yes (2.4) | The report now reads the rule literally, says it is triggered by roof-thing, lists options A, B and C, and states that no owner reply has been received. The gate log does not yet show the options or the trigger (M1). |

## Required fixes and sceptic findings: applied?

| Item | Applied? | Where |
|---|---|---|
| R3 fixes 1, 2 (Arceon price and versions) | Yes | 2.1 Arceon row |
| R3 fix 3 (633 blockstate files) | Yes | 2.1, contradiction 3 |
| R3 fix 4 (Macaw's downloads, CurseForge vs Modrinth) | Yes | 2.2 |
| R3 fix 5 (Macaw's roof blocks carry stair properties) | Superseded by a labelled design decision (accept any block with stair properties); see m8 | 5 Materials |
| R3 fix 6 (Pugtools wording) | Yes | 2.1 Pugtools row |
| R3 fixes 7, 8 (roof-thing preview, overwrite) | Yes | 2.1, 6.6 |
| R3 fix 9 (llmbuilder, "only roof-thing" wrong) | Yes in 2.1 and contradiction 5; not mentioned in the 2.4 stop-rule assessment (m7) | 2.1, 8 |
| R3 fixes 10 to 16 | Yes | 2.2, 5, 6.1, 6.4, 4.1, 4.2, 3.1 |
| R3 fix 17 (as-of dates) | Yes | 1, 2.1, 2.2, 2.3, 5 |
| R4 F1 (stop rule literal) | Yes in the report; gate-log evidence incomplete (M1) | 1, 2.4, 8 |
| R4 F2 (MCTools) | Yes | 2.1, 2.4 table stakes |
| R4 F3 (roof-thing comparison) | Yes; differentiator 3 still overstated (m4) | 2.4 |
| R4 F4 (gable composition) | Yes, redesigned; simplification and some cases left unaddressed (m6) | 4.3 |
| R4 F5 (Chebyshev scope) | Yes | 4.2 |
| R4 F6 (bundle limit, ghost obligations) | Yes | 5 Preview |
| R4 F7 (game rule default) | Yes | 6.1 |
| R4 F8 (CPA linkage, offline placement) | Yes | 6.5 |
| R4 F9 (wand grants nothing) | Yes | 5 Wand |
| R4 F10 (absolute wording) | Yes | 1 |
| R4 F11 (Arnis, Effortless Structure, dynamic_framing) | Yes; Arnis understated (m3) | 2.1, 4.5 |
| R4 F12 (demand heading) | Yes | 1, 2.2 |

## Spot-checks (re-fetched by R6 on 2026-10-08)

| # | Claim in research.md | Source checked | Verdict |
|---|---|---|---|
| 1 | Name free: Modrinth `roofwright`, `roof-wright`, `roofwrights` 404; CurseForge slug 0 for all three, control `macaws-roofs` 1; text search 0 (2.3) | https://api.modrinth.com/v2/project/roofwright ; https://api.curse.tools/v1/cf/mods/search?gameId=432&slug=roofwright | Supported (404 x3; 0, 0, 0, control 1; text 0) |
| 2 | roof-thing created 2026-10-06, 0 stars, 0 forks, description "e", CC0-1.0, not on Modrinth (2.1) | https://api.github.com/repos/howdoiusethissite/roof-thing ; https://api.modrinth.com/v2/project/roof-thing | Supported (created 00:01:19Z, pushed 00:21:54Z, 0/0, "e", CC0-1.0; Modrinth 404) |
| 3 | roof-thing README: styles gable, gable_rotated, hip; odd widths slab ridge; overhang 0 to 3; undo last 5 with materials; gable walls not filled; per-section heights; survival cost; test against vanilla stair shapes (2.1) | https://github.com/howdoiusethissite/roof-thing (README.md) | Supported, near verbatim |
| 4 | roof-thing code: `level.mayInteract(player, pos)`, air or `canBeReplaced`, undo skips changed blocks, custom `roof_wand` item with recipe (2.1, 2.4) | RoofService.java lines 162 to 163, 238; `data/roofthing/recipe/roof_wand.json` | Supported (`MAX_UNDO = 5`, "Only touch blocks that are still exactly what we placed") |
| 5 | MCTools `/mct roof <block> <width> <length> <pitch> <style>`, peaked, hip, flat with parapet, hollow variant; built against paper-api 1.21.4 (2.1) | https://github.com/PenguinStudiosOrganization/MCTools (`shapes/Roof.java`, `pom.xml`, `plugin.yml`) | Supported (`api-version: '1.20'`, `1.21.4-R0.1-SNAPSHOT`) |
| 6 | Arnis applies six roof types (gabled, hipped, half-hipped, mansard, skillion, pyramidal) (2.1, 4.5) | https://github.com/louis-e/arnis `src/element_processing/buildings.rs` | **Partial**: `RoofType` also has Gambrel, Round, Dome, Cone, Onion and Flat (12 variants). Licence Apache-2.0 and 18,184 stars confirmed (m3) |
| 7 | Macaw's Roofs Modrinth 2.3.2 ships `mcw-roofs-2.3.3-mc26.3fabric.jar`, 2026-09-29; 104.6M CurseForge, 8.0M Modrinth downloads (2.1, 2.2) | https://api.modrinth.com/v2/project/macaws-roofs/version?loaders=["fabric"] ; https://api.curse.tools/v1/cf/mods/search?gameId=432&slug=macaws-roofs | Supported (104,631,282; 7,976,771) |
| 8 | Axiom 6.1.3 for 26.3, 2026-09-25, Fabric; changelogs never mention roof or gable (2.1) | https://api.modrinth.com/v2/project/axiom/version | Supported (251 versions, 0 changelog matches) |
| 9 | WorldEdit 7.4.6-beta-02 for 26.3, 2026-09-24 (2.1) | https://api.modrinth.com/v2/project/worldedit/version | Supported (Fabric and NeoForge, and Bukkit family) |
| 10 | FAWE 2.16.0 released 2026-10-04; VoxelSniper Reimagined last pushed 2024-09-30 (2.1) | GitHub API releases/latest; repos/KevinDaGame/VoxelSniper-Reimagined | Supported |
| 11 | Arceon `//roof <block> <width> [height]`, convex and cuboid selections, noise, bevel, hollow, slab flags; no stairs or hips (2.1) | https://arceon.gitbook.io/arceon-wiki/tools/roof-tool | Supported (no "stair" or "hip" on the page) |
| 12 | Pugtools: 16 styles, rectangle or L footprint, pitch shown as 1:1, per-side overhang, 3D preview, Litematica export (2.1) | https://pugtools.com/tools/roof-designer/ | Supported (meta description "16 real roof and arch styles"; Front, Back, Left, Right overhang) |
| 13 | CraftShape FAQ says to split irregular buildings into rectangles (2.1) | https://usecraftshape.com/minecraft-roof-generator | Supported as a paraphrase ("Split the building into rectangles, generate a roof for each, and join them at the ridges") |
| 14 | Effortless Structure has Slope Floor and Pyramid, Cone, Dome shape modes, no roof mention (2.1) | https://api.curse.tools/v1/cf/mods/1557540/description | Supported |
| 15 | r/feedthebeast 1mtk8f6, 2025-08-18, score 52, 15 comments, about roof-block undersides, no generator request (2.2) | https://arctic-shift.photon-reddit.com/api/posts/ids?ids=1mtk8f6 | Supported |
| 16 | Minecraft wiki: gable "not suitable ... greater than about 12 blocks"; mansard example about 16x20, buildings "need to be quite large"; helm "very fiddly"; 1 in 1 and 2 in 1 pitch (3.1, 3.2) | https://minecraft.wiki/w/Tutorial:Roof_types (via api.php) | Supported |
| 17 | Straight skeleton as "the set of ridge lines of a building roof"; Felkel and Obdrzalek "has been shown ... incorrect" (4.1) | https://en.wikipedia.org/wiki/Straight_skeleton | Supported |
| 18 | MC-276285 open, 4,096 item displays, doubling made MSPT and FPS unplayable, affects 1.21.4 (5) | https://mojira.dev/MC-276285 (mirror of the cited tracker) | Supported |
| 19 | 26.3 released 2026-09-15, Java 25, no mappings download; Fabric loader 0.19.5 stable; Fabric API 0.162.0+26.3 (5) | https://piston-meta.mojang.com/mc/game/version_manifest_v2.json ; https://meta.fabricmc.net/v2/versions/loader ; Modrinth fabric-api versions | Supported (2026-09-15T11:23:02Z, majorVersion 25, downloads client and server only) |
| 20 | Bytecode: `max_block_modifications` default 32768, minimum 1; `BUNDLE_SIZE_LIMIT = 4096`; update flags 1, 2, 16, 32; display setters private; `BlockFamilies` and `BlockFamily$Variant` present; `getStairsShape` outer-then-inner rule (3.3, 5, 6.1, 6.2) | `javap` on local `minecraft-common-deobf-26.3.jar` | Supported |
| 21 | fabric-permissions-api 0.7.0 for 26.1 to 26.3, 15,050 bytes; CPA only 1.0.0 and 2.0.0, last updated 2026-03-23; `canPlaceBlock(Level, BlockPos, NameAndId, @Nullable Player)` (6.4, 6.5) | Modrinth fabric-permissions-api versions; https://maven.nucleoid.xyz/eu/pb4/common-protection-api/maven-metadata.xml ; CommonProtection.java | Supported |
| 22 | WorldEdit history 15 steps; FAWE 50,000,000 changes, 1 concurrent action, 7-day history (6.1, 6.3) | WorldEdit 7.4.x LocalSession.java; FAWE Settings.java | Supported (`MAX_HISTORY_SIZE = 15`; `MAX_CHANGES = 50000000`, `MAX_ACTIONS = 1`, `DELETE_AFTER_DAYS = 7`) |

## Findings

| Id | Severity | Location | Finding | Fix |
|---|---|---|---|---|
| M1 | Medium | Sections 1 and 2.4, reference LOG-1; `docs/sdlc-log.md` line 18 | The report says the owner "was notified on 2026-10-08 with options A, B and C". The cited gate log only says "Stop rule outcome reported to the owner (push notification) with the default 'differentiate'"; it names no options, does not say the rule was triggered on a literal reading, and records nothing after research G3 (no G4 claim ledger, G5 sceptic or G6 review rows). R4 F1 asked for the owner's explicit choice, or an explicit "proceeding on default" he can see, before G3 Build green. As it stands the citation overstates its source. | Append to `docs/sdlc-log.md`: research-track rows for G4 (ledger, 57 rows, 17 fixes), G5 (sceptic, 12 findings) and G6 (this review); a delivery-track entry quoting the notification text with options A, B and C, the literal-trigger statement and the time sent, and "no reply as of <time>; proceeding on default A under the standing goal instruction, reversible". If the notification did not list the options, send one that does and log it. |
| M2 | Medium | 6.6 (and 2.4 "Table stakes") | The brief requires "never overwriting non-air without consent". The design places "only into air and replaceable blocks" by default. Replaceable blocks are non-air: short grass, snow layers, flowers, vines, dead bushes and, in vanilla, fluids (water and lava report `canBeReplaced`). The default therefore overwrites non-air blocks, including water, without consent. | Either make the default air only (plus cave air and void air) and put replaceable blocks behind an explicit consent flag shown in the preview, or keep a short, documented allow list (for example vegetation and snow layers, never fluids) and record it as a design decision the owner accepts. Add a GameTest that a roof over water or a fluid source leaves it unchanged without consent. |
| m3 | Minor | 2.1 "Building generators" row; 4.5 | Arnis is described as applying six roof types. Its `RoofType` enum also has Gambrel, Round, Dome, Cone, Onion and Flat (12 variants), which overlaps Roofwright's gambrel, cone, dome and flat styles. It does not change the stop rule (a standalone OpenStreetMap world generator, not a tool for a player's build), but it is the best Apache-2.0 reference for those profiles. | List the actual variants and note Arnis as a reference implementation for gambrel, cone and dome profiles (licence compatible; credit in THIRD_PARTY_NOTICES only if code is reused). |
| m4 | Minor | 2.4 differentiator 3 | "Any rectilinear footprint (L, T, U, arbitrary), versus marked rectangles" overstates the gap. roof-thing's README: "For a wing or an annex, mark another pair of corners. Every section can have its own height and its own style", and sections merge into valleys, so L, T and U are reachable by hand. The real difference is automatic decomposition of the detected outline, which overlaps differentiator 2. | Reword: "automatic decomposition of any rectilinear outline, including noisy ones, versus manually marking each rectangle". |
| m5 | Minor | 4.2 scope bullet; 4.3 | 4.2 says shed, gable ends and dutch gable "need per-edge treatment (4.3)", but 4.3 gives no rule for dutch gable or the gambrel profile, and cone, dome and flat with parapet get one line each (3.1, 4.2). Gambrel and mansard angles are presets with no proposed values. | Add one short rule per style (for example gambrel as a two-segment f(d) on each wing's gable profile; dutch gable as the hip field capped at a height with a gable on the top rectangle; cone and dome from Euclidean distance on a rasterised circle; flat with parapet as a wall ring one block up) and the preset slopes, or mark them explicitly as open design items for the spec. |
| m6 | Minor | 4.3 gable composition | The main-wing-plus-arms rule does not say what happens to an arm attached only to another arm, an arm touching two wings (a U closed into an O around a courtyard), or which side wins when an arm touches the wing on two sides. R4 F4 also recommended footprint simplification (ignore jogs shallower than a threshold), a minimum rectangle width and dominance pruning; the report adopts none and does not say why. Its "bump cross gable" test is weaker than R4's "jog invariance" (a depth-1 bump changes nothing outside its neighbourhood): a 6 by 1 bump on a 20 by 10 house still gets a 6-wide cross gable. | State the rule for arms of arms and multi-contact arms, and either adopt a jog threshold or record why a local cross gable is the intended result. Carry both into the spec as acceptance criteria with named property tests. |
| m7 | Minor | 2.4 stop-rule assessment | R3 fix 9 asked the stop-rule assessment to mention the building-generator class (llmbuilder, Arnis, GDMC). 2.4 names only Arceon, MCTools and Pugtools; the generators appear only in 2.1 and 4.5. | Add one sentence: generators such as llmbuilder, Arnis and GDMC entries build roofs over their own generated or map-derived buildings, not a player's existing build, so they do not qualify. |
| m8 | Minor | 5 Materials | R3 fix 5 required detection to check the block class or an allow list. The report instead accepts any block with `facing`, `half` and `shape` as a stair, as a labelled design decision with the Macaw's corner-rule risk stated. The deviation from a required fix is not called out, and two risks are missing: other modded blocks with the same property names that are not stair-shaped, and Macaw's steep, lower and top pieces having different geometry from a 1:1 stair. | Say explicitly that R3 fix 5 is superseded and why; restrict the property check to blocks whose `shape` property uses `StairsShape` and whose `half` uses `Half`, offer a config deny list, and list the geometry risk in section 8. |
| i1 | Info | 2.4 differentiator 4 | "Ten styles; roof-thing has three" is true, but the out-of-game Pugtools offers 16 (including dutch gable, gambrel, mansard, jerkinhead, saltbox, skillion, pyramid and dome). | Optional: note that style count is not a differentiator against Pugtools; in-game detection and placement are. |

## Positive notes

- Every body citation resolves in References and none is orphaned (checked by script).
- Volatile facts carry as-of dates; unverifiable sources (Bukkit.org 403, BuiltByBit 403, the paid
  Arceon build) are labelled in place and in Limitations.
- The stop rule is now read literally and the options are fairly stated, including the honest case for
  B (stop).
- Platform facts that matter for the build (stair rule, update flags, bundle limit, game rule default,
  private display setters) rest on bytecode, the strongest available source.
- Contradictions between sources are listed and resolved with reasons.
