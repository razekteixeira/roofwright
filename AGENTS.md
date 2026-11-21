# Source Code Agent Guide

This repository contains the source code for Roofwright. The binding project,
engineering, toolchain and release rules are in the "Roofwright" sections at
the end of this file; they apply to every agent (Claude Code, Codex, Cursor,
others). Everything needed for code work is in this file, `CONTRIBUTING.md`
and `docs/`.

## First Read

1. Read this file.
2. If `.vault/` exists, it holds the owner's personal working notes (Margin):
   follow `.vault/AGENTS.md`. It is local and never committed; never name or
   copy its content into the repository, commits, PRs or code comments.
3. Read only the docs relevant to the task.

## Source Hierarchy

1. User request in the current conversation.
2. Source-code repository rules in this file.
3. `docs/spec.md`, `docs/sdlc-log.md` and `CONTRIBUTING.md`.
4. The owner's global agent instructions, when the runtime loads them.

## Rules

- When reporting information to me, be extremely concise and sacrifice grammar for the sake of concision
- Use EN-GB for project knowledge unless the user asks otherwise.
- Classify documentation code by role, not extension. This repository owns
  generators, themes and documentation applications that require a build,
  dependencies, tests or deployment.
- Do not edit generated documentation outputs directly; change their source and
  rebuild.
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
contact details into tracked files. The owner's personal notes live in `.vault/`
(local, never committed).

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
  (`../throughput/`, github.com/razekteixeira/throughput).
  Reuse its structure, tooling, scripts and quality bar. Read it only; never
  modify it.

### 2. Conduct and authorisation

- Release tags (`v*`), CurseForge uploads, new repositories and any change
  outside this repository need the owner's explicit instruction. Reading
  `../throughput/` and `.vault/` is allowed.
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
