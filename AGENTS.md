# Source Code Agent Guide

This repository contains the source code for Roofwright. The sibling vault
repository owns the project authority map and durable agent-facing synthesis.
The binding project, engineering, toolchain and release rules are in the
"Roofwright" sections at the end of this file; they apply to every agent (Claude
Code, Codex, Cursor, others).

The vault is local only (no remote). Cloud sessions and worktrees outside
`personal/roofwright/` will not find it: everything needed for code work is
in this file, `CONTRIBUTING.md` and `docs/`.

## First Read

1. Read this file.
2. Read `../roofwright-vault/Welcome.md`.
3. Read `../roofwright-vault/Vault Map.md`.
4. Read `../roofwright-vault/00 - Meta/Agent Guide.md`.
5. Read `../roofwright-vault/00 - Meta/Source Authority.md` and
   `../roofwright-vault/00 - Meta/Agentic Project Profile.json`.
6. Read only the notes relevant to the task.

## Source Hierarchy

1. User request in the current conversation.
2. Source-code repository rules in this file.
3. The authority map and governed vault notes under `../roofwright-vault/`.
4. The owner's global agent instructions, when the runtime loads them.

## SDLC method (central)

This project adopts the central Agentic SDLC method. Source of truth for the
method (profiles, phases, G0 Intake ready to G8 Learning captured gates,
operational patterns):
`../../../agentic-suite-agnostic/` (start with `00 - Map/Vault map.md`).
Adapt, do not copy: the central method governs the form; this project governs
the local instantiation (stack, commands, thresholds).

- adopted agentic_sdlc_version: 2026-10-20
- Deltas since then: `../../../agentic-suite-agnostic/CHANGELOG.md` (focus on the
  "Downstream impact" section).
- Without the sibling checkout, the method is at
  `https://github.com/brunofgsaraiva/agentic-suite-agnostic`.
- Local adaptation (when it exists): see local instantiation note,
  `../roofwright-vault/05 - Operations/SDLC Local Instantiation.md` (gates
  mapping, thresholds, repeatability decisions). The gate commands needed for
  code work are repeated in "Engineering and gates" below.
- Knowledge sync mode: authoritative-vault.
- Knowledge navigation scale: small.
- Adoption contract: `../../../agentic-suite-agnostic/ADOPTION.md`.
- Agent entry rule: for delivery or coding work, use the `agentic-sdlc` gateway
  before acting. If the runtime does not load skills automatically, read
  `../../../agentic-suite-agnostic/skills/agentic-sdlc/SKILL.md` and follow it.

<!-- agentic-project:runtime-profile:start -->
## Agentic Project runtime defaults

- Selected by default when available: `rtk-ai-cli`. Compress noisy development command output before it enters agent context.
- These are project runtime preferences, not Agentic SDLC dependencies or gate
  criteria. The user can opt out of a default for a task.
- Explicit operator modes such as `modo-compacto` remain opt-in only.
- Optional scaffold files for project type `product-tooling`: none, by declaration. Reason: Product tooling is reproduced by its own engineers from the repository's test path, so the reader-facing report adds a second, weaker surface.

<!-- agentic-project:runtime-profile:end -->

## Rules

- When reporting information to me, be extremely concise and sacrifice grammar for the sake of concision
- Use EN-GB for project knowledge unless the user asks otherwise.
- Follow the sibling vault's `00 - Meta/Source Authority.md` for persistent
  knowledge, external facts, approvals and work state.
- Preserve relative links between the source-code and vault repositories.
- Keep raw source material, working notes and derived outputs separate.
- Use `../roofwright-vault/02 - Sources/raw/` as the unprocessed source inbox;
  move ingested source files to `../roofwright-vault/02 - Sources/raw/processed/`
  and attachments to `../roofwright-vault/02 - Sources/raw/assets/`.
- Classify documentation code by role, not extension. This repository owns
  generators, themes and documentation applications that require a build,
  dependencies, tests or deployment. Hand-authored HTML, CSS or JavaScript that
  is itself human-readable documentation may belong in the sibling vault.
- Do not edit generated documentation outputs directly. Record
  `generated_from`, `build_command` and `output_path` in the vault operations
  note.
- If the sibling vault is unavailable, do not invent or silently replace its
  decisions. Record the missing dependency and continue only with bounded code work.
- Repeatability rule: when a task, or a stable part of it, is reasonably
  expected to run more than once, the work is not complete until it leaves the
  smallest suitable deterministic path under version control. Reuse an existing
  path first. Otherwise add a script or task, hook or CI check, scheduled job,
  skill or pipeline, or a loop only when iteration and a verifiable done-rule
  are required.
  Size the mechanism against its payback before building it: state the
  expected number of runs over a named horizon, the cost of one manual run,
  and the cost of building and owning the path. When the build does not pay
  back inside that horizon, keep the task manual and record that decision
  where the next operator will look. Frequency alone does not justify
  automation, and neither does a paid subscription.
  Document the trigger and verification. Keep irreducible judgement as an
  explicit human checkpoint; do not automate an unstable process or oracle.
- Do not revert or overwrite changes from another agent. Inspect dirty state before editing.
- Keep this file as the only canonical instruction source. `CLAUDE.md` is an
  import-only pointer and must not contain separate project rules.
- Do not store credentials, secrets or private customer data in the repository.

## Writing Conventions

- Never use the em dash or en dash in human-facing deliverables (HTML pages, slide decks, DOCX/PDF, published or client-facing content) or in the sources that render into them. Prefer commas, colons, parentheses or full stops; in ranges use "to" (for example, "G0 Intake ready to G8 Learning captured"). Internal artefacts (chat responses, code, commit messages, scratch notes) are exempt.
- Short identifiers (G0 Intake ready, D40 plus readable name, R10 plus readable name) always appear with the readable name beside them (for example, "G4 Tests with teeth"), never just the code.

## Roofwright

Binding rules for every agent working on Roofwright. Merged here on 2026-10-19
from the previous local session brief (the durable, publishable rules only),
the previous `AGENTS.md` and the owner's Claude memory notes. This repository
is **public**: never copy vault contents, private notes or the owner's private
contact details into tracked files; refer to the vault only as the local path
`../roofwright-vault/`.

### 1. Project

- Roofwright is a server-side Fabric mod for **Minecraft Java 26.3** that
  generates complete roofs from a footprint (outline detection, styles, pitch,
  overhang, materials, preview, place, undo). Mod id `roofwright`, package
  `io.github.razekteixeira.roofwright`, repo
  https://github.com/razekteixeira/roofwright, site
  https://razekteixeira.github.io/roofwright/, published on CurseForge
  (project 1735168).
- Owner: César (GitHub `razekteixeira`). Keep status updates short and
  readable on a phone.
- Requirements and decisions: `docs/spec.md` (acceptance criteria AC1 to AC24,
  each mapped to a discriminating test), `docs/research.md` (cited research
  and its ledger, sceptic and review annexes). Gate log with evidence per gate:
  `docs/sdlc-log.md`. Contributor rules, layout, artwork pipeline and release
  steps: `CONTRIBUTING.md`. Do not duplicate them here.
- Reference implementation: the sibling mod **Throughput**
  (`../../throughput/throughput-source-code/`, github.com/razekteixeira/throughput).
  Reuse its structure, tooling, scripts and quality bar. Read it only; never
  modify it.

### 2. Conduct and authorisation

- Release tags (`v*`), CurseForge uploads, new repositories and any change
  outside this repository need the owner's explicit instruction. Reading
  `../../throughput/throughput-source-code/` and the vault is allowed.
- Never `cd` into a sibling repository: the session working directory and
  worktree tools follow `cd` (on 2026-10-08 a `cd` into the Throughput repo made a worktree
  inside the sibling repo). Read siblings by absolute path (`cat`, `javap`,
  `git -C`).
- On resume, run `git status` first (look for an interrupted rebase or merge),
  then read `docs/sdlc-log.md` and `git log`. Keep the gate log current as each
  gate closes, so a lost session costs nothing.
- Name gates with code and readable name (for example "G4 Tests with teeth").
- No secrets in the repository and never publish the owner's private email
  addresses. Commit identity is already set in this repository
  (`22983237+razekteixeira@users.noreply.github.com`); do not change it.

### 3. Engineering and gates

- Local gate (G3 Build green): `./gradlew build` with JDK 25 (CI uses Temurin
  25 in `.github/workflows/build.yml`): compile, JUnit tests for `core`, server
  GameTests on a headless 26.3 server.
- G4 Tests with teeth: `python3 -P tools/mutation.py` plants the mutants and
  fails unless a **named test** kills each one, never a compile error or a
  crash. Read the results, do not just count them. Add a mutant when you add a
  safety rule.
- Benchmarks are tests: re-run `./gradlew benchmark` (planner) and
  `tools/benchmark.sh` (placement over RCON) before and after any change to
  `PlacementJob`, placement or the planner. Measure the operation itself,
  warmed up, repeated readings; coarse `/tick query` hides spikes.
- G5 Security clean is decided on every work item (audit or explicit skip
  reason); the last full review is `docs/security-review.md`. G6 Independent
  review for sensitive surfaces: run an outsider reviewer (different model
  family) and re-verify after fixing (`docs/independent-review.md`).
- Client captures: `caffeinate -dimsu ./gradlew runClientGameTest` (display
  sleep stalls the window), always with a time limit and a "process died" exit
  in any wait loop. `./gradlew clean` deletes the captures: generate site media
  in the same pass as the capture run.
- Look at real captures in review: a shape or banner that passes the tests can
  still look wrong. Hide hand and hotbar in chat shots (spectator mode), clear
  chat first.
- A flaky test is a bug until proven otherwise; rank explicitly whenever a
  registry or map is searched for "the first match".
- Dev server ports for this project: server 25567, RCON 25577 bound to
  127.0.0.1 with a random password per run. Another session may build a
  sibling mod at the same time; never reuse its ports.

### 4. Minecraft and Fabric lessons (from Throughput)

- Saved data: version it, never let a decode throw, quarantine unreadable or
  newer data verbatim, and handle Minecraft's own unreadable-file path (it
  starts empty and overwrites on next save): back the file up and go
  read-only. Prove it with a truncated-file run on a real server plus a control
  run without the guard.
- Config: lenient read codec with defaults, strict write codec
  (`optionalFieldOf` omits default values when encoding), clamp with warnings,
  never overwrite a broken file, `/roof reload`.
- Permissions: bundle `me.lucko:fabric-permissions-api:0.7.0` with `include`,
  use the `PermissionLevel` overloads (the int ones are deprecated), resend
  command trees after reload.
- Performance: spread periodic work across ticks; guard against `/tick freeze`.
- GameTests: start from a fresh world each run; tests touching global state
  clean up first. Operators default to level 0 (op mock players with an
  explicit level); vanilla adds `minecraft:always_pass` to the required count;
  out-of-height positions report "not loaded".
- Minecraft 26.3 is unobfuscated (Mojang names); inspect APIs with `javap` on
  `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-common-deobf/26.3/*.jar`
  (client classes: `minecraft-clientonly-deobf`).

### 5. Toolchain on the owner's Mac

- JDK 25: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25`. Gradle via the
  wrapper.
- GitHub CLI: `env -u GITHUB_TOKEN gh ...` (use the keychain login). Git pushes
  use SSH.
- Blender 5.2 LTS (Cycles on Metal):
  `/Applications/Blender.app/Contents/MacOS/Blender -b -P script.py -- args`.
- Headless Chrome: `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome --headless=new --virtual-time-budget=5000 --screenshot=...`.
  It has a minimum window width, so check the phone layout inside a 390 px
  iframe wrapper.
- PIL with `python3 -P` (not `-I`: Pillow is in user site-packages), ffmpeg,
  ImageMagick (`magick`), actionlint, shellcheck.

### 6. Site and release

- Distribution is CurseForge only (owner's choice, 2026-10-08, for its larger
  community) through mod-publish-plugin; never add Modrinth unless the owner
  asks.
- Site: `site/` deploys to GitHub Pages on push to `main` (`pages.yml`); the
  Pages environment allows only `main`. Verify at 1440 px and a true 390 px
  iframe, and that every referenced asset returns 200.
- Release (owner's instruction only): steps in `CONTRIBUTING.md`; tag `vX.Y.Z`
  matching `version` in `gradle.properties`; `release.yml` builds, tests,
  creates the GitHub release and uploads to CurseForge (dry run without the
  token or project id). Text and screenshots for CurseForge
  (`branding/curseforge-*.md`) come from the same capture run.
- GitHub: a push that creates the default branch runs only workflows whose
  path filters match; `workflow_dispatch` returns 404 until the workflow is
  registered. `curl --get` moves `--data-binary` into the URL.
