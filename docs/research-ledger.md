# Roofwright: claim ledger (R3 Claim Verification)

Gate: G4 Claims with teeth (research track). Verifier: R3 Claim Verification agent, fresh context,
2026-10-08. Input: `docs/research.md` (draft) and the URLs in its References section only; no evidence
packs or notes were read. Every source below was re-fetched on 2026-10-08 (Modrinth API, CurseForge via
api.curse.tools, Spiget, Hangar, GitHub API and raw files, Fabric meta, Mojang piston-meta, Nucleoid
maven, minecraft.wiki API, arXiv PDFs, the local 26.3 Loom jar with `javap`, and the Macaw's Roofs
26.3 Fabric jar downloaded from Modrinth).

Verdict scale (token-boundary strictness: scope, version and date must match):
**supported**, **partial** (true in part, or true but cited to the wrong source, or wider than the source),
**unsupported** (the source could not be retrieved or does not say it), **contradicted** (the source
says otherwise). "Single-source" marks a load-bearing claim that rests on one origin only. "No as-of"
marks a volatile fact (version, date, count) stated in the report body without an as-of date; the
References table gives a blanket "accessed 2026-10-08", which covers the table but not prose that will be
copied into the spec.

## Ledger

| Id | Claim (short) | Citations | Verdict | Note |
|---|---|---|---|---|
| L1 | No Modrinth or CurseForge mod generates a stair-and-slab roof from a footprint | S2-1, S2-2, S2-3, S2-4, S1-16 | supported | Modrinth "roof generator" (mods) 0 hits; "roof" 54 hits, all roof blocks, Nether-roof mods or resource packs. CurseForge "auto roof" 0, "roof generator" 1 (an unrelated cavern world generator), "roof" 61, all roof blocks or unrelated. Keyword search only; as of 2026-10-08. |
| L2 | No SpigotMC, Hangar or CurseForge Bukkit plugin generates roofs | S1-14, S1-15, S1-17, S2-25 | supported | Spiget "roof" 29 results, Hangar 12, CurseForge Bukkit 11: all Nether-roof blockers, waterproofing or unrelated. As of 2026-10-08. |
| L3 | WorldEdit `roof.js` (2011, Bentech) places one-block rings of one block type, one level higher per ring | S1-1, S2-11 | supported | Source: "Copyright (c) 2011 Bentech"; loop sets `blocktype` on the ring at `getMaximumPoint().getY() + c`. Last change 2018-12-20 confirmed by commit history. |
| L4 | WorldEdit has no roof command or brush in its code | S1-4 | supported | GitHub code search for "roof" in EngineHub/WorldEdit returns only two CHANGELOG lines about roof.js. Code search covers the default branch only. |
| L5 | FAWE ships the same roof.js and has no roof brush; FAWE 2.16.0 released 2026-10-04 | S1-3, S1-4 | supported | `contrib/craftscripts` lists roof.js; code search "roof" finds only comments ("cave roof", "roofed forest"); release 2.16.0 published 2026-10-04. The Rhino and `/cs` detail was not re-checked. |
| L6 | FastAsyncVoxelSniper has no roof brush; VoxelSniper Reimagined last pushed 2024-09-30 | S1-6 | supported | Code search "roof" in fastasyncvoxelsniper returns nothing; Reimagined `pushed_at` 2024-09-30T18:05:49Z. The "about 80 brushes" count was not re-counted. |
| L7 | Axiom 6.1.3 for 26.3 (2026-09-25), Fabric; 251 version changelogs never mention roof or gable; only roof mention in docs is a Smear demo | S1-7, S1-8, S2-13 | supported | Modrinth: 251 versions, latest 6.1.3 for 26.3 published 2026-09-25, loader fabric; regex roof or gable over all changelogs: 0. Docs: "The player builds a roof by smearing two rows of blocks." No as-of in the table row beyond the version date. |
| L8 | Arceon `//roof <block> <width> [height]` extrudes a spike shape along a convex or cuboid selection with noise, bevel, hollow and slab flags; no documented stair facing, hips, valleys, styles or wall detection | S1-9, S2-8 | supported | Wiki: "Uses convex and cuboid selections", flags -n noise to the spike shape, -d, -b bevel, -h hollow, -p straightens the path; "Use any slab pattern"; no stairs, hips, valleys, styles or wall detection mentioned. |
| L9 | Arceon is paid | S1-9, S1-13, S2-9 | partial | Wiki home gives no price; download links go to Patreon posts; the Patreon post returned 403. Only a tertiary listing calls it "premium". Single-source (tertiary). |
| L10 | Arceon claims support up to 1.21.10, no 26.x evidence | S1-13 | partial | The cited Patreon post returned 403. Wiki home lists "Arceon Fawe v0.5.5 [1.20+]" and "Arceon x Axiom v0.1.1 [1.20+]". The 1.21.10 figure appears only in a nullforums listing (0.5.4, 1.20 to 1.21.10), which is tertiary. "No 26.x evidence" holds. Single-source (tertiary). |
| L11 | Macaw's Roofs has no generator, wand or blueprint item; one `roofing_hammer`; 634 blockstates | S2-5, S2-6 | partial | 26.3 Fabric jar: items are `Hammer` (roofing_hammer) and `RoofItem`; no wand or blueprint. The jar has 633 blockstate JSON files (634 zip entries including the directory entry). |
| L12 | Macaw's Roofs supports 26.3 on Fabric (2026-09-29); Modrinth version 2.3.2 ships jar `mcw-roofs-2.3.3-mc26.3fabric.jar` | S2-5, S4-37 | supported | Modrinth version 2.3.2, published 2026-09-29, game version 26.3, file `mcw-roofs-2.3.3-mc26.3fabric.jar`; loaders fabric, forge, neoforge. |
| L13 | Macaw's Roofs has 104.6M CurseForge downloads | S2-5 | partial | Figure is right (CurseForge API: 104,631,282 on 2026-10-08) but S2-5 is the Modrinth project, which shows 7,976,313 downloads. Wrong citation and no as-of date. |
| L14 | Macaw's roof blocks are not stairs and need a dedicated mapping; modded stairs are detected by the `facing`, `half`, `shape` properties | S3-11, S3-14, S4-37, S2-5 (design decision) | contradicted | Jar inspection: `RoofBlock` extends `Block` (not `StairBlock`) but declares `FACING`, `HALF` and `SHAPE` (`StairsShape`) and its own `getStairsShape`. A property-based detector would classify Macaw's roof blocks as stairs, so the two statements conflict. |
| L15 | WorldEdit runs on Fabric, Forge, NeoForge, Quilt and Bukkit; 7.4.6-beta-02 for 26.3 on 2026-09-24 | S2-12, S4-26 | supported | Modrinth loaders include fabric, forge, neoforge, quilt, bukkit (and paper, spigot, folia); 7.4.6-beta-02 for 26.3 published 2026-09-24 for fabric and neoforge and for Bukkit family. |
| L16 | Pugtools: 16 styles, rectangle or L footprint, pitch, per-side overhang, 3D stair and slab preview, Litematica or material list export, cannot read or place into a world; changelog 2026-07-07 | S1-21, S2-23 | partial | Styles, footprints, per-side overhang, 3D preview and exports all confirmed. The page links a changelog but shows no date, so "2026-07-07" is not visible on the cited page; only one pitch value (1:1) is shown. "Cannot read or place" is inferred from export-only wording. |
| L17 | CraftShape: gable, hip, pyramid, shed, A-frame; rectangles only; "split irregular buildings into rectangles" | S1-22, S2-24 | supported | Styles match; width and length inputs only; FAQ: "Split the building into rectangles, generate a roof for each, and join them at the ridges." Quote in the report is a paraphrase, not verbatim. |
| L18 | roof-thing: created 2026-10-06, last push about 20 minutes later, 2 commits, 0 stars, 0 forks, description "e", CC0-1.0, not on Modrinth or CurseForge | R1-1, S1-20 | supported | GitHub API: created 00:01:19Z, pushed 00:21:54Z, 2 commits, 0 stars, 0 forks, description "e", licence CC0-1.0. Modrinth `/project/roof-thing` 404; absent from CurseForge "roof" results. Volatile, as of 2026-10-08. |
| L19 | roof-thing features: wand marks wall-top corner pairs; sections merged by highest value; valleys with inner corner stairs; styles gable, gable_rotated, hip; odd widths slab ridge; overhang 0 to 3; `/roof undo` last 5 with materials returned; survival needs stairs and slabs; gable walls not filled; selection in memory; test against vanilla stair shapes | S1-19, R1-1 | supported | README states each point almost verbatim. Single-source (README only; the code was not built or run, as the report says). |
| L20 | roof-thing has no preview | S1-19 | partial | README: "Green particles outline the section" after marking corners. There is a selection outline, but no preview of the roof itself. |
| L21 | roof-thing has undo only, no redo | R1-1 | supported | Command list: build, undo, clear, style, overhang, info; no redo. |
| L22 | roof-thing reports any non-air position as blocked and leaves it alone | R1-1 | partial | README: "Existing solid blocks are left alone, as is anything protected from you." It says solid (not non-air) and does not say the positions are reported. |
| L23 | GitHub-wide searches for roof generators surfaced only roof-thing | S1-25 | partial | The cited search (`gable roof minecraft in:readme`) also returns jkuhta/llmbuilder, a Fabric mod for 1.21.11 that generates whole buildings with a "distance-field roof planner" and roof types gable, hip, mansard, flat and stepped gable, plus other procedural or AI building generators. None takes a player's own footprint, but the "only" is wrong. |
| L24 | Name `roofwright`, `roof-wright`, `roofwrights` free on Modrinth and CurseForge; control slug `macaws-roofs` returns 1 | S2-26 | supported | Modrinth project lookups 404 for all three; Modrinth search 0 hits; CurseForge slug search 0 for all three, control 1; text search 0. As of 2026-10-08. |
| L25 | The only GitHub repository named Roofwright is the owner's | S2-28 | supported | GitHub repository search finds `razekteixeira/roofwright` only. Single-source; volatile, as of 2026-10-08. |
| L26 | r/feedthebeast 1mtk8f6 (2025-08-18, score 52, 15 comments) complains about roof block undersides; replies recommend Domum Ornamentum, Macaw's with Framed Blocks or Copycats+, vanilla; nobody asks for a generator | S2-29 | supported | Archive (arctic-shift): created 2025-08-18, score 52, 15 comments; selftext about the "wall" part of roof blocks; comments name Framed Blocks, Copycats+, Domum Ornamentum, vanilla building. No generator request. Score is volatile (archive snapshot). |
| L27 | Bukkit.org request (about 2012) to click points around a house and generate a normal, gable or hip roof; only reply pointed to WorldEdit scripting | S1-24, S2-30 | unsupported | Page returns 403; archive.org unreachable from this environment. Content and date cannot be verified. Single-source. |
| L28 | 2024 command-block "Automatic Roof Generator" post (score 1); 2022 cone roof generator request | S2-31, S2-32 | supported | 1eaow2d: r/Minecraftbuilds, 2024-07-24, score 1. ubbc7s: r/Minecraft, 2022-04-25, asks for a cone generator tool. |
| L29 | Minecraft 26.3 released 2026-09-15, needs Java 25, ships unobfuscated | S4-1, S4-4, S4-5 | supported | piston-meta: 26.3 releaseTime 2026-09-15T11:23:02Z, javaVersion majorVersion 25; downloads list only client and server (no mappings). The obfuscation article (S4-4) was not re-fetched. |
| L30 | Fabric loader 0.19.5 and Fabric API 0.162.0+26.3 support 26.3 | S4-6 | supported | Fabric meta: loader 0.19.5 stable, game 26.3 stable; Modrinth Fabric API 0.162.0+26.3 published 2026-10-06. No as-of in section 5 prose. |
| L31 | From 26.1, dependencies use `implementation` instead of `modImplementation` | S4-7 | supported | USAGE.md: "As of 26.1, use `implementation` instead of `modImplementation`". |
| L32 | `custom_data` is ignored by the game; `item_name` is anvil-proof and not italic unlike `custom_name`; a missing `item_model` renders the missing model; constants exist in the 26.3 jar | S4-15, S4-16, S4-17 | supported | Wiki: "custom data not used by the game", item_name "cannot be erased using an anvil" and "is not italicized", nonexistent model "will cause the missing model to be used". javap: CUSTOM_DATA, ITEM_NAME, CUSTOM_NAME, LORE, ITEM_MODEL, ENCHANTMENT_GLINT_OVERRIDE all present. |
| L33 | Block displays exist since 1.19.4, have no hitbox or collision | S4-8, S4-9 | supported | Wiki: "Display entities have no hitbox", "have no collision", added in 1.19.4 (23w06a). |
| L34 | `glow_color_override` recolours the Glowing outline per entity | S4-10, S4-11 | partial | The 26.3 jar has `Display.setGlowColorOverride(int)` and the tag; the Block Display wiki page does not mention recolouring, and the Entity format page (S4-11) was not re-fetched within budget. Behaviour unconfirmed by this pass; prove it in the capture run. |
| L35 | Display setters are private in 26.3 | S4-12 | supported | javap -p: `setTransformation`, `setViewRange`, `setShadowRadius`, `setGlowColorOverride`, `setBrightnessOverride` and `BlockDisplay.setBlockState` are all private. |
| L36 | MC-276285 (unresolved): 4,096 item displays riding one entity; doubling made MSPT and FPS unplayable | S4-13 | supported | Via the mojira.dev mirror (bugs-legacy refused connection): status Open, "MSPT and FPS degraded to unplayable levels"; affects 1.21.1, 24w36a, 1.21.4. Not reported on 26.x. |
| L37 | fabric-permissions-api 0.7.0 supports 26.1.1 to 26.3 and is about 12 KB | S4-28 | partial | Modrinth: 0.7.0 (2026-05-27) game versions 26.1, 26.1.1, 26.1.2, 26.2, 26.3; jar 15,050 bytes (about 15 KB). |
| L38 | `Permissions.check` and `require`; five overloads deprecated; `PermissionLevel` overloads preferred; exact signatures not read | S4-29 | supported | Permissions.java: exactly 5 `@Deprecated` methods, all taking `int defaultRequiredLevel`; each has a `PermissionLevel` twin, e.g. `require(String, PermissionLevel)`, `check(SharedSuggestionProvider, String, PermissionLevel)`, `check(Entity, String, PermissionLevel)`. |
| L39 | LuckPerms for Fabric supports 26.3 | S4-30 | supported | Modrinth v5.5.85-fabric (2026-09-19) lists 26.3. |
| L40 | Common Protection API is a common front for claim mods; static methods on `CommonProtection`; meant to be bundled; README uses `modImplementation include` | S4-18 | supported | README: "one single common api for checking against multiple mods", "call static methods in ... CommonProtection", "This will also include it in yours mods". |
| L41 | CPA 2.0.0 offers `canPlaceBlock(Level, BlockPos, NameAndId, Player)`, `canBreakBlock`, `isAreaProtected(Level, AABB)` | S4-19 | supported | CommonProtection.java has these signatures; the `Player` parameter is `@Nullable`. |
| L42 | CPA built against 26.1-rc-3 and has no 26.3 build | S4-20 | supported | gradle.properties `minecraft_version=26.1-rc-3`, `mod_version = 2.0.0`; maven versions 1.0.0 and 2.0.0 only, lastUpdated 2026-03-23. |
| L43 | GOML bundles and implements CPA 2.0.0 on 26.3 | S4-21 | supported | build.gradle `implementation include("eu.pb4:common-protection-api:2.0.0")`; Modrinth 1.22.0+26.3-rc-1 (2026-09-11) lists 26.3; built against `minecraft_version=26.3-rc-1`. Indirect evidence for 26.3 binary compatibility only. |
| L44 | Flan registers a CPA provider whenever CPA is loaded | S4-22 | supported | Branch 26.3, FlanFabric: `commonProtApi = isModLoaded("common-protection-api")`, then `FlanProtectionProvider.register()`. |
| L45 | WorldEdit for Fabric hooks `AttackBlockCallback` and `UseBlockCallback` and has no claim integration | S4-26 | supported | version/7.4.x FabricWorldEdit.java registers AttackBlockCallback, UseBlockCallback and UseItemCallback; code search "claim" finds only licence text. |
| L46 | WorldEdit defaults: `defaultChangeLimit = -1`, `calculationTimeout` 100 ms; history 15 steps, new action drops the redo tail, records only direct changes | S4-31, S4-32 | supported | LocalConfiguration: `defaultChangeLimit = -1`, `calculationTimeout = 100`; LocalSession: `MAX_HISTORY_SIZE = 15`, entries past `historyPointer` removed on remember; docs: "WorldEdit only records direct changes". |
| L47 | FAWE: 50,000,000 changes, one concurrent action, history deleted after 7 days, no blocks-per-tick key | S4-34 | supported | Settings.java: `MAX_CHANGES = 50000000`, `MAX_ACTIONS = 1`, history `DELETE_AFTER_DAYS = 7` (a second `DELETE_AFTER_DAYS = 1` belongs to another section). |
| L48 | `max_block_modifications` exists; default "believed 32768, unverified" | S4-36 | partial | Source supports the name only. The 26.3 bytecode registers `max_block_modifications` with default 32768 (minimum 1), so the value is verifiable and the "unverified" hedge can go. |
| L49 | `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE` = 18 with no neighbour bit; `UPDATE_SUPPRESS_DROPS` exists | S4-35 | supported | javap -constants: UPDATE_NEIGHBORS 1, UPDATE_CLIENTS 2, UPDATE_KNOWN_SHAPE 16, UPDATE_SUPPRESS_DROPS 32. |
| L50 | `StairBlock.getStairsShape` rule: front check gives OUTER (left if g == f.ccw), then back check gives INNER; `canTakeShape` false only for a same-facing, same-half stair; shape recomputed only for horizontal neighbour changes | S3-13 | supported | Bytecode matches each step, including `canTakeShape(state, level, pos, g.getOpposite())` for outer and `(…, g)` for inner, and the `isHorizontal` guard in `updateShape`. |
| L51 | Stairs states: `facing` is the full-block side; half-block side touching gives inner, full-block side gives outer; upright and upside-down never join | S3-11, S3-12 | supported | Wiki: facing "The direction the stairs' full-block side faces"; inner and outer corner wording as stated; "Right side up stairs do not join with upside-down stairs". |
| L52 | Barequet et al.: offset surface at time t is the set at L-infinity distance t; polycube skeleton by a voxel sweep, one L-infinity unit per round; "significantly easier to compute for orthogonal inputs" | S3-23 | supported | Full text contains each quoted statement (3D polyhedra and polycubes). |
| L53 | Applied to a 2D rectilinear footprint, the 45-degree hip height is the Chebyshev distance to the outside, computed by an 8-neighbour multi-source BFS | S3-23 | partial | The paper treats 3D orthogonal polyhedra; it does not state the 2D roof application or an 8-neighbour BFS. The step is the report's inference, cited as if from the source. |
| L54 | Straight skeleton: Felkel and Obdrzalek incorrect; Huber-Held O(nr log n), Eppstein-Erickson O(nr + n log n); skeleton arcs are the roof's ridges, hips and valleys | S3-20 | partial | Bounds and "algorithm is incorrect" confirmed. The article calls the skeleton "the set of ridge lines of a building roof" and does not mention hips or valleys. |
| L55 | Minimum rectangle partition needs n/2 + h - g - 1 rectangles, polynomial via bipartite matching | S3-28 | supported | Body text: "n/2 + h − g − 1, where g is the maximum size of a set of disjoint good diagonals", via the bipartite intersection graph of good diagonals. Not in the abstract. |
| L56 | Wiki roof guide: simple gable "not suitable ... greater than about 12 blocks"; mansards need about 16x20; helm "very fiddly"; 1-in-1 and 2-in-1 skillion; slab layer at the peak; flat roof border of slabs, backwards stairs or parapet | S3-16 | partial | All confirmed except mansard size: 16x20 is the size of the example building ("about 16×20 meters"), not a minimum; the page only says buildings "need to be quite large". |
| L57 | `BlockFamilies`, `BlockFamily`, `BlockFamily$Variant` are in the 26.3 common jar | (author verification) | supported | `unzip -l` on the 26.3 common jar lists all three under `net/minecraft/data/`. |

## Origin independence

Single-source load-bearing claims: L9 and L10 (Arceon price and versions, tertiary only), L19 (all
roof-thing capabilities rest on its README; code not built), L25 (GitHub name search), L27 (Bukkit
request, unretrievable), L34 (glow recolour, wiki only, not reconfirmed), L36 (one tracker issue, read
through a mirror), L43 (CPA on 26.3 rests on GOML alone). All other load-bearing claims have a primary
source re-checked here (API, code or jar) or two agreeing origins.

## Recency flags

Volatile facts in the report body without an as-of date: Macaw's Roofs downloads (section 2.2),
Fabric loader and API versions (section 5), WorldEdit, Axiom and Macaw's version rows in the table in
section 2.1 (dated by release, not by check), roof-thing stars and forks (sections 1 and 2.4), the
Reddit score (section 2.2) and the name check results (section 2.3). Add "as of 2026-10-08" to each.

## Required fixes

1. **L9 (Arceon paid)**: "Arceon's downloads are distributed through Patreon posts; a tertiary listing
   describes it as premium. Its price was not confirmed (Patreon returned 403)."
2. **L10 (Arceon versions)**: "The Arceon wiki lists Arceon Fawe v0.5.5 and Arceon x Axiom v0.1.1 as
   '[1.20+]'; a tertiary listing gives 0.5.4 for 1.20 to 1.21.10. No 26.x build is documented." Cite the
   wiki home (S2-9) for the first sentence and mark the second tertiary.
3. **L11 (Macaw's blockstates)**: "633 blockstate files" instead of "634 blockstates".
4. **L13 (Macaw's downloads)**: "Macaw's Roofs has 104.6M CurseForge downloads and 8.0M Modrinth
   downloads (as of 2026-10-08)", citing the CurseForge API for the first figure; S2-5 supports only the
   Modrinth figure.
5. **L14 (Macaw's roof blocks)**: "Macaw's roof blocks do not extend `StairBlock`, but they carry the same
   `facing`, `half` and `shape` properties and compute their own stair shape, so a detector keyed on those
   properties would treat them as stairs. Detection must check the block class (or an allow list), and
   Macaw's roofs need a dedicated mapping." (Source: jar inspection of mcw-roofs-2.3.3-mc26.3fabric.jar.)
6. **L16 (Pugtools)**: drop "(changelog 2026-07-07)" or cite the changelog page itself; say "pitch
   setting" rather than implying several pitches are shown, and mark "cannot read or place into a world"
   as inferred from its export-only workflow.
7. **L20 (roof-thing preview)**: "roof-thing outlines marked sections with particles but has no preview
   of the roof before building."
8. **L22 (roof-thing overwrite)**: "roof-thing leaves existing solid blocks and protected positions
   alone"; drop "non-air" and "reported" for roof-thing.
9. **L23 (GitHub search)**: "GitHub searches surfaced roof-thing as the only tool that roofs a player's
   own build; procedural building generators such as jkuhta/llmbuilder (Fabric, 1.21.11) generate whole
   buildings with gable, hip and mansard roofs but do not take an existing footprint." The stop-rule
   assessment should mention this class of tool next to GDMC.
10. **L27 (Bukkit request)**: mark as "unverified (page returns 403)" in the bullet itself, not only in
    the date, and do not count it as a demand signal without that label.
11. **L34 (glow colour)**: "The 26.3 Display entity has a `glow_color_override` field; per-entity
    outline colour is to be confirmed in the client capture run."
12. **L37 (permissions API)**: "fabric-permissions-api 0.7.0 supports 26.1 to 26.3 and is about 15 KB
    (15,050 bytes)."
13. **L48 (`max_block_modifications`)**: "The `max_block_modifications` game rule defaults to 32768
    (26.3 bytecode, minimum 1)"; remove "believed" and "unverified" in sections 6.1 and 8.
14. **L53 (Chebyshev BFS)**: label the 2D application as author inference: "Barequet et al. show the
    L-infinity offset for 3D orthogonal polyhedra; by analogy (author inference), the 45-degree hip
    height on a rectilinear footprint is the Chebyshev distance to the outside, computed by an
    8-neighbour multi-source BFS."
15. **L54 (straight skeleton)**: "skeleton arcs are the ridge lines of the roof [S3-20]"; attribute hips
    and valleys to S3-2, not S3-20.
16. **L56 (mansard size)**: "the wiki's mansard example is about 16x20 and it says buildings need to be
    quite large before mansards look right."
17. **Recency**: add "as of 2026-10-08" to the volatile facts listed under Recency flags.
