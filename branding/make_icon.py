"""Roofwright mod icon.

Authored on a 32x32 pixel grid and exported with nearest-neighbour scaling only, so every output stays
crisp pixel art: a cottage at night whose roof is a staircase of two-pixel steps, the way stairs build a
roof in the game, with lit windows.

Run from the project root:  python3 -P branding/make_icon.py
(-P keeps the script directory off sys.path but still sees user-site Pillow.)
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
N = 32

PALETTE = {
    ".": (0, 0, 0, 0),            # transparent (outside rounded corners)
    "K": (8, 10, 18, 255),        # tile border
    "n": (20, 26, 48, 255),       # night sky, top
    "m": (28, 36, 66, 255),       # night sky, lower band
    "S": (226, 232, 255, 255),    # star
    "O": (12, 12, 18, 255),       # outline
    "R": (214, 96, 50, 255),      # roof terracotta
    "r": (160, 62, 32, 255),      # roof step shadow
    "Q": (246, 150, 92, 255),     # roof top edge light
    "W": (234, 222, 196, 255),    # plaster wall
    "V": (198, 182, 152, 255),    # wall shade under the eaves
    "A": (255, 180, 50, 255),     # lit window
    "a": (196, 112, 20, 255),     # window sill shade
    "B": (96, 62, 38, 255),       # door
    "G": (66, 116, 58, 255),      # grass
    "g": (48, 88, 44, 255),       # grass shade
}

CENTRE = 15.5
ROOF_TOP, ROOF_BOTTOM = 7, 18
WALL_X0, WALL_X1, WALL_Y1 = 7, 24, 27
STARS = [(5, 4), (9, 7), (24, 4), (27, 9), (20, 3), (4, 11)]


def blank():
    return [["." for _ in range(N)] for _ in range(N)]


def tile(grid):
    """Rounded tile: night sky in two bands, a dark border."""
    corner = {(0, 0), (1, 0), (0, 1), (N - 1, 0), (N - 2, 0), (N - 1, 1),
              (0, N - 1), (1, N - 1), (0, N - 2), (N - 1, N - 1), (N - 2, N - 1), (N - 1, N - 2)}
    border = {(1, 1), (N - 2, 1), (1, N - 2), (N - 2, N - 2)}
    for y in range(N):
        for x in range(N):
            if (x, y) in corner:
                continue
            if x in (0, N - 1) or y in (0, N - 1) or (x, y) in border:
                grid[y][x] = "K"
            else:
                grid[y][x] = "n" if y < 13 else "m"
    for x, y in STARS:
        grid[y][x] = "S"


def roof_cells():
    """Rows widen by two pixels every two rows: a staircase, like stairs on a 1:1 roof."""
    cells = {}
    for y in range(ROOF_TOP, ROOF_BOTTOM + 1):
        k = y - ROOF_TOP
        half = 2 + 2 * (k // 2) + (1 if y == ROOF_BOTTOM else 0)
        for x in range(int(CENTRE - half + 0.5), int(CENTRE + half + 0.5)):
            cells[(x, y)] = "r" if k % 2 == 1 else "R"
    return cells


def draw(grid):
    shape = {}
    # Walls with two lit windows and a door.
    for y in range(ROOF_BOTTOM + 1, WALL_Y1 + 1):
        for x in range(WALL_X0, WALL_X1 + 1):
            shape[(x, y)] = "V" if y == ROOF_BOTTOM + 1 else "W"
    for x0 in (9, 20):
        for y in range(21, 24):
            for x in range(x0, x0 + 3):
                shape[(x, y)] = "a" if y == 23 else "A"
    for y in range(23, WALL_Y1 + 1):
        for x in range(14, 18):
            shape[(x, y)] = "B"
    shape.update(roof_cells())
    # Top edge of the roof catches the light.
    for (x, y), ch in list(shape.items()):
        if ch in "Rr" and (x, y - 1) not in shape:
            shape[(x, y)] = "Q"
    # Outline everything, then lay the shapes on top.
    for x, y in shape:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                p = (x + dx, y + dy)
                if p not in shape and 1 < p[0] < N - 2 and 1 < p[1] < N - 2:
                    grid[p[1]][p[0]] = "O"
    for (x, y), ch in shape.items():
        grid[y][x] = ch
    # Grass under the house.
    for x in range(2, N - 2):
        for y in (WALL_Y1 + 1, WALL_Y1 + 2):
            if grid[y][x] in "nmO":
                grid[y][x] = "G" if y == WALL_Y1 + 1 else "g"


def render(grid):
    img = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            img.putpixel((x, y), PALETTE[grid[y][x]])
    return img


def main():
    grid = blank()
    tile(grid)
    draw(grid)
    img = render(grid)
    outputs = {
        ROOT / "src/main/resources/assets/roofwright/icon.png": 128,
        ROOT / "branding/icon-512.png": 512,
        ROOT / "branding/icon-32.png": 32,
        ROOT / "site/favicon.png": 64,
        ROOT / "site/apple-touch-icon.png": 192,
    }
    for path, size in outputs.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        img.resize((size, size), Image.NEAREST).save(path, optimize=True)
        print(f"wrote {path.relative_to(ROOT)} ({size}x{size})")


if __name__ == "__main__":
    main()
