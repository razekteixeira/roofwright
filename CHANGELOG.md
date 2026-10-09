# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0-beta.1] - 2026-10-09

First public release, for Minecraft 26.3 on Fabric.

### Added
- Outline detection from one wall-top block: rectangles, L, T, U and any right-angled outline;
  walls that touch only at corners still close; gaps in the top layer are reported.
- Ten roof styles: gable, hip, dutch gable, gambrel, mansard, pyramid, shed, flat with a parapet,
  cone and dome. Three pitches (1:2, 1:1, 2:1), overhang 0 to 3, ridge direction, shed side, slab
  or full ridge caps.
- Hips and valleys from the chessboard distance (the straight skeleton of right-angled outlines);
  gable wings that meet the main roof in valleys; gable walls filled with the wall's own block.
- Every stair placed with the corner shape Minecraft's own rule gives it, so nothing changes shape later.
- Materials from any member of a vanilla block family, any stair-like block from other mods
  (tested with Macaw's Roofs), or three blocks given explicitly.
- Wand built from a vanilla stick: right-click a wall top to preview, sneak + right-click to build,
  left-click to change style or pitch.
- Ghost preview with block display entities sent to the builder only; blocks in the way shown in red.
- Placement spread over ticks within a block and time budget; undo and redo that skip blocks
  changed since; `/roof cancel`.
- Safety: air only unless `force` (never block entities or unbreakable blocks), claims through the
  Common Protection API (bundled, place and break rights), spawn protection, adventure mode, world
  border, per-roof limits checked before any work, no chunk loading.
- Safe materials only: no block entities, fluids, unbreakable, operator or falling blocks, and the
  `#roofwright:forbidden` block tag (portals, fire, TNT, spawners and more) for roofs and gable walls.
- Wand click cooldown (`wandCooldownTicks`).
- `config/roofwright.json` with limits, budget, history, preview and permission settings, and
  `/roof reload`. Permission nodes through fabric-permissions-api (bundled), with vanilla level fallback.
