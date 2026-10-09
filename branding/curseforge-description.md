![Roofwright: whole roofs from the tops of your walls, in one click](https://razekteixeira.github.io/roofwright/media/banner.png)

# Roofwright

**Whole roofs from the tops of your walls, in one click.**

Roofs are the slow part of every build: hundreds of stairs and slabs, and hips, valleys and corners that are easy to get wrong. Roofwright builds the whole roof for you. Right-click the top of a wall with the wand: it finds the building's outline, shows you a ghost of the roof, and builds it when you sneak-click. Undo puts everything back.

Server-side only. Players join with a vanilla client.

![Bare walls](https://razekteixeira.github.io/roofwright/media/before.png)

![The same village, one click per house](https://razekteixeira.github.io/roofwright/media/after.png)

## Features

- **Finds the outline itself.** One click on a wall top: rectangles, L, T and U shapes and any other right-angled outline. No marking corners by hand.
- **Ten styles:** gable, hip, dutch gable, gambrel, mansard, pyramid, shed, flat with a parapet, cone and dome.
- **Three pitches** (1:2 with slabs, 1:1 with stairs, 2:1 steep) and an **overhang** of 0 to 3 blocks.
- **Corners done right.** Hips get outer corner stairs, valleys get inner corners, and every stair gets the exact shape Minecraft's own rule gives it, so nothing reshapes after it is placed.
- **Wings meet properly.** On L and T houses the smaller wing's ridge runs into the main roof and ends in a valley; a small bay gets its own little cross gable.
- **Gable walls filled** with the block your walls are made of.
- **Any material:** any member of a block family (oak, spruce, deepslate tiles, bricks, blackstone...), stair-like blocks from other mods such as **Macaw's Roofs**, or three blocks of your choice.
- **Ghost preview** that only you can see; blocks in the way show in red.
- **Undo and redo** that leave alone anything someone changed in the meantime.
- **Safe on servers:** fills air only unless you ask for `force` (which still never replaces chests or bedrock), respects claim mods (Common Protection API: GOML, Flan and others, including their break rights), spawn protection and adventure mode, never builds with portals, containers, fluids, TNT or other unsafe blocks (`#roofwright:forbidden`, extendable with a data pack), and places a budgeted number of blocks per tick.

![Ghost preview, then the roof going up](https://razekteixeira.github.io/roofwright/media/grow.gif)

## How to use it

1. `/roof wand` gives you the Roofwright Wand.
2. **Right-click the top block of a wall.** The outline is detected and a ghost roof appears.
3. Change anything and the ghost updates: `/roof style hip`, `/roof pitch steep`, `/roof overhang 2`, `/roof material deepslate_tiles`. Left-click cycles the style, sneak + left-click the pitch.
4. **Sneak + right-click** (or `/roof place`) builds it. `/roof undo` takes it back.

What you see in chat, from the capture run behind these screenshots:

```
Hip, 1:1, overhang 1 roof in spruce_stairs: 123 blocks over 79 columns. Sneak + right-click with the wand or /roof place to build.
```

![The ghost preview with a block in the way in red](https://razekteixeira.github.io/roofwright/media/preview.png)

## Gallery

Every screenshot is a real capture from the game client, made by the project's capture test.

![Six styles on one house](https://razekteixeira.github.io/roofwright/media/styles.png)

![Hips and valleys on an L-shaped house](https://razekteixeira.github.io/roofwright/media/gallery-valleys.png)

![A gambrel barn](https://razekteixeira.github.io/roofwright/media/gallery-barn.png)

![A cone on a round tower](https://razekteixeira.github.io/roofwright/media/gallery-tower.png)

## Commands

`/roof` and the alias `/roofwright` are the same command.

| Command | What it does |
|---|---|
| `wand` | Gives you the wand |
| `detect [pos]` | Detects the building whose wall top you look at (or at `pos`) and previews its roof |
| `select <from> <to>`, `select add <from> <to>` | Selects rectangles by hand |
| `style <style>` | gable, hip, dutch_gable, gambrel, mansard, pyramid, shed, flat, cone, dome |
| `pitch low\|normal\|steep` | 1:2, 1:1 or 2:1 |
| `overhang <0-3>` | How far the roof reaches past the walls |
| `material <block> [<slab> <full>]` | Material from any member of a block family, or three blocks |
| `ridge auto\|x\|z`, `shed <side>`, `ridgecap slab\|full`, `gable match\|<block>` | Fine tuning |
| `preview` | Shows the ghost again |
| `place [force]` | Builds the roof; `force` also replaces plain blocks in the way |
| `undo`, `redo`, `cancel` | Takes your last roof back, puts it back, or stops one still going up |
| `info`, `help`, `reload` | Your settings and history, the command list, re-read the config |

## For server owners

- **Permissions:** with a permissions mod such as LuckPerms, grant `roofwright.use`, `roofwright.force`, `roofwright.unlimited` and `roofwright.admin`. Without one, the operator levels from `config/roofwright.json` apply (defaults 2, 2, 3, 3). Holding a wand grants nothing; every click is checked.
- **Limits** in `config/roofwright.json`: `maxSpan` (widest outline, 96), `maxBlocks` per roof (30,000), `blocksPerTick` (2,000) and `millisPerTick` (5) shared by everyone's roofs, `historySize` (10), `previewLimit` (3,000), `previewSeconds` (300) and `wandCooldownTicks` (5). Applied with `/roof reload`. Grant `roofwright.use` to trusted builders.
- **Performance:** planning a 256 by 256 hip roof takes about 18 ms; while a 16,900-block roof goes up, the worst tick stays at the 5 ms budget (measured on an Apple M-series Mac with the repository's benchmark scripts).
- History and previews live in memory; a server restart clears them.

## Install

Needs Minecraft **26.3**, Fabric Loader 0.19.5 or newer, **Fabric API** and Java 25. Put the jar and Fabric API in the `mods/` folder of your server or your Fabric 26.3 profile.

## Links

- Website: https://razekteixeira.github.io/roofwright/
- Source and issues: https://github.com/razekteixeira/roofwright

Open source under the Apache License 2.0. Not an official Minecraft product; not approved by or associated with Mojang or Microsoft.
