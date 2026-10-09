# CurseForge launch sheet

Everything CurseForge asks for, in order. The project page, logo and gallery have no public API,
so they are entered by hand once; files are uploaded by the release workflow afterwards.

## 1. Create the project

[authors.curseforge.com](https://authors.curseforge.com) → Create project → Minecraft → Mods.

| Field | Value |
|---|---|
| Name | `Roofwright` (free on CurseForge and Modrinth, checked 2026-10-08, see `docs/research.md`) |
| Slug | `roofwright` (URL: curseforge.com/minecraft/mc-mods/roofwright) |
| Summary | contents of [`summary.txt`](summary.txt) (under 250 characters) |
| Description | switch the editor to **Markdown**, paste [`curseforge-description.md`](curseforge-description.md) |
| Main category | Utility & QoL |
| Additional categories | CreativeMode, Server Utility |
| License | Apache License 2.0 |
| Source URL | https://github.com/razekteixeira/roofwright |
| Issues URL | https://github.com/razekteixeira/roofwright/issues |
| Wiki / website | https://razekteixeira.github.io/roofwright/ |
| Logo | [`icon-512.png`](icon-512.png) (square PNG, at least 400×400; not WebP) |

Why these categories (from CurseForge's own list, read from its API on 2026-10-09): it is a building
tool that saves hours of manual placement (**Utility & QoL**), aimed at builders in creative mode and
on build servers (**CreativeMode**), and it is a server-side mod with permission nodes, limits and
claim protection that server owners install for their players (**Server Utility**). It adds no new
blocks or items, so not Cosmetic, World Gen or Structures.

## 2. Settings

- **Project distribution / third-party apps: enable it.** Without it, launchers (Prism,
  ATLauncher) and server hosts cannot download the file, which matters for a server-side mod.
- **Dependencies** (set by the release workflow on every file): Fabric API, required.

## 3. Images tab (gallery)

Upload in this order; mark the first one as featured. All are real captures from the game client
(`RoofwrightCaptures`) except the banner, which is composed from them by `make_banners.py`.

| File | Title | Description |
|---|---|---|
| `site/media/banner.png` | Roofwright | Whole roofs from the tops of your walls, in one click. |
| `site/media/after.png` | One click per house | Seven houses roofed with /roof detect and /roof place: gable, hip, dutch gable, gambrel, mansard, cone and flat. |
| `site/media/before.png` | Before | The same village with bare walls. |
| `site/media/grow.gif` | Ghost, then roof | The preview only you can see, then the roof going up from the eaves, a few blocks per tick. |
| `site/media/preview.png` | Ghost preview | A glowing ghost of the roof; blocks in the way show as red glass and are skipped. |
| `site/media/gallery-valleys.png` | Hips and valleys | A hip roof on an L-shaped house: outer corners on the hips, inner corners in the valley. |
| `site/media/styles.png` | Six styles, one house | Gable, hip, dutch gable, gambrel, mansard and flat on the same L-shaped house. |
| `site/media/gallery-barn.png` | Gambrel barn | Steep below, shallow above; the gable wall is filled with the barn's own block. |
| `site/media/gallery-tower.png` | Cone tower | A steep cone on a round cobblestone tower. |
| `site/media/gallery-dusk.png` | Dusk | The roofed village at sunset. |

## 4. Releases

1. Create the project (step 1), then put its numeric project ID in `gradle.properties`
   (`curseforge_project_id=`) and commit.
2. Create an API token at [authors.curseforge.com → API tokens](https://authors.curseforge.com/#/settings/api-tokens)
   and run `gh secret set CURSEFORGE_TOKEN -R razekteixeira/roofwright`.
3. Tag the release: `git tag v1.0.0-beta.1 && git push origin v1.0.0-beta.1`. The workflow checks the
   tag matches `gradle.properties`, builds, runs every test, creates the GitHub release and uploads
   the jar to CurseForge as a beta (game version 26.3, Java 25, Fabric, client + server, requires
   Fabric API). Without the token it stops after the GitHub release; `./gradlew publishCurseforge`
   without a token or project ID is a dry run.
4. The file shows as "Under review" until a CurseForge moderator approves it.
5. After approval, add the CurseForge downloads badge to the README
   (`https://img.shields.io/curseforge/dt/<project id>?logo=curseforge&label=CurseForge`).
