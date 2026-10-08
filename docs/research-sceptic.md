# Roofwright: research sceptic pass (R4)

Gate: G5 Sceptic clean. Reviewer: R4 Research Sceptic, fresh context, 2026-10-08. Input:
`docs/research.md` (589 lines, R5 draft). Decision under test: "no published equivalent exists, so
build (differentiate)". Method: disconfirming searches on the catalogues and tools the report skipped
or covered thinly, a direct read of the closest competitors' code, a bytecode check of two platform
claims in the local 26.3 jar, and a small simulation of the algorithm claims
(`/tmp/sceptic-scripts/roofsim.py`, pure Python, results quoted below).

## 1. Disconfirming searches

All run on 2026-10-08. "0" means the API returned zero hits.

| # | Query | Where | Result | URL |
|---|---|---|---|---|
| D1 | roof generator, auto roof, roof maker, roof builder, rooftool, roof tool, roof wand, procedural roof, hip roof, gable, roofer | Modrinth API v2 search, all project types | 0 for every phrase | https://api.modrinth.com/v2/search?query=roof%20generator |
| D2 | roof, roofs | Modrinth API v2, all project types (mods, plugins, datapacks, packs) | 54 and 16 hits: roof block mods (Macaw's, aleki's, Terracotta Shingles, Roads and Roofs TFC, Fetzi's Asian Decoration, Mythrais), Nether-roof mods and datapacks. No generator | https://api.modrinth.com/v2/search?query=roof&limit=100 |
| D3 | gopaint, gobrush | Modrinth | goPaintAdvanced (Paper/Folia, 26.2, updated 2026-06-21), BetterGoPaint (1.21, 2024-12-05); goBrush only named in a server listing. Descriptions contain no "roof", "gable" or "hip" | https://api.modrinth.com/v2/project/gopaintadvanced |
| D4 | builders utilities, effortless structure, building gadgets, structurize | Modrinth | BuildersUtilities (Paper, 26.3), Intelibuild (Fabric, 26.2, 2026-10-01), Effortless Structure, Building Gadgets add-ons. Zero roof mentions in any description | https://api.modrinth.com/v2/project/intelibuild |
| D5 | roof generator, auto roof, roof maker, roof builder, rooftool, roof tool, roof wand, roofing, procedural roof, gobrush, builders utilities, effortless structure, building gadgets, structurize | CurseForge via curse.tools, classId 6 (mods), 5 (Bukkit plugins), 6945 (data packs) | Mods: only "Endless Cavern World Generator" (bedrock roof) and "The Drywall Mod" (roofing blocks). Plugins and data packs: 0 for every phrase | https://api.curse.tools/v1/cf/mods/search?gameId=432&classId=6&searchFilter=roof%20generator |
| D6 | Description grep for roof, gable, pyramid, slope, stair | CurseForge descriptions of Building Gadgets Extra (1614988), Creative Companion (1694884), Effortless Structure (1557540, 676885), unofficial port (1659533), Structurize (298744), Building Gadgets (298187) | No roof feature. **Effortless Structure** lists Slope Floor plus **Pyramid, Cone and Dome** full-block shape modes | https://api.curse.tools/v1/cf/mods/1557540/description |
| D7 | roof, roof generator, shape generator | Polymart API | Only "Rooftop Plot Road" (a plot schematic); "roof generator" 0 | https://api.polymart.org/v1/search?query=roof |
| D8 | resources search "roof"; MCTools listing | BuiltByBit | HTTP 403 to WebFetch for both; web search found the MCTools listing ("advanced shape generation and building tool") | https://builtbybit.com/resources/mctools.102917/ |
| D9 | minecraft roof generator; minecraft roof; roof generator fabric; auto roof minecraft; gable roof minecraft; roof builder minecraft; roof plugin minecraft; roof wand; roof mod fabric | GitHub repository search (sorted by update) | Owner's own repo; `BetterBuiltFool/dynamic_framing-1.20.1-fabric-forge` (WIP timber frames "and roofs", only a froe tool so far, pushed 2026-09-30); `PhaseZenith/roofed_minecraft_mod` (ore mod, unrelated); Nether-roof plugins. roof-thing itself does **not** surface (its description is "e"), so repository search is a weak instrument | https://github.com/search?q=minecraft+roof&type=repositories |
| D10 | `RoofStyle language:Java minecraft`; `gable hip StairBlock language:Java`; `roof org:IntellectualSites language:Java`; `roof user:Arcaniax` | GitHub code search | **New: `PenguinStudiosOrganization/MCTools`** has `shapes/Roof.java`. FAWE: only incidental mentions in `Extent.java` and `TextureUtil.java`, no roof command or brush (corroborates S1-4). Arcaniax: 0. Others are world generators and AI build bots | https://github.com/PenguinStudiosOrganization/MCTools |
| D11 | Arnis roof code | GitHub, `louis-e/arnis` `src/element_processing/buildings.rs` | `RoofType` enum with Gabled, Hipped, HalfHipped, Skillion, Pyramidal, Mansard, applied to OpenStreetMap polygon footprints (18,184 stars, Apache-2.0, pushed 2026-10-05) | https://github.com/louis-e/arnis |
| D12 | Axiom full tool list | Axiom docs print page | Builder tools Move, Clone, Stack, Smear, Extrude, Erase, Symmetry; Shape and Path drawing tools; Slope heightmap tool; Lua **Script Brush** and tool masks. One "roof" mention, the Smear demo. No roof preset | https://axiomdocs.moulberry.com/print.html |
| D13 | axiom lua script roof generator minecraft | Web search | No published Axiom roof script found; only an AI prompt page for writing Axiom brushes | https://openwebui.com/m/amz123/create-axiom-brush |
| D14 | goBrush goPaint Arcaniax brushes roof; FAWE roof command or brush | Web search | goBrush is a heightmap terrain brush; no roof brush documented. FAWE: none | https://dev.bukkit.org/projects/brushes-plugin |
| D15 | minecraft roof generator mod 2026; youtube automatic roof plugin | Web search | Roof block mods only | https://moddex.gg/mod/macaws-roofs |
| D16 | "minecraft roof generator mod"; "minecraft automatic roof plugin worldedit" | YouTube results page (scraped, locale pt) | Roof block mod showcases (Macaw's, Yuushya, Rojiura), a WorldEdit maths roof video, and "Create Instant Roads, Arches, Ropes, and Roofs, Arceon Guide" (jUjOPSO3Lp4). No new generator | https://www.youtube.com/results?search_query=minecraft+roof+generator+mod |
| D17 | roof-thing code read | GitHub contents API, `RoofService.java`, `SelectionPreview.java`, commit list | Two commits, both 2026-10-06 00:21 UTC. Undo skips any block that is no longer exactly what it placed; placement checks `level.mayInteract(player, pos)` and `before.isAir() \|\| before.canBeReplaced()`; flags `Block.UPDATE_CLIENTS`; particle outline of marked sections; custom `roof_wand` item with texture and recipe | https://github.com/howdoiusethissite/roof-thing |
| D18 | CPA versions | Nucleoid maven metadata | Only 1.0.0 and 2.0.0; lastUpdated 2026-03-23, before the 26.3 release (2026-09-15) | https://maven.nucleoid.xyz/eu/pb4/common-protection-api/maven-metadata.xml |
| D19 | `max_block_modifications` default; bundle limit | `javap -c -constants` on `minecraft-common-deobf-26.3.jar` (`GameRules`, `BundlerInfo`, `BundlerInfo$1$1`) | Default `ldc 32768`. `BUNDLE_SIZE_LIMIT = 4096`, exceeding it throws `IllegalStateException("Too many packets in a bundle")` | local Loom cache |

Not reachable: BuiltByBit search (403), Discord-only tools, the Arceon paid build. SpigotMC and Hangar
were covered by the report (S1-14, S1-15) and not repeated.

## 2. Findings

### F1. Stop rule reinterpreted (major)

Evidence: the owner's rule is "if research finds an existing tool that already generates proper roofs
on current versions, report to the owner with options before building". It does not say "published".
roof-thing runs on 26.3, builds gable and hip roofs with valleys over unions of rectangles, and tests
stair shapes against vanilla (D17, R1-1). On a literal reading the rule is triggered. Section 2.4 adds a
"published" qualifier and concludes "not triggered". `docs/sdlc-log.md` line 18 records a push
notification to the owner "with the default differentiate", and build scaffolding (`build.gradle`,
`src/`) already exists in the worktree, so building went ahead on a default rather than an explicit
owner choice.

Fix: in sections 1 and 2.4, say the rule is triggered on a literal reading (or at best ambiguous) by an
unpublished tool, list options A, B and C, and record the owner's explicit answer (or an explicit
"proceed on default" he can see on his phone) in `docs/sdlc-log.md` before G3 Build green. The
recommendation itself (option A) is sound.

### F2. Missed published competitor: MCTools (major)

Evidence: `PenguinStudiosOrganization/MCTools` (D10), sold on BuiltByBit, Paper plugin
(`api-version: '1.20'`, built against `paper-api 1.21.4`, Java 21). `shapes/Roof.java` (added in commit
"roof, stairs, mixed structures", 2026-04-12) implements `/mct roof <block> <width> <length> <pitch>
<style>` with styles peaked (gable), hip and flat with parapet, plus a hollow variant. It places one
block type in a rectangle centred on the player: no stairs, no slabs, no facing, no footprint, no
valleys. The plugin also advertises preview, 1000-step undo and redo, pause and resume, and
TPS-adaptive async placement. Its README does not mention the roof shape, which is why text searches
miss it. The BuiltByBit page returned 403, so whether the sold build already contains `Roof.java` is
unconfirmed.

Fix: add a row to table 2.1 ("Partial: single-block rectangle roof shape, not footprint-aware, not
stairs, Paper only, no 26.x evidence"). It does not trigger the stop rule (not "proper" roofs), but it
weakens S1-25 ("GitHub-wide searches surfaced only roof-thing") and differentiators 7 and 8 (undo and
redo, limits, tick spreading are table stakes in builder tools, not differentiators).

### F3. roof-thing comparison is not a superset and misstates two points (major)

Evidence (D17):

- roof-thing's undo is already conflict-safe ("Only touch blocks that are still exactly what we
  placed"). Differentiator 7 implies otherwise. The real difference is redo and a longer history.
- roof-thing already respects protection through `level.mayInteract(player, pos)` and places only into
  air or replaceable blocks. Differentiator 10 compares against WorldEdit, not against the closest rival.
- roof-thing supports **per-section heights** (wings at different wall heights) and **survival material
  cost**. Roofwright v1 excludes both (section 8). On those axes Roofwright v1 is behind.
- An unstated real differentiator: roof-thing registers a custom item with a texture and a recipe, so
  clients need the mod. Roofwright's server-only design (vanilla stick plus components) lets vanilla
  clients join.
- roof-thing does have a selection preview (particles), but no roof ghost. "No preview" should read
  "no roof preview".

Fix: rewrite differentiators 6, 7, 10 and 11 against roof-thing specifically, add a "where Roofwright v1
is behind" line (multi-height wings, survival cost), and add server-side-only as a cited differentiator.

### F4. Maximal-rectangle max-composition is unsound for gables in common cases (major)

The hip half of section 4.3 holds; the gable half needs design work before it reaches the spec.
Simulation results (`roofsim.py`):

- **Hip equality confirmed.** Max over maximal rectangles of per-rectangle hip profiles equalled the
  Chebyshev transform on 300 of 300 random unions of up to five rectangles; the Chebyshev surface had 0
  neighbour height jumps greater than 1.
- **Spurious cross gable from a one-block jog.** A 20 by 10 block with a 6-long, 1-deep bump on one long
  wall yields maximal rectangles (0..19, 0..9) and (7..12, 0..10). The second is 6 by 11, so its ridge
  runs along its long side, **perpendicular** to the main ridge, through the whole building: 12 cells of
  the main roof change, up to height 3 at the opposite eave where the main roof is at 1. A bay window or
  a misplaced wall block gives the house an unwanted cross gable.
- **Cliffs.** Gable profiles have vertical ends, so max-composition is not 1-Lipschitz. A 10-wide block
  flush with an 8-wide block gives 3 neighbour jumps greater than 1, inside the footprint. Each needs a
  gable infill wall, not only the outer gable triangles that differentiator 5 covers.
- **Orientation flips.** A 7 by 6 rectangle gets a ridge along x, a 6 by 7 one along z; one block
  changes the roof. Ties (squares) need a deterministic rule.
- **Count growth.** 16 single-cell bumps on one wall of a 32 by 16 block give 17 maximal rectangles; noise
  on two walls gives 25 and 6 cliffs. The worst case is quadratic in the vertex count, and every extra
  rectangle can add a small gable.
- **L shapes.** Both arms' maximal rectangles run to the outer corner, so the corner gets two gable ends
  and a crossed ridge, not the common "main ridge plus wing ending in a valley". It is a valid style, but
  it is a choice and should be stated.

Fix: in section 4.3 mark the gable path as an open design problem. Add footprint simplification (ignore
jogs shallower than a threshold or the overhang), a minimum rectangle width, dominance pruning, a
deterministic ridge tie-break, and a "main block plus wings" mode (largest rectangle first, wings
attached by valley). Add an internal gable infill rule for cliffs. G4 property tests: jog invariance
(adding a depth-1 bump changes no cell outside its 1-block neighbourhood), cliff detection (every jump
greater than 1 is filled), and the hip equality check.

### F5. Chebyshev claim is correct but mis-cited and under-scoped (minor)

Evidence: [S3-23] is about 3D orthogonal polyhedra. The 2D fact follows directly: for an orthogonal
polygon with equal slopes, the straight-skeleton wavefront at time t equals erosion by an axis-aligned
square of half-size t, because a square has mitred corners at both convex and reflex vertices and
erosion by squares composes (t1 then t2 equals t1 + t2). The simulation agrees (F4). Scope limits the
report omits:

- Equal pitch on every edge only. Shed, gable ends and dutch gable need per-edge weights. Mansard,
  gambrel-hip and pyramid are expressible as a monotone function f(d) of the same transform.
- Discrete cases: even widths give a 2-wide top plateau; a 1-wide arm gives a flat run with no slope;
  cells touching only diagonally give outer-corner stairs meeting at a point; at 1:2 pitch hips mix
  slabs and stairs and there is no slab corner piece.
- Overhang geometry is undefined: computing the transform on the dilated footprint raises the roof by
  the overhang unless the base is lowered, which at 2:1 pitch and overhang 3 drops the eave 6 blocks
  below the wall top.

Fix: write the erosion argument as author inference, keep [S3-23] as supporting, and list these limits.

### F6. Packet-only ghost has a hard protocol limit the report does not mention (major)

Evidence (D19): `BundlerInfo.BUNDLE_SIZE_LIMIT = 4096` in 26.3, and the bundler throws
`IllegalStateException("Too many packets in a bundle")`. A block display needs at least an add-entity
and an entity-data packet, so one bundle holds at most about 2,047 ghosts, below the "few thousand" cap
in section 5. Other unstated obligations of packet-only entities: entity ids must come from the server's
own id counter to avoid colliding with real entities; ghosts vanish when the client unloads their chunk
or changes dimension and must be resent; removal must be sent on cancel, place and disconnect; glowing
outlines for thousands of entities add a client outline pass.

Fix: add these to section 5 and to the spec as acceptance criteria (for example, a GameTest or client
test that previews more than 2,048 cells without a disconnect).

### F7. `max_block_modifications` default is verified (minor)

Evidence (D19): `ldc 32768` in `GameRules` bytecode. Fix: drop "believed" and "unverified" in sections
6.1 and 8, cite the jar.

### F8. Protection details (minor)

Evidence (D18): CPA's last release predates 26.3 by almost six months, as the report says. Unstated:
Fabric jar-in-jar picks one CPA copy when GOML also bundles it, and a load-only smoke test does not
prove binary linkage; the test must call `canPlaceBlock` on a 26.3 server. Placement spread over ticks
can outlive the player's session, so the check needs the stored profile, or the job must cancel on
logout. Claim mods without CPA (OPAC, possibly FTB Chunks) are unprotected; `level.mayInteract`, as
roof-thing uses, is a cheap extra layer.

Fix: add to sections 6.5 and 8.

### F9. The wand must never be the authority (minor)

Evidence: the wand is a vanilla stick with a `custom_data` key (section 5). Creative-mode clients can
send arbitrary item stacks, including components. Fix: state that every action checks the permission
node at use time, and that holding the item grants nothing.

### F10. Absolute wording beyond what was searched (minor)

Evidence: "Nothing ... generates a complete stair-and-slab roof from a building footprint, on any loader
or version". BuiltByBit was unreadable (403), Polymart and Discord were not covered by the packs, and
roof-thing shows that repository search misses projects with poor metadata (D9). Fix: "No tool found in
searches of Modrinth, CurseForge, SpigotMC, Hangar, Polymart and GitHub on 2026-10-08 ...", and list
BuiltByBit and Discord as unchecked.

### F11. Missing prior art and overlaps (minor)

Evidence: Arnis (D11) generates gabled, hipped, half-hipped, mansard, skillion and pyramidal roofs over
real-world polygon footprints. It is not a tool for a player's own build, but it is the largest open
source Minecraft implementation of footprint-to-roof and belongs in section 4.5 next to GDMC.
Effortless Structure (D6) offers full-block Pyramid, Cone and Dome shapes, which overlap Roofwright's
pyramid, cone and dome styles in name. dynamic_framing (D9) is a WIP timber-frame and roof mod on
1.20.1. Fix: add them to sections 2.1 and 4.5.

### F12. Demand summary contradicts the body (minor)

Evidence: the summary heading says "Demand is real but indirect", but section 2.2 concludes "there is no
measured demand". Macaw's 104.6M downloads show demand for roof blocks placed by hand, and the brief's
Reddit thread complains about roof-block undersides, which a stair generator does not fix. Fix: change
the heading to "Demand is unmeasured", and say Macaw's downloads argue for Macaw's integration, which
v1 defers.

## 3. Bias check

- **Sunk cost and confirmation.** The worktree is named `roofwright-build`, build files were in place
  before G5, and the stop rule was narrowed to "published" (F1). The differentiator list compares
  Roofwright's plans with the weakest reading of each rival (F3) and lists table stakes as
  differentiators (F2).
- **Competitors understated.** roof-thing is closer than presented: conflict-safe undo, protection
  checks, multi-height sections and survival cost (F3). MCTools was missed (F2). Arceon is treated fairly:
  primary docs are preferred over a tertiary review, and its paid, Bukkit-only status is stated. Pugtools
  is fairly presented as the closest feature set out of game.
- **Design decisions as facts.** Mostly well labelled ("design decision", "author inference"). Exceptions:
  "hip and gable roofs share one framework" reads as established, and F4 shows it is not for gables; the
  "few thousand entities" cap is presented as safe, and F6 shows a protocol limit below it.
- **Demand.** The body is honest and explicitly discounts the brief's Reddit citation; only the summary
  heading overstates it (F12).
- **Positives.** The contradictions are logged, gaps are listed, the stair-shape rule was read from
  bytecode, and Arceon's tertiary and primary sources are kept apart.

## 4. Injection sweep

No prompt-injection attempts found. Checked: the roof-thing README and source, the MCTools README and
`Roof.java`, the dynamic_framing and roofed repo metadata, the Arnis repo metadata and `buildings.rs`
grep, the Axiom docs print page, Modrinth, CurseForge and Polymart API JSON, Nucleoid maven metadata,
YouTube result titles, and web search snippets. One web result (`openwebui.com/m/amz123/create-axiom-brush`)
is itself a prompt written for an AI model. It was not fetched and its content was not followed. Notes
appended to tool output by the search tool are tool text, not source content. Downloaded files were kept
in `/tmp/sceptic-dl/` and parsed with `python3 -I`.

## 5. Verdict

**The build decision stands, but G5 Sceptic clean is not yet clean.** Every disconfirming search
failed to find a published tool that generates stair-and-slab roofs from a footprint on 26.3. The near
misses are MCTools (single-block rectangle shapes, Paper, no 26.x evidence), Arceon (paid FAWE add-on,
spike extrusion, no 26.x evidence), Pugtools (out of game), Effortless Structure (full-block pyramid,
cone and dome) and Arnis (world generator from map data). roof-thing remains the only real
near-equivalent and is unpublished.

To clear G5:

1. F1: record the owner's explicit choice on the stop rule (roof-thing literally qualifies).
2. F2, F3: correct the competitor table and the differentiator list.
3. F4: mark gable composition as an open design problem, with the mitigations and property tests above
   carried into the spec.
4. F6: add the 4,096-packet bundle limit and other packet-only obligations to the preview design and
   acceptance criteria.

F5 and F7 to F12 are minor wording and completeness fixes and can go into the same revision.
