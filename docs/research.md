# Roofwright: research report

Deep research item for Roofwright, an open source Fabric mod for Minecraft Java 26.3 that generates a
complete roof from a building footprint. Written by the R5 Synthesis Writer of the Agentic SDLC Deep
research team on 2026-10-08 from four R2 evidence packs (S1 world editors and plugins, S2 mod catalogues
and name, S3 architecture and algorithms, S4 platform and safety), one lead-verified addendum (R1), the
R3 claim ledger (`docs/research-ledger.md`) and the R4 sceptic pass (`docs/research-sceptic.md`), revised
after both reviews. Citation ids: `[S<slice>-<claim>]` for pack claims, `[R1-1]` for the addendum,
`[R3-L<row>]` for ledger rows, `[R4-D<row>]` and `[R4-F<finding>]` for the sceptic's searches and
findings, `[LOG-1]` for the gate log. The References section maps each id to its source. Statements that
no source supports are marked "(author inference)" or "(design decision)". Volatile counts and versions
are as of 2026-10-08 unless stated.

## 1. Summary

- **No published equivalent found in the searched catalogues.** Searches of Modrinth, CurseForge (mods,
  Bukkit plugins, data packs), SpigotMC, Hangar, Polymart and GitHub on 2026-10-08 found no published
  tool that generates a complete stair-and-slab roof from a building footprint [S2-1] [S2-3] [S1-14]
  [S1-15] [R4-D2] [R4-D5] [R4-D7]. BuiltByBit (403) and Discord-only tools were not checked [R4-D8].
  The nearest published tools place single-block roof shapes: WorldEdit's 2011 `roof.js` [S1-1], Arceon's
  `//roof` [S1-9] and MCTools' `/mct roof` [R4-D10]. Roof block mods such as Macaw's Roofs are placed by
  hand [S2-5] [R3-L11].
- **One unpublished near-equivalent triggers the stop rule.** `howdoiusethissite/roof-thing` is a Fabric
  26.3 mod (created 2026-10-06) that builds gable, rotated gable and hip roofs over marked rectangles with
  valley merging, conflict-safe undo, protection checks, per-section heights and survival material cost
  [S1-19] [R1-1] [R4-D17]. It is not on Modrinth or CurseForge and has 0 stars (as of 2026-10-08) [S1-20].
- **Out-of-game near-equivalent.** The Pugtools web designer has 16 styles on rectangle or L footprints
  with `.litematic` export [S1-21] [R3-L16].
- **Name is free** on Modrinth and CurseForge (as of 2026-10-08) [S2-26] [S2-28].
- **Demand is unmeasured.** The brief's Reddit thread is about the underside of roof blocks, which a
  stair generator does not fix [S2-29]. Direct requests are few, old or unverified [S2-30] [S2-31]
  [S2-32].
- **Decision.** The owner was notified on 2026-10-08 with options A, B and C and the default
  "differentiate" [LOG-1]; no explicit reply has been received yet. The build proceeds on option A under
  the owner's standing goal instruction and is reversible if he chooses B or C (section 2.4).

## 2. Question 1: demand and gaps

### 2.1 Existing tools

Versions and dates as of 2026-10-08.

| Tool | What it does for roofs | Platform | Latest MC version seen | Near-equivalent? |
|---|---|---|---|---|
| WorldEdit + `roof.js` | Bundled CraftScript (2011, Bentech): one-block rings of one block type, one level higher per ring, a stepped full-block pyramid over the bounding box, no stairs [S1-1] [S2-11]. `//generate` places one pattern without stair facing [S1-5]. No roof command or brush in the code [S1-4] | Fabric, Forge, NeoForge, Quilt, Bukkit [S2-12] | 26.3 (7.4.6-beta-02, 2026-09-24) [S4-26] | No |
| FastAsyncWorldEdit | Same `roof.js`; no roof brush [S1-3] [S1-4] | Paper | 2.16.0, 2026-10-04 [S1-4] | No |
| FastAsyncVoxelSniper, VoxelSniper Reimagined | No roof brush; Reimagined last pushed 2024-09-30 [S1-6] | Paper | active / stale [S1-6] | No |
| Axiom | Shape (pyramid, cone), Path with stairs and slabs, Slope, a Lua Script Brush; only roof mention is a manual Smear demo; changelogs never mention roof or gable [S1-7] [S1-8] [R4-D12]. No published Axiom roof script was found [R4-D13] | Fabric client mod | 6.1.3 for 26.3, 2026-09-25 [S1-8] | No |
| Arceon | `//roof <block> <width> [height]` extrudes a spike cross-section along a convex or cuboid selection (noise, bevel, hollow, slab flags); no documented stair facing, hips, valleys, styles or wall detection [S1-9] [S2-8]. Downloads are distributed through Patreon posts; a tertiary listing calls it premium; price not confirmed (Patreon 403) [R3-L9] | FAWE add-on (Bukkit/Paper), plus Arceon x Axiom | wiki: v0.5.5 and Axiom v0.1.1 "[1.20+]" [S2-9]; a tertiary listing gives 0.5.4 for 1.20 to 1.21.10 [R3-L10]; no 26.x build documented | Partial |
| MCTools | `/mct roof <block> <width> <length> <pitch> <style>` with peaked (gable), hip and flat-with-parapet styles plus a hollow variant; one block type in a rectangle centred on the player; no stairs, slabs, facing, footprint or valleys. Advertises preview, 1000-step undo and redo, pause and resume, TPS-adaptive async placement. Sold on BuiltByBit (page 403, so whether the sold build contains `Roof.java` is unconfirmed) [R4-D10] [R4-D8] | Paper plugin (built against paper-api 1.21.4) | 1.21.4; no 26.x evidence [R4-D10] | Partial |
| SpigotMC, Hangar, CurseForge Bukkit, Polymart | "roof" hits are Nether-roof blockers and unrelated plugins [S1-14] [S1-15] [S1-17] [S2-25] [R4-D7] | Bukkit/Paper | n/a | No |
| Macaw's Roofs and other roof block mods | Decorative roof blocks (633 blockstate files), one `roofing_hammer`, no generator, wand or blueprint item [S2-6] [R3-L11]. Others: aleki's, Terracotta Shingles, Chiseled Roofs, FWP Roofs, Roads and Roofs TFC [S2-2] [S2-4] | Fabric, Forge, NeoForge | 26.3 (2026-09-29) [S2-5] [S4-37] | No (material source) |
| Effortless Building | Build modes incl. "Slope Floor", mirrors, arrays, undo/redo; no roofs [S2-14] | Fabric, Forge, NeoForge | 26.2 [S2-14] | No |
| Effortless Structure | Slope Floor plus full-block Pyramid, Cone and Dome shape modes, which overlap Roofwright's pyramid, cone and dome styles in name only (no stairs) [R4-D6] | CurseForge | not recorded [R4-D6] | No |
| Building Gadgets, Construction Wand(s), Architectural Building Wand, WallWand, Build-Helper-Mod | Copy, paste, fill, rows, planes, rectangles, cylinders, triangles, walls; none has roof styles [S2-18] [S2-19] [S2-16] [S2-17] [S2-15] | various | up to 26.3 [S2-16] [S2-19] | No |
| Litematica | Projects schematics made elsewhere, such as Pugtools exports [S2-20] | Fabric | 26.3 [S2-20] | No |
| Structurize / Domum Ornamentum | MineColonies schematics; two-material roof blocks; no generation from a footprint [S2-21] | Forge/NeoForge | 1.21.1 [S2-21] | No |
| dynamic_framing | Work-in-progress timber frames "and roofs"; only a froe tool so far [R4-D9] | Fabric/Forge | 1.20.1 [R4-D9] | No |
| Pugtools Roof Designer | 16 styles, rectangle or L footprint, a pitch setting (the page shows 1:1), per-side overhang, 3D stair and slab preview, Litematica or material list export [S1-21] [R3-L16]. That it cannot read or place into a world is inferred from its export-only workflow [R3-L16] | Browser | n/a | Closest feature set, out of game |
| Other web planners | CraftShape (rectangles only; its FAQ says to split irregular buildings into rectangles), Jethz, VoxShaper, MCToolbox, minebuildr [S1-22] [S1-23] [S2-24] | Browser | n/a | Partial, out of game |
| Building generators | jkuhta/llmbuilder (Fabric, 1.21.11) generates whole buildings with gable, hip, mansard, flat and stepped gable roofs from a distance-field planner [R3-L23]; Arnis builds gabled, hipped, half-hipped, mansard, skillion and pyramidal roofs over OpenStreetMap footprints [R4-D11]; GDMC entries (section 4.5). None takes a player's existing build | various | n/a | No (prior art) |
| roof-thing | See below [S1-19] [R1-1] [R4-D17] | Fabric | 26.3 | **Closest in-game**, unpublished |

**roof-thing in detail.** README: a wand marks wall-top corner pairs per rectangular section; sections can
have their own height and style; roofs are merged by taking the highest at every spot, giving valleys with
inner corner stairs; styles `gable`, `gable_rotated`, `hip`; odd widths get a slab ridge; overhang 0 to 3;
`/roof undo` for the last 5 roofs with materials returned; survival players must carry stairs and slabs;
existing solid blocks and protected positions are left alone; triangular gable walls are not filled;
selection in memory only; a test checks stair shapes against vanilla [S1-19] [R1-1] [R3-L22]. Marked
sections are outlined with particles, but there is no preview of the roof [R3-L20]. Code: undo skips any
block that is no longer exactly what it placed; placement checks `level.mayInteract(player, pos)` and
places only into air or replaceable blocks; it registers a custom `roof_wand` item with texture and
recipe, so clients need the mod [R4-D17]. Two commits, 0 stars, 0 forks, description "e", CC0-1.0, not
on Modrinth or CurseForge (as of 2026-10-08) [S1-20] [R1-1]. The code was read but not built or run.

GitHub repository search is a weak instrument: roof-thing does not surface in it because its description
is "e" [R4-D9], and MCTools surfaced only through code search because its README does not mention the
roof shape [R4-D10].

### 2.2 Demand signals

The brief cites r/feedthebeast 1mtk8f6 ("Feel like no mod does roofs right", 2025-08-18; score 52 and 15
comments in the archive snapshot of 2026-10-08) as players asking for a roof tool. The thread complains
that decorative roof blocks have a fixed underside texture; replies recommend Domum Ornamentum, Macaw's
Roofs with Framed Blocks or Copycats+, and vanilla building. Nobody asks for a generator [S2-29]. A stair
generator does not fix that complaint, so this report does not count the thread as demand.

Direct signals are weak:

- A Bukkit.org request for a plugin that generates a normal, gable or hip roof from points clicked around
  a house, answered with a pointer to WorldEdit scripting. **Unverified**: the page returns 403, so
  content and date (about 2012) come from a search summary [S1-24] [S2-30] [R3-L27].
- A 2024 command-block "Automatic Roof Generator" post with a score of 1 [S2-31], and a 2022 request for a
  cone roof generator [S2-32].
- Indirect: at least six web roof planners exist [S1-21] [S1-22] [S1-23] [S2-24], and roof-thing and
  MCTools' roof shape show other developers saw the gap [R1-1] [R4-D10].

Macaw's Roofs has 104.6M CurseForge downloads and 8.0M Modrinth downloads (as of 2026-10-08) [R3-L13]
[S2-5]. That shows demand for roof blocks placed by hand, and argues for supporting Macaw's blocks; v1
supports them only generically (section 5) and defers a dedicated mapping.

Honest reading: demand for an in-game roof generator is unmeasured. The bet is that builders who use web
planners or place roofs by hand will prefer a tool that reads the real build and places the result
directly (author inference).

### 2.3 Name check

As of 2026-10-08, Modrinth `GET /v2/project/{roofwright, roof-wright, roofwrights}` returns 404, and
CurseForge slug and text searches return 0 for all three (control slug `macaws-roofs` returns 1) [S2-26].
"-wright" names on CurseForge (Plankwright, Runewright, Forgewright, Mapwright) are unrelated [S2-27].
The only GitHub repository named Roofwright is the owner's [S2-28]. The name is free.

### 2.4 Stop rule assessment

The owner's rule, literally: "if research finds an existing tool that already generates proper roofs on
current versions, report to the owner with options before building." roof-thing runs on 26.3, builds
gable and hip roofs with valleys over unions of rectangles and tests stair shapes against vanilla
[S1-19] [R4-D17]. **On a literal reading the rule is triggered** [R4-F1]; that roof-thing is unpublished
and two days old does not change the wording. Arceon and MCTools do not qualify (single-block shapes, no
26.x evidence) [S1-9] [R4-D10]; Pugtools is out of game [S1-21].

Options reported to the owner:

- **(A) Differentiate and build (recommended, the default).** Build Roofwright with the differentiators
  below and credit roof-thing, MCTools and Pugtools as prior art. Risk: roof-thing may publish first.
- **(B) Stop.** Reasonable if the owner judges roof-thing good enough and likely to be published.
- **(C) Contribute to roof-thing.** CC0-1.0 allows reuse [R1-1], but the repository shows no maintenance
  or community (two commits, 0 stars, 0 forks) [S1-20], and it needs a client-side mod [R4-D17].

Status: the owner was notified by push notification on 2026-10-08 with these options and the default
"differentiate" [LOG-1]. No explicit owner reply has been received yet. The build proceeds under the
owner's standing goal instruction; it is reversible if the owner chooses B or C.

**Headline differentiators against roof-thing** (each maps to an acceptance criterion in
`docs/spec.md`; design decisions unless cited):

1. **Server-side only: vanilla clients can join.** The wand is a vanilla item with components; roof-thing
   registers a textured, craftable item that needs the mod on the client [R4-D17].
2. **One-click footprint detection from wall tops**, versus marking rectangle corners [S1-19].
3. **Any rectilinear footprint** (L, T, U, arbitrary), versus marked rectangles [S1-19].
4. **Ten styles**: gable, hip, dutch gable, gambrel, mansard, pyramid, shed, flat with parapet, cone,
   dome; roof-thing has three [S1-19].
5. **Three pitches** (1:2, 1:1, 2:1); roof-thing documents none [S1-19].
6. **Gable wall infill** matched to the wall block, including internal steps; roof-thing leaves gable
   triangles open [R1-1].
7. **Roof preview ghost** with blocked positions highlighted; roof-thing outlines sections with particles
   only [R3-L20].
8. **Claim-mod support** through Common Protection API (GOML, Flan) in addition to vanilla checks;
   roof-thing checks `level.mayInteract` [R4-D17] (whether claim mods hook that method was not checked).

**Table stakes, not differentiators**: per-operation limits, placement spread across ticks, undo and redo
history that never clobbers later edits, permission nodes. MCTools advertises undo and redo and
TPS-adaptive placement [R4-D10]; roof-thing's undo already skips changed blocks [R4-D17]. Redo is the only
one roof-thing lacks [R1-1].

**Where Roofwright v1 is behind roof-thing**: wings at different wall heights, and survival material
cost [R1-1] [R4-F3]. Both are v1 scope decisions (section 8).

## 3. Question 2: roof architecture

### 3.1 Types and parts

| Style | Definition | Source |
|---|---|---|
| Gable | Inverted V: two slopes meet at a central ridge | [S3-1] |
| Hip | All sides slope to the walls; hips rise from external corners to the ridge; a valley joins slopes at an internal corner; cross hips serve L and T plans; a square hip is a pyramid | [S3-2] |
| Dutch gable | Gable at the top, hipped slope lower down (jerkinhead is the reverse) | [S3-3] |
| Gambrel | Two slopes per side, shallow upper and steep lower, vertical gable ends | [S3-4] |
| Mansard | Two slopes on all four sides, lower steeper ("curb hip"), often with dormers | [S3-5] |
| Pyramid | Hipped equally on all sides over a square or regular polygon | [S3-6] |
| Shed | One slope, historically against a taller wall | [S3-7] |
| Flat with parapet | "Generally gently pitched"; Minecraft guides recommend a border of slabs, backwards stairs or a parapet | [S3-7] [S3-16] |
| Cone, dome | Illustrated but not defined in text; on the grid, stacks of rasterised circles | [S3-7] [S3-31] |

No source gives standard gambrel or mansard angles, so they are presets. The Minecraft wiki guide: a
simple gable is "not suitable for buildings with widths greater than about 12 blocks"; its mansard
example is about 16x20 and it says buildings need to be quite large before mansards look right; a good
helm roof is "very fiddly" [S3-16] [R3-L56].

Parts: **eaves** overhang a wall; the overhang at a gable end is the **verge** or **rake**; the
**fascia** runs along the eaves and the **soffit** sits under them [S3-8]. The **ridge** is where slopes
meet, **hips** run from external corners and **valleys** from internal corners [S3-2]. Inverted
(top-half) stairs under the verge are a common Minecraft detail [S3-33]. CityEngine separates eave
overhang (`overhangX`) from verge overhang (`overhangY`) and takes angle or height plus ridge direction
[S3-27].

### 3.2 Pitch on a block grid

Pitch is rise over run [S3-10]; the degrees below are arithmetic [S3-10].

| Pitch | Blocks per step | Angle |
|---|---|---|
| 1:2 (low) | bottom slab, then a full step every two blocks | 26.57 deg |
| 1:1 (standard) | one stair per block, facing up-slope toward the ridge | 45 deg |
| 2:1 (steep) | full block plus stair per block | 63.43 deg |

The 1:1 convention matches the GDPC tutorial (stairs facing the ridge, full-block ridge row) [S3-17].
Bottom slabs are the natural half step and stop mob spawning [S3-14]; the wiki shows 1-in-1 and 2-in-1
pitches [S3-16].

**Even and odd widths.** An odd width gives a single ridge row; an even width gives a two-wide top level
closed by two stairs back to back or a slab cap [S3-35] (agent analysis). roof-thing caps odd widths with
a slab ridge [R1-1]; the wiki's half-hipped example includes a layer of slabs at the peak [S3-16].

### 3.3 Stair block states and the 26.3 corner rule

Stairs have `facing` (the direction of the full-block side), `half` (bottom or top), `shape` (straight,
inner_left, inner_right, outer_left, outer_right) and `waterlogged` [S3-11]. On a roof `facing` points
up-slope [S3-17]. Upright and upside-down stairs never join [S3-12].

The exact rule in 26.3 `StairBlock.getStairsShape`, with `f` the stair's facing [S3-13] [R3-L50]:

1. Block **in front** (`pos.relative(f)`): if it is a stair with the same `half` and a facing `g` on the
   other axis, and `canTakeShape(state, level, pos, g.getOpposite())`, return OUTER_LEFT when
   `g == f.getCounterClockWise()`, else OUTER_RIGHT.
2. Else block **behind** (`pos.relative(f.getOpposite())`): same test with `canTakeShape(..., g)`, giving
   INNER_LEFT or INNER_RIGHT.
3. Else STRAIGHT.

`canTakeShape` is false only when the block at `pos.relative(dir)` is a stair with the same facing and
half. Outer wins over inner. Shape is recomputed only on horizontal neighbour changes [S3-13]. A generator
that places with update suppression must compute `shape` itself with this rule; hip cells become outer
corners and valley cells inner corners [S3-13] [S3-35]. Roofwright ports the rule and proves it against
vanilla (section 6.2) (design decision). roof-thing already tests this [R1-1], so it is table stakes.

## 4. Question 3: algorithms

### 4.1 Straight skeleton background

The straight skeleton (Aichholzer, Aurenhammer, Alberts and Gartner, 1995) gives "a canonical way of
constructing a polygonal roof above a general layout of ground walls" [S3-19]. Its arcs are the ridge
lines of the roof obtained by lifting each point by the time the inward wavefront reaches it [S3-20]
[R3-L54]; hips and valleys are the roof's external and internal corner lines [S3-2]. Felkel and
Obdrzalek's 1998 algorithm [S3-21] "has been shown ... incorrect"; correct bounds are Huber-Held
O(nr log n) and Eppstein-Erickson O(nr + n log n) [S3-20]. CGAL offers weighted skeletons and capped
extrusion (gables and mansards as weight or height tricks), but it is C++ [S3-22]. Kelly and Wonka report
that architectural models produce "a large number of degenerate events" and that weighted skeletons are
ambiguous in the concave case [S3-24]. Laycock and Day (2003) derived several roof styles from a modified
skeleton (metadata only) [S3-25]. CityEngine warns that `roofGable` only works on convex single-face
shapes [S3-27]. Rectilinear footprints are among the most degenerate inputs for event-based skeletons
[S3-24], so a general skeleton is the wrong tool here (author inference).

### 4.2 Key result: the hip roof is a Chebyshev distance transform

Barequet, Eppstein, Goodrich and Vaxman show, for **3D orthogonal polyhedra**, that the offset surface at
time t is the set of points at L-infinity distance t from the boundary, and that for polycubes the
skeleton is a voxel sweep one L-infinity unit per round, "significantly easier to compute for orthogonal
inputs" [S3-23]. The 2D application is **author inference**: for an orthogonal polygon with equal slopes,
the wavefront at time t equals erosion by an axis-aligned square of half-size t, because a square has
mitred corners at convex and reflex vertices and erosions by squares compose [R4-F5]. So the 45-degree
hip height on a rectilinear footprint is the Chebyshev distance to the outside, computed by an
8-neighbour multi-source BFS in O(cells). An L-shape check produced ridge rows, hips at convex corners and
a straight valley at the reflex corner [S3-35]. A Euclidean transform would round reflex corners into
cones; it is only for cone and dome over round towers [S3-31] (author inference).

**Scope** [R4-F5]:

- **Equal pitch on every edge only.** Shed, gable ends and dutch gable need per-edge treatment (4.3).
  Mansard and pyramid are a monotone function f(d) of the same transform.
- **Overhang** is a chessboard (square) dilation of the footprint by k cells, with heights offset so the
  roof over the wall line stays at the wall top. The eave then drops k times the pitch below the wall
  top (6 blocks at 2:1 with k = 3), so overhang is clamped per pitch (design decision).
- **Discrete edge cases**: even widths give a two-wide plateau; a 1-wide arm gives a flat run with no
  slope; cells touching only diagonally give outer-corner stairs meeting at a point; at 1:2 hips mix
  slabs and stairs and there is no slab corner piece. Each gets a named test (design decision).

### 4.3 Design: composition rules per style

**Hip, pyramid, mansard (and shed).** Hip height is the Chebyshev transform. Equivalently, it is the
maximum over all maximal rectangles (axis-aligned rectangles inside the footprint that cannot be extended)
of each rectangle's own hip profile (author inference: any rectangle inside the footprint has no larger
Chebyshev distance, and the largest square centred on a cell lies in some maximal rectangle). The R4
simulation confirmed the equality on 300 of 300 random unions of up to five rectangles [R4-F4]. Mansard
is f(d) of the same field [R4-F5]. Shed keeps maximal-rectangle max-composition with one slope direction
for the whole roof (design decision).

**Gable, dutch gable, gambrel: main wing plus arms (design decision).** Max-composition over maximal
rectangles is unsound for gables [R4-F4]: a 6-long, 1-deep bump on a 20 by 10 block creates a 6 by 11
maximal rectangle whose ridge runs across the whole building; a narrow block flush with a wider one makes
vertical cliffs inside the footprint; and an L gets two gable ends and a crossed ridge at the corner
instead of a wing ending in a valley. Roofwright therefore uses a partition:

1. The largest maximal rectangle is the **main wing**, ridge along its long side; ties (squares) break by
   a fixed rule (ridge along x) so results are deterministic.
2. The rest of the outline is split greedily into non-overlapping rectangles.
3. Each attached piece is an **arm**. Its ridge runs perpendicular to the side by which it is attached
   and extends into the wing it touches up to that wing's ridge line, so it meets the main roof in a
   valley and stops.
4. If the arm would rise above that wing's ridge (it is wider than the wing), its ridge instead runs
   parallel to the attachment, with no extension.
5. Every neighbour height jump greater than one block inside the footprint gets gable infill, not only the
   outer gable triangles.

Property tests (G4) cover the sceptic's failure cases: **bump cross gable** (a shallow bump yields at
most a local cross gable that ends in a valley, never a ridge through the building), **width step** (every
internal jump is filled), **L with two gable ends** (main ridge plus one arm ending in a valley, one gable
end per arm), plus the hip equality check above. A minimum rectangle partition (n/2 + h - g - 1 pieces,
polynomial via bipartite matching) [S3-28] is the fallback if the greedy split produces too many pieces
on noisy outlines.

### 4.4 Footprint detection from wall tops

Design decision, based on connected-component labelling and flood fill [S3-29]:

1. Take the horizontal slice at the top of the clicked wall within a bounded search box.
2. Mark wall cells; walls 8-connected, outside 4-connected, so a diagonal seam still seals the inside
   (author inference).
3. Flood fill from the box border through non-wall cells; unreached cells are interior. Footprint =
   interior plus walls.
4. Fail cleanly if the fill leaks inside through a gap, and report where.

Enclosed courtyards are unreachable from outside, so v1 covers them (a stated limitation) [S3-29].

### 4.5 Prior art in generators

GDMC entries add roofs after the shell with no general roof algorithm [S3-32]; a typical entry
hard-codes a fixed-width gable with filled gable walls and upside-down accent stairs [S3-33]. An
evolutionary generator labels a voxel with empty space above it as roof [S3-34]. Arnis, the largest open
source footprint-to-roof implementation in Minecraft (Apache-2.0), applies six roof types to OpenStreetMap
polygons [R4-D11]. jkuhta/llmbuilder uses a distance-field roof planner for generated buildings [R3-L23].
Effortless Structure has full-block pyramid, cone and dome shapes [R4-D6]; dynamic_framing plans roofs but
ships only a froe tool [R4-D9]. None works on a player's existing build.

## 5. Question 4: interaction design for vanilla clients

**Platform.** Minecraft 26.3 was released 2026-09-15, needs Java 25 and ships unobfuscated [S4-1] [S4-4]
[S4-5]. As of 2026-10-08, Fabric loader 0.19.5 and Fabric API 0.162.0+26.3 support it; from 26.1,
dependencies use `implementation` [S4-6] [S4-7].

**Wand.** A vanilla `minecraft:stick` (design decision) with components that exist in the 26.3 jar
[S4-16]: `custom_data` with a `roofwright` key [S4-15]; `item_name` (anvil-proof, not italic), `lore`,
`enchantment_glint_override: true`, and `item_model` pointing at an existing vanilla model, because a
missing model renders as the missing-model cube [S4-17] [R3-L32]. **The wand grants nothing**: creative
clients can create arbitrary stacks with any components [R4-F9], so every action checks the permission
node at use time.

**Commands.** `/roof` with style, pitch, overhang, material, preview, place, cancel, undo, redo and reload
(design decision). WorldEdit for Fabric hooks wand clicks with `AttackBlockCallback` and `UseBlockCallback`
[S4-26].

**Preview.** Block displays (since 1.19.4) are vanilla entities with no hitbox or collision, so a ghost
never blocks building [S4-8] [S4-9]. The jar has `block_state`, `transformation`, `view_range`,
`shadow_radius` and `glow_color_override` [S4-10]; the `Glowing` flag draws an outline [S4-11]. Per-entity
outline colour (green placeable, red blocked) is to be confirmed in the client capture run [R3-L34]. The
setters are private in 26.3, so a mod needs an accessor, an access widener, the NBT path or packet-only
entities [S4-12]. Design decision: **packet-only ghosts** sent only to the builder, never saved, invisible
to others. Obligations [R4-F6]:

- **Bundle limit.** 26.3 caps a packet bundle at 4,096 packets and throws "Too many packets in a bundle"
  beyond it [R4-D19]. A ghost needs at least an add-entity and an entity-data packet, so the preview is
  sent in several bundles of at most about 2,000 ghosts each.
- **Entity ids** come from the game's own entity counter, so ghosts never collide with real entities.
- **Lifetime.** Ghosts vanish when the client unloads their chunk or changes dimension; the player can
  re-show them with the preview command. Removal is sent on cancel, place and disconnect.
- **Cost.** MC-276285 (open) reports 4,096 item displays on one entity, and doubling that made MSPT and
  FPS unplayable, on 1.21.1 to 1.21.4, not reported on 26.x [S4-13] [R3-L36]; batching mods exist for this
  load [S4-14]. So the ghost count is capped (configurable), shadow is 0, view range modest, and above the
  cap only the outline and blocked positions are shown (design decision).

**Materials.** `net.minecraft.data.BlockFamilies` (with `BlockFamily$Variant`) is in the 26.3 common jar
[R3-L57], so vanilla families (stairs, slab, full block, wall) resolve without a hand list; runtime use on
a dedicated server is to be checked. Modded blocks are detected by block state properties (`facing`,
`half`, `shape` for stairs; `type` for slabs) [S3-11] [S3-14]. Macaw's Roofs `RoofBlock` does not extend
`StairBlock` but declares the same `FACING`, `HALF` and `SHAPE` properties and its own `getStairsShape`
[R3-L14]. **Roofwright deliberately accepts any block with stair properties as a stair and sets the shape
itself** (design decision), so Macaw's roof blocks work as stair material. Risk: Macaw's own corner rule
may differ from vanilla's and could rewrite a shape on a later neighbour update; a GameTest with Macaw's
loaded should check this before it is advertised. A dedicated mapping of Macaw's piece types (steep,
lower, top, attic) is deferred.

## 6. Question 5: safety and scale

### 6.1 Limits

| Tool | Default per-operation cap |
|---|---|
| WorldEdit | unlimited (`defaultChangeLimit = -1`), `calculationTimeout` 100 ms [S4-31] |
| FAWE | 50,000,000 changes, one concurrent action per player [S4-34] |
| Vanilla commands | `max_block_modifications` game rule, default 32768, minimum 1 (26.3 bytecode) [R3-L48] [R4-D19] |

Design decision: a configurable block limit per operation (default 32768, matching the vanilla rule) and a
span limit for the footprint search box, clamped on config load.

### 6.2 Placement across ticks

WorldEdit models side effects explicitly because neighbour updates and listeners are expensive [S4-33];
FAWE places chunk by chunk asynchronously with no blocks-per-tick key [S4-34]. Design decision: place a
queued plan in slices under a per-tick time budget, with `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE` (18, no
neighbour bit) and stair shapes computed up front [S4-35]. This is an inference; a GameTest must prove
every placed stair equals what vanilla `getStairsShape` computes for the final neighbourhood [S3-13]. The
queue pauses under `/tick freeze` (design decision).

### 6.3 Undo and redo

WorldEdit keeps a per-session history of 15 steps, drops the redo tail on a new action and records only
direct changes [S4-32]; FAWE persists history to disk for 7 days [S4-34]; roof-thing keeps 5 undo steps,
no redo, and skips blocks it no longer owns [R1-1] [R4-D17]. Design decision: per-player history (15
steps, in memory), redo cleared by a new action, and conflict-safe undo and redo: a position whose state no
longer equals the recorded "after" state is skipped and reported. Removal uses `UPDATE_SUPPRESS_DROPS`
[S4-35].

### 6.4 Permissions

fabric-permissions-api 0.7.0 supports 26.1 to 26.3 and is about 15 KB (15,050 bytes), bundled with
`include` [S4-28] [R3-L37]. Five `int` overloads are deprecated; each has a `PermissionLevel` twin such as
`require(String, PermissionLevel)` and `check(Entity, String, PermissionLevel)` [S4-29] [R3-L38]. Nodes per
command plus a node to bypass limits; checked on every wand use and command (design decision).

### 6.5 Protection

Common Protection API is a common front for claim mods, called through static methods on
`CommonProtection` and meant to be bundled [S4-18]; 2.0.0 offers `canPlaceBlock(Level, BlockPos, NameAndId,
@Nullable Player)`, `canBreakBlock` and `isAreaProtected(Level, AABB)` [S4-19]. Its last release is
2026-03-23, built against 26.1-rc-3, and predates 26.3 (2026-09-15) [S4-20] [R4-D18]. GOML bundles and
implements 2.0.0 on 26.3 [S4-21]; Flan registers a provider when CPA is loaded [S4-22]; OPAC has none and
no 26.x bridge was found [S4-23]; FTB Chunks is unverified [S4-24]. WorldEdit for Fabric checks no claims
[S4-26]. Fabric picks one CPA copy when GOML also bundles it, and a load-only test does not prove binary
linkage [R4-F8].

Design decisions:

- The smoke test calls `canPlaceBlock` on a real 26.3 server with a CPA provider loaded.
- Every target position is checked at preview and again at placement, plus `level.mayInteract` (as
  roof-thing does) and vanilla rules (spawn protection, world border, adventure mode, build height).
- Placement that continues after the player logs off keeps checking claims with the stored player name
  and id (`NameAndId`).
- Blocked positions show red in the preview and are skipped.

### 6.6 Consent before overwriting

Design decision: by default Roofwright places only into air and replaceable blocks (as roof-thing does
[R4-D17]); any other position is left alone and reported. A force option behind its own permission node
may replace ordinary blocks but never block entities or unbreakable blocks.

## 7. Question 6: performance plan and benchmark

**Planner cost.** The Chebyshev transform is O(cells) [S3-23]; enumerating maximal rectangles by row pairs
is about O(H^2 W) for an H by W box, and the count grows with outline noise (17 rectangles for 16 bumps on
one wall in the R4 simulation) [R4-F4] (complexity: author inference). The span limit bounds both. No
authoritative blocks-per-tick figure exists in WorldEdit or FAWE [S4-34].

**Benchmark (design decision).**

1. *Planner*: footprints from 16x16 to 256x256 (rectangle, L, U, noisy rectilinear), every style and
   pitch; warm-up, then repeated timed readings with median and p95; committed script and output.
2. *In-game placement*: a headless 26.3 dev server (port 25567, RCON 25577) driven over RCON places roofs
   of increasing size; the mod logs its own per-tick placement time and blocks per tick, and the harness
   reads those rather than coarse `/tick query`.
3. *Preview*: 1k, 2k (one bundle) and 5k ghosts captured on a real client, since no 26.x FPS figures exist
   [S4-13].

Targets until measured: a few milliseconds of placement work per 50 ms tick, and planning a 64x64
footprint within one tick (author inference).

## 8. Limitations and open questions

- **Stop rule**: no explicit owner reply yet (section 2.4) [LOG-1].
- **Unchecked sources**: BuiltByBit (403), Discord-only tools, the paid Arceon build, the MCTools sold
  build [R4-D8]; city generators such as CityWorld (S1 gaps).
- **Arceon** not tried; a tertiary review claims wall detection and roof types, contradicting the primary
  docs [S1-12] [S2-10].
- **roof-thing** read but not built or run [R4-D17].
- **CPA on 26.3** needs the `canPlaceBlock` smoke test; OPAC and FTB Chunks coverage absent or unverified
  [S4-23] [S4-24].
- **Display entities**: no 26.x FPS figures; outline recolouring unconfirmed [S4-13] [R3-L34].
- **Placement path**: `UPDATE_KNOWN_SHAPE` is an inference until a GameTest proves it [S4-35].
- **Macaw's corner rule** may differ from vanilla on neighbour updates (section 5) [R3-L14].
- **Gable composition** (4.3) is a design decision that rests on its property tests [R4-F4].
- **Scope decisions for v1**: courtyards covered; one wall-top level per operation (roof-thing supports
  per-section heights); no survival material cost (roof-thing has it [R1-1]); v1 targets creative
  builders and operators.
- **No standard gambrel or mansard angles**; they are presets [S3-4].

### Contradictions between sources

1. Arceon scope: primary docs [S1-9] [S2-8] against a tertiary review [S1-12] [S2-10], which the packs
   attribute to different sites. Primary docs win.
2. Arceon versions differ by listing; none shows 26.x [S1-13] [R3-L10].
3. Macaw's Roofs: Modrinth version 2.3.2 ships `mcw-roofs-2.3.3-mc26.3fabric.jar` [S2-5] [S4-37]; 633
   blockstate files, not 634 [R3-L11].
4. WorldEdit: tag 7.4.5 in S1 [S1-4], 7.4.6-beta-02 for 26.3 in S4 [S4-26].
5. "Only roof-thing" in GitHub search [S1-25] is wrong: llmbuilder [R3-L23] and MCTools [R4-D10] also
   surface.
6. Stairs wording versus bytecode agree once `facing` is the full-block side [S3-12] [S3-13].
7. CPA README says `modImplementation include`; Fabric 26.1 and GOML use `implementation include`
   [S4-7] [S4-21].
8. CityEngine `roofGable` documented convex-only yet shown on an L lot [S3-27].
9. Euclidean versus Chebyshev for hip roofs: Chebyshev is correct for rectilinear footprints [S3-23]
   [R4-F5].

## 9. References

All accessed 2026-10-08. Tier as assigned by the pack or review.

| Id | Title | URL | Published | Tier |
|---|---|---|---|---|
| R1-1 | howdoiusethissite/roof-thing (GitHub API metadata and README, lead-verified) | https://github.com/howdoiusethissite/roof-thing ; https://api.github.com/repos/howdoiusethissite/roof-thing | created 2026-10-06 | primary |
| LOG-1 | Roofwright gate log, delivery item G0 Intake ready (owner notification) | docs/sdlc-log.md | 2026-10-08 | primary (project record) |
| S1-1 | roof.js (WorldEdit contrib CraftScript) | https://github.com/EngineHub/WorldEdit/blob/master/contrib/craftscripts/roof.js | 2011; last change 2018-12-20 | primary |
| S1-3 | FAWE contrib craftscripts; WorldEdit docs "CraftScripts" | https://github.com/IntellectualSites/FastAsyncWorldEdit/tree/main/contrib/craftscripts ; https://worldedit.enginehub.org/en/latest/usage/other/craftscripts/ | undated | primary |
| S1-4 | GitHub code search, WorldEdit and FAWE | https://github.com/EngineHub/WorldEdit ; https://github.com/IntellectualSites/FastAsyncWorldEdit | FAWE 2.16.0 2026-10-04 | primary |
| S1-5 | WorldEdit docs "Generation" | https://worldedit.enginehub.org/en/latest/usage/generation/ | undated | primary |
| S1-6 | FastAsyncVoxelSniper; VoxelSniper-Reimagined | https://github.com/IntellectualSites/fastasyncvoxelsniper ; https://github.com/KevinDaGame/VoxelSniper-Reimagined | 2026-10-07 / 2024-09-30 | primary |
| S1-7 | Axiom Documentation | https://axiomdocs.moulberry.com/print.html | 2025-07-31 | primary |
| S1-8 | Modrinth API, Axiom versions | https://api.modrinth.com/v2/project/axiom/version | 2026-09-25 | primary |
| S1-9 | Arceon wiki: Roof-Tool | https://arceon.gitbook.io/arceon-wiki/tools/roof-tool | undated | primary |
| S1-12 | ARCEON PLUGIN Features + Review + How-to | https://premiumminecraft.com/arceon-plugin-overview-of-features-review/ | undated | tertiary |
| S1-13 | Arceon v0.4.1 Patreon post (series) | https://www.patreon.com/posts/arceon-v0-4-1-1-81659189 | 2023 | tertiary |
| S1-14 | Spiget search "roof" | https://api.spiget.org/v2/search/resources/roof?field=name | live | primary |
| S1-15 | Hangar API search "roof" | https://hangar.papermc.io/api/v1/projects?q=roof | live | primary |
| S1-17 | CurseForge Bukkit search via curse.tools | https://api.curse.tools/v1/cf/mods/search?gameId=432&classId=5&searchFilter=roof | live | primary |
| S1-19 | Roof Thing README | https://github.com/howdoiusethissite/roof-thing | 2026-10-06 | primary |
| S1-20 | Modrinth and GitHub API, roof-thing | https://api.modrinth.com/v2/project/roof-thing ; https://api.github.com/repos/howdoiusethissite/roof-thing | 2026-10-08 | primary |
| S1-21 | Minecraft Roof Designer & Generator (Pugtools) | https://pugtools.com/tools/roof-designer/ | 2026 | primary |
| S1-22 | Minecraft Roof Generator (CraftShape) | https://usecraftshape.com/minecraft-roof-generator | undated | primary |
| S1-23 | Jethz, VoxShaper, MCToolbox, dome roof generator | https://jethz.tools/roof/ ; https://voxshaper.com/ ; https://mctoolbox.net/roof ; https://www.minecraftcircle-generator.net/dome-roof-generator/ | undated | secondary |
| S1-24 | [Request] Roof Generator Plugin | https://bukkit.org/threads/request-roof-generator-plugin-formatted.83960/ | about 2012 (unverified) | tertiary |
| S1-25 | GitHub repository search | https://github.com/search?q=gable+roof+minecraft+in%3Areadme&type=repositories | 2026-10-08 | primary |
| S2-1 | Modrinth API v2 search (generator phrasings) | https://api.modrinth.com/v2/search?query=roof%20generator&limit=50&facets=[["project_type:mod"]] | live | primary |
| S2-2 | Modrinth API v2 search "roof" | https://api.modrinth.com/v2/search?query=roof&limit=50&facets=[["project_type:mod"]] | live | primary |
| S2-3 | CurseForge API search (generator phrasings) | https://api.curse.tools/v1/cf/mods/search?gameId=432&classId=6&searchFilter=auto%20roof&sortField=2&sortOrder=desc&pageSize=50 | live | primary |
| S2-4 | CurseForge API search "roof" | https://api.curse.tools/v1/cf/mods/search?gameId=432&classId=6&searchFilter=roof&sortField=2&sortOrder=desc&pageSize=50 | live | primary |
| S2-5 | Macaw's Roofs (Modrinth) | https://api.modrinth.com/v2/project/macaws-roofs ; https://modrinth.com/mod/macaws-roofs | 2026-09-29 | primary |
| S2-6 | Macaw's Roofs 26.3 Fabric jar inspection | https://modrinth.com/mod/macaws-roofs (mcw-roofs-2.3.3-mc26.3fabric.jar) | 2026-09-29 | primary |
| S2-8 | Roof-Tool, Arceon Wiki (GitBook and legacy GitHub wiki) | https://arceon.gitbook.io/arceon-wiki/tools/roof-tool ; https://github.com/ArcaniaxGit/Arceon-1.14/wiki/Roof-Tool | about 2025 | primary |
| S2-9 | Arceon Wiki home; Spiget search "arceon" | https://arceon.gitbook.io/arceon-wiki ; https://api.spiget.org/v2/search/resources/arceon?field=name | about 2026 | primary |
| S2-10 | Arceon Building Tools reviews | https://nullforums.net/resources/arceon-building-tools.3162/reviews | undated | tertiary |
| S2-11 | roof.js | https://github.com/EngineHub/WorldEdit/blob/master/contrib/craftscripts/roof.js | 2011 | primary |
| S2-12 | WorldEdit (Modrinth) | https://api.modrinth.com/v2/project/worldedit | 2026-09-24 | primary |
| S2-14 | Effortless Building | https://api.modrinth.com/v2/project/effortless-building | 2026-08-01 | primary |
| S2-15 | Build-Helper-Mod | https://api.modrinth.com/v2/project/build-helper-mod | 2026-03-16 | primary |
| S2-16 | Architectural Building Wand | https://api.curse.tools/v1/cf/mods/1467556/description | 2026-09-20 | primary |
| S2-17 | WallWand | https://api.curse.tools/v1/cf/mods/1599413/description | 2026-07-06 | primary |
| S2-18 | Building Gadgets / St'ructure Tools Continued | https://api.modrinth.com/v2/project/structure-tools-continued-(building-gadget) | 2026-04-22 / 2026-10-02 | primary |
| S2-19 | Construction Wand(s) (CurseForge search) | https://api.curse.tools/v1/cf/mods/search?gameId=432&classId=6&searchFilter=construction%20wand | 2023-10-22 / 2026-09-27 | primary |
| S2-20 | Litematica | https://api.modrinth.com/v2/project/litematica | 2026-09-27 | primary |
| S2-21 | Structurize / Domum Ornamentum | https://api.curse.tools/v1/cf/mods/search?gameId=432&classId=6&searchFilter=structurize | 2026-09-30 / 2026-09-01 | primary + tertiary |
| S2-24 | Web roof planners | https://usecraftshape.com/minecraft-roof-generator ; https://jethz.tools/roof/ ; https://minebuildr.com/tools/roof-designer ; https://minecraftcirclegenerator.app/minecraft-roof-generator | undated | secondary |
| S2-25 | Spiget and CurseForge Bukkit "roof" | https://api.spiget.org/v2/search/resources/roof?field=name&size=50 ; https://api.curse.tools/v1/cf/mods/search?gameId=432&classId=5&searchFilter=roof | live | primary |
| S2-26 | Name check, Modrinth and CurseForge | https://api.modrinth.com/v2/project/roofwright ; https://api.curse.tools/v1/cf/mods/search?gameId=432&slug=roofwright | live | primary |
| S2-27 | CurseForge search "wright" | https://api.curse.tools/v1/cf/mods/search?gameId=432&searchFilter=wright&pageSize=50 | live | primary |
| S2-28 | Web and GitHub search "Roofwright" | https://github.com/razekteixeira/roofwright | 2026-10-08 | secondary |
| S2-29 | Feel like no mod does roofs right (r/feedthebeast) | https://www.reddit.com/r/feedthebeast/comments/1mtk8f6/ | 2025-08-18 | tertiary |
| S2-30 | [Request] Roof Generator Plugin [Formatted] | https://bukkit.org/threads/request-roof-generator-plugin-formatted.83960/ | about 2012 (unverified) | tertiary |
| S2-31 | Automatic Roof Generator in Vanilla Minecraft (r/Minecraftbuilds) | https://arctic-shift.photon-reddit.com/api/posts/ids?ids=1eaow2d | 2024-07-24 | tertiary |
| S2-32 | Cone roof generator request (r/Minecraft) | https://arctic-shift.photon-reddit.com/api/posts/search?subreddit=Minecraft&title=roof%20generator | 2022-04-25 | tertiary |
| S3-1 | List of roof shapes | https://en.wikipedia.org/wiki/List_of_roof_shapes | 2026-10-03 | secondary |
| S3-2 | Hip roof | https://en.wikipedia.org/wiki/Hip_roof | 2026-07-22 | secondary |
| S3-3 | List of roof shapes (dutch gable, jerkinhead) | https://en.wikipedia.org/wiki/List_of_roof_shapes | 2026-10-03 | secondary |
| S3-4 | Gambrel | https://en.wikipedia.org/wiki/Gambrel | 2026-03-11 | secondary |
| S3-5 | Mansard roof | https://en.wikipedia.org/wiki/Mansard_roof | 2026-06-19 | secondary |
| S3-6 | Tented roof; List of roof shapes | https://en.wikipedia.org/wiki/Tented_roof | 2025-02-13 | secondary |
| S3-7 | List of roof shapes (shed, flat, cone, dome) | https://en.wikipedia.org/wiki/List_of_roof_shapes | 2026-10-03 | secondary |
| S3-8 | Eaves | https://en.wikipedia.org/wiki/Eaves | 2026-03-30 | secondary |
| S3-10 | Roof pitch | https://en.wikipedia.org/wiki/Roof_pitch | 2025-11-01 | secondary |
| S3-11 | Stairs (Block states) | https://minecraft.wiki/w/Stairs | revision 2026-09-18 | secondary |
| S3-12 | Stairs (Placement) | https://minecraft.wiki/w/Stairs | revision 2026-09-18 | secondary |
| S3-13 | `StairBlock` in minecraft-common-deobf-26.3.jar (javap) | local Loom cache | 2026-09-15 (26.3) | primary |
| S3-14 | Slab; `SlabType` in the 26.3 jar | https://minecraft.wiki/w/Slab | revision 2026-09-18 | secondary + primary |
| S3-16 | Tutorial:Roof types | https://minecraft.wiki/w/Tutorial:Roof_types | revision 2026-06-07 | secondary |
| S3-17 | GDPC tutorial: Adding a roof | https://github.com/avdstaaij/gdpc/blob/master/docs/source/getting-started/tutorial-house.md | undated | primary |
| S3-19 | Aichholzer et al., A Novel Type of Skeleton for Polygons, J.UCS 1(12) | https://lib.jucs.org/article_preview.php?id=27191 | 1995 | primary |
| S3-20 | Straight skeleton | https://en.wikipedia.org/wiki/Straight_skeleton | 2026-07-12 | secondary |
| S3-21 | Felkel and Obdrzalek, Straight skeleton implementation, SCCG'98 | https://www.sthu.org/misc/SKRW14/papers/Felkel_1998_StraightSkeletonImplementation.pdf | 1998 | primary |
| S3-22 | CGAL 2D Straight Skeleton manual; CGAL news | https://doc.cgal.org/latest/Straight_skeleton_2/index.html ; https://www.cgal.org/2023/05/09/improved_straight_skeleton/ | undated / 2023-05-09 | primary |
| S3-23 | Barequet, Eppstein, Goodrich, Vaxman, Straight Skeletons of Three-Dimensional Polyhedra | https://arxiv.org/abs/0805.0022 | 2008-04-30 | primary |
| S3-24 | Kelly and Wonka, Interactive Architectural Modeling with Procedural Extrusions | https://eprints.gla.ac.uk/48707/1/48707.pdf | 2011 | primary |
| S3-25 | Laycock and Day, Automatically generating roof models from building footprints | https://dspace5.zcu.cz/handle/11025/991 | 2003 | primary (metadata) |
| S3-27 | CityEngine CGA roofGable, roofHip | https://doc.arcgis.com/en/cityengine/latest/cga/cga-roof-gable.htm ; https://doc.arcgis.com/en/cityengine/latest/cga/cga-roof-hip.htm | undated (2026.0) | primary |
| S3-28 | Eppstein, Graph-Theoretic Solutions to Computational Geometry Problems | https://arxiv.org/abs/0908.3916 | 2009-08-26 | primary |
| S3-29 | Connected-component labeling | https://en.wikipedia.org/wiki/Connected-component_labeling | 2026-10-03 | secondary |
| S3-31 | Midpoint circle algorithm | https://en.wikipedia.org/wiki/Midpoint_circle_algorithm | 2026-06-23 | secondary |
| S3-32 | Salge et al., GDMC first-year report; founding paper | https://arxiv.org/abs/2103.14950 ; https://arxiv.org/abs/1803.09853 | 2021-03-27 / 2018 | primary |
| S3-33 | JoanaVR Minecraft-Settlement-Procedural-Generation, build_houses.py | https://github.com/JoanaVR/Minecraft-Settlement-Procedural-Generation/blob/HEAD/build_houses.py | undated | tertiary |
| S3-34 | Open-Ended Evolution for Minecraft Building Generation | https://arxiv.org/abs/2209.03108 | 2022 | primary |
| S3-35 | S3 agent analysis: 8-neighbour BFS on an L footprint | n/a | 2026-10-08 | analysis |
| S4-1 | Mojang version_manifest_v2 | https://piston-meta.mojang.com/mc/game/version_manifest_v2.json | live | primary |
| S4-4 | Removing obfuscation in Java Edition | https://www.minecraft.net/en-us/article/removing-obfuscation-in-java-edition | late 2025 | primary |
| S4-5 | 26.3 version JSON (piston-meta) | https://piston-meta.mojang.com/mc/game/version_manifest_v2.json | 2026-09-15 | primary |
| S4-6 | Fabric meta; Modrinth Fabric API versions | https://meta.fabricmc.net/v2/versions/game ; https://meta.fabricmc.net/v2/versions/loader ; https://api.modrinth.com/v2/project/fabric-api/version?game_versions=["26.3"] | 2026-10-06 | primary |
| S4-7 | fabric-permissions-api USAGE.md | https://github.com/lucko/fabric-permissions-api/blob/HEAD/USAGE.md | undated | primary |
| S4-8 | Display; Block Display (Minecraft Wiki) | https://minecraft.wiki/w/Display ; https://minecraft.wiki/w/Block_Display | undated | secondary |
| S4-9 | Block Display (Minecraft Wiki) | https://minecraft.wiki/w/Block_Display | undated | secondary |
| S4-10 | `Display` in the 26.3 jar (javap) | local Loom cache | 2026-09-15 (26.3) | primary |
| S4-11 | Entity format (Minecraft Wiki) | https://minecraft.wiki/w/Entity_format | undated | secondary |
| S4-12 | `Display`, `Display$BlockDisplay` in the 26.3 jar (javap -p) | local Loom cache | 2026-09-15 (26.3) | primary |
| S4-13 | MC-259879; MC-276285 | https://bugs-legacy.mojang.com/browse/MC-259879 ; https://bugs-legacy.mojang.com/browse/MC-276285 | 2023 / undated | primary tracker, tertiary evidence |
| S4-14 | Vanillin | https://www.curseforge.com/minecraft/mc-mods/vanillin-fly | undated | tertiary |
| S4-15 | Data component format | https://minecraft.wiki/w/Data_component_format | undated | secondary |
| S4-16 | `DataComponents` in the 26.3 jar (javap) | local Loom cache | 2026-09-15 (26.3) | primary |
| S4-17 | Data component format (History) | https://minecraft.wiki/w/Data_component_format | undated | secondary |
| S4-18 | Common Protection API README | https://github.com/Patbox/common-protection-api | undated | primary |
| S4-19 | CommonProtection.java | https://github.com/Patbox/common-protection-api | undated | primary |
| S4-20 | Nucleoid maven metadata, CPA | https://maven.nucleoid.xyz/eu/pb4/common-protection-api/maven-metadata.xml | 2026-03-23 | primary |
| S4-21 | Get Off My Lawn ReServed; Modrinth versions | https://github.com/Patbox/get-off-my-lawn-reserved ; https://api.modrinth.com/v2/project/goml-reserved/version | 2026-09-11 | primary |
| S4-22 | Flan (branch 26.3) | https://github.com/Flemmli97/Flan | 2026-09-18 | primary |
| S4-23 | CPA X OPAC listing; OPAC on Modrinth | https://www.modpackindex.com/mod/69907/cpa-x-opac | undated / 2026-10-05 | tertiary + primary |
| S4-24 | Common Bridge listings | https://moddex.gg/mod/common-bridge ; https://metamods.net/en/mods/common-bridge | undated | tertiary |
| S4-26 | worldedit-fabric source (7.4.x) | https://github.com/EngineHub/WorldEdit/tree/version/7.4.x/worldedit-fabric | 2026-09-24 | primary |
| S4-28 | fabric-permissions-api versions | https://api.modrinth.com/v2/project/fabric-permissions-api/version | 2026-05-27 | primary |
| S4-29 | fabric-permissions-api Permissions.java and USAGE.md | https://github.com/lucko/fabric-permissions-api/blob/HEAD/USAGE.md | undated | primary |
| S4-31 | WorldEdit configuration docs; LocalConfiguration.java | https://worldedit.enginehub.org/en/latest/config/ | undated | primary |
| S4-32 | WorldEdit history docs; LocalSession.java | https://worldedit.enginehub.org/en/latest/usage/general/history/ | undated | primary |
| S4-33 | WorldEdit SideEffect.java; Introducing WorldEdit 7.2 | https://madelinemiller.dev/blog/introducing-worldedit-72/ | undated | primary + secondary |
| S4-34 | FAWE Settings.java | https://github.com/IntellectualSites/FastAsyncWorldEdit/blob/main/worldedit-core/src/main/java/com/fastasyncworldedit/core/configuration/Settings.java | undated | primary |
| S4-35 | `Block` and `Level` in the 26.3 jar (javap -constants) | local Loom cache | 2026-09-15 (26.3) | primary |
| S4-37 | Macaw's Roofs Fabric versions | https://api.modrinth.com/v2/project/macaws-roofs/version?loaders=["fabric"] | 2026-09-29 | primary |
| R3-L9 | R3 ledger: Arceon distribution (wiki download links to Patreon; tertiary "premium" listing) | https://arceon.gitbook.io/arceon-wiki | undated | primary + tertiary |
| R3-L10 | R3 ledger: Arceon versions (nullforums listing, 0.5.4 for 1.20 to 1.21.10) | https://nullforums.net/resources/arceon-building-tools.3162/ | undated | tertiary |
| R3-L11 | R3 ledger: Macaw's Roofs 26.3 jar, 633 blockstate files, items Hammer and RoofItem | https://api.modrinth.com/v2/project/macaws-roofs/version?loaders=["fabric"] (mcw-roofs-2.3.3-mc26.3fabric.jar) | 2026-09-29 | primary |
| R3-L13 | R3 ledger: Macaw's Roofs downloads, CurseForge 104,631,282, Modrinth 7,976,313 | https://api.curse.tools/v1/cf/mods/search?gameId=432&slug=macaws-roofs ; https://api.modrinth.com/v2/project/macaws-roofs | as of 2026-10-08 | primary |
| R3-L14 | R3 ledger: Macaw's `RoofBlock` extends `Block`, declares FACING, HALF, SHAPE and its own getStairsShape | https://api.modrinth.com/v2/project/macaws-roofs/version?loaders=["fabric"] (jar inspection) | 2026-09-29 | primary |
| R3-L16 | R3 ledger: Pugtools page re-read (one pitch value shown, no changelog date, export-only) | https://pugtools.com/tools/roof-designer/ | undated | primary |
| R3-L20 | R3 ledger: roof-thing README, particle outline of sections | https://github.com/howdoiusethissite/roof-thing | 2026-10-06 | primary |
| R3-L22 | R3 ledger: roof-thing README, solid and protected blocks left alone | https://github.com/howdoiusethissite/roof-thing | 2026-10-06 | primary |
| R3-L23 | R3 ledger: jkuhta/llmbuilder (Fabric 1.21.11, distance-field roof planner) | https://github.com/jkuhta/llmbuilder | undated | primary |
| R3-L27 | R3 ledger: Bukkit.org request unverifiable (403, archive unreachable) | https://bukkit.org/threads/request-roof-generator-plugin-formatted.83960/ | about 2012 | unverified |
| R3-L32 | R3 ledger: data components re-checked (wiki and javap) | https://minecraft.wiki/w/Data_component_format | undated | secondary + primary |
| R3-L34 | R3 ledger: glow colour override field present, recolouring unconfirmed | local Loom cache; https://minecraft.wiki/w/Block_Display | 2026-09-15 (26.3) | primary + secondary |
| R3-L36 | R3 ledger: MC-276285 via mojira.dev mirror, Open, affects 1.21.1 to 1.21.4 | https://bugs-legacy.mojang.com/browse/MC-276285 | undated | primary tracker |
| R3-L37 | R3 ledger: fabric-permissions-api 0.7.0 game versions 26.1 to 26.3, jar 15,050 bytes | https://api.modrinth.com/v2/project/fabric-permissions-api/version | 2026-05-27 | primary |
| R3-L38 | R3 ledger: Permissions.java, five deprecated int overloads with PermissionLevel twins | https://github.com/lucko/fabric-permissions-api | undated | primary |
| R3-L48 | R3 ledger: `max_block_modifications` default 32768, minimum 1 (GameRules bytecode) | local Loom cache, minecraft-common-deobf-26.3.jar | 2026-09-15 (26.3) | primary |
| R3-L50 | R3 ledger: getStairsShape bytecode re-checked | local Loom cache | 2026-09-15 (26.3) | primary |
| R3-L54 | R3 ledger: straight skeleton as "the set of ridge lines of a building roof" | https://en.wikipedia.org/wiki/Straight_skeleton | 2026-07-12 | secondary |
| R3-L56 | R3 ledger: wiki mansard example about 16x20, buildings "need to be quite large" | https://minecraft.wiki/w/Tutorial:Roof_types | revision 2026-06-07 | secondary |
| R3-L57 | R3 ledger: BlockFamilies, BlockFamily, BlockFamily$Variant in the 26.3 common jar | local Loom cache | 2026-09-15 (26.3) | primary |
| R4-D2 | R4: Modrinth search "roof", "roofs", all project types (54 and 16 hits, no generator) | https://api.modrinth.com/v2/search?query=roof&limit=100 | live | primary |
| R4-D5 | R4: CurseForge mods, Bukkit plugins and data packs, generator phrasings | https://api.curse.tools/v1/cf/mods/search?gameId=432&classId=6&searchFilter=roof%20generator | live | primary |
| R4-D6 | R4: Effortless Structure description (Pyramid, Cone, Dome shape modes) | https://api.curse.tools/v1/cf/mods/1557540/description | undated | primary |
| R4-D7 | R4: Polymart search "roof" | https://api.polymart.org/v1/search?query=roof | live | primary |
| R4-D8 | R4: BuiltByBit search and MCTools listing (403) | https://builtbybit.com/resources/mctools.102917/ | undated | unverified |
| R4-D9 | R4: GitHub repository search; dynamic_framing | https://github.com/search?q=minecraft+roof&type=repositories ; https://github.com/BetterBuiltFool/dynamic_framing-1.20.1-fabric-forge | 2026-09-30 | primary |
| R4-D10 | R4: MCTools source, shapes/Roof.java (commit 2026-04-12) | https://github.com/PenguinStudiosOrganization/MCTools | 2026-04-12 | primary |
| R4-D11 | R4: Arnis, src/element_processing/buildings.rs (RoofType enum) | https://github.com/louis-e/arnis | pushed 2026-10-05 | primary |
| R4-D12 | R4: Axiom docs full tool list (Script Brush) | https://axiomdocs.moulberry.com/print.html | 2025-07-31 | primary |
| R4-D13 | R4: web search for Axiom roof scripts | https://openwebui.com/m/amz123/create-axiom-brush (not fetched) | undated | tertiary |
| R4-D17 | R4: roof-thing code read (RoofService.java, SelectionPreview.java, commits) | https://github.com/howdoiusethissite/roof-thing | 2026-10-06 | primary |
| R4-D18 | R4: Nucleoid maven metadata, CPA versions | https://maven.nucleoid.xyz/eu/pb4/common-protection-api/maven-metadata.xml | 2026-03-23 | primary |
| R4-D19 | R4: GameRules and BundlerInfo bytecode (32768; BUNDLE_SIZE_LIMIT 4096) | local Loom cache, minecraft-common-deobf-26.3.jar | 2026-09-15 (26.3) | primary |
| R4-F1 | R4 finding: stop rule read literally | docs/research-sceptic.md | 2026-10-08 | review |
| R4-F3 | R4 finding: roof-thing comparison | docs/research-sceptic.md | 2026-10-08 | review |
| R4-F4 | R4 finding and simulation (roofsim.py): hip equality 300 of 300; gable failure cases; rectangle count growth | docs/research-sceptic.md | 2026-10-08 | analysis |
| R4-F5 | R4 finding: 2D erosion argument and Chebyshev scope limits | docs/research-sceptic.md | 2026-10-08 | analysis |
| R4-F6 | R4 finding: packet-only ghost obligations | docs/research-sceptic.md | 2026-10-08 | review |
| R4-F8 | R4 finding: CPA linkage and offline placement | docs/research-sceptic.md | 2026-10-08 | review |
| R4-F9 | R4 finding: the wand must never be the authority | docs/research-sceptic.md | 2026-10-08 | review |
