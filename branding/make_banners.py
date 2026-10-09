"""Composes the project banners from real captures and the rendered 3D logo.

Inputs (run branding/make_media.sh and branding/render_logo3d.py first):
  site/media/after.png    background, the real roofed village
  site/media/before.png   the same village with bare walls, for the before and after strip
  branding/logo3d.png     Blender render of the icon

Outputs:
  site/media/banner.png         1920x1080, CurseForge gallery and description header
  site/media/social.png         1280x640, GitHub social preview and link previews (og:image)
  site/media/readme-header.png  1600x480, top of the README

Run from the project root: python3 -P branding/make_banners.py
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
MEDIA = ROOT / "site" / "media"
FONTS = ROOT / "branding" / "fonts"
TERRACOTTA = (232, 112, 58)
SHADE = (120, 50, 22)
INK = (10, 13, 24)


def font(name, size, weight=None):
    f = ImageFont.truetype(str(FONTS / name), size)
    if weight is not None:
        f.set_variation_by_axes([weight])
    return f


def cover(image, size):
    """Scale and centre-crop to fill size."""
    w, h = size
    scale = max(w / image.width, h / image.height)
    resized = image.resize((round(image.width * scale), round(image.height * scale)), Image.LANCZOS)
    left = (resized.width - w) // 2
    top = (resized.height - h) // 2
    return resized.crop((left, top, left + w, top + h))


def background(size):
    village = Image.open(MEDIA / "after.png").convert("RGB")
    bg = cover(village, size).filter(ImageFilter.GaussianBlur(1.4))
    # Darken towards the left, where the text sits.
    shade = Image.new("L", size)
    px = shade.load()
    for x in range(size[0]):
        alpha = int(238 - 170 * min(1.0, x / (size[0] * 0.9)))
        for y in range(size[1]):
            px[x, y] = alpha
    return Image.composite(Image.new("RGB", size, INK), bg, shade).convert("RGBA")


def logo(height):
    image = Image.open(ROOT / "branding" / "logo3d.png").convert("RGBA")
    bbox = image.getchannel("A").getbbox()
    image = image.crop(bbox)
    width = round(image.width * height / image.height)
    image = image.resize((width, height), Image.LANCZOS)
    pad = height // 6
    size = (width + 2 * pad, height + 2 * pad)
    # Blur on the padded canvas so the glow fades out instead of stopping at the image edge.
    mask = Image.new("L", size)
    mask.paste(image.getchannel("A").point(lambda a: a // 3), (pad, pad))
    glow = Image.new("RGBA", size, TERRACOTTA + (0,))
    glow.putalpha(mask.filter(ImageFilter.GaussianBlur(height // 12)))
    glow.alpha_composite(image, (pad, pad))
    return glow


def title(draw, xy, size, max_width):
    x, y = xy
    f = font("Silkscreen-Regular.ttf", size)
    while draw.textlength("ROOFWRIGHT", font=f) > max_width and size > 20:
        size -= 2
        f = font("Silkscreen-Regular.ttf", size)
    shadow = max(3, size // 24)
    draw.text((x + shadow, y + shadow), "ROOFWRIGHT", font=f, fill=SHADE)
    draw.text((x, y), "ROOFWRIGHT", font=f, fill=TERRACOTTA)
    return draw.textbbox((x, y), "ROOFWRIGHT", font=f)[3]


def before_after(width):
    """The bare village and the roofed one, side by side with thin terracotta frames."""
    tile_w = (width - 24) // 2
    tiles = []
    for name in ("before", "after"):
        shot = Image.open(MEDIA / f"{name}.png").convert("RGBA")
        shot = cover(shot, (tile_w, round(tile_w * 9 / 16)))
        frame = Image.new("RGBA", (shot.width + 8, shot.height + 8), TERRACOTTA + (255,))
        frame.alpha_composite(shot, (4, 4))
        tiles.append(frame)
    strip = Image.new("RGBA", (tiles[0].width * 2 + 16, tiles[0].height), (0, 0, 0, 0))
    strip.alpha_composite(tiles[0], (0, 0))
    strip.alpha_composite(tiles[1], (tiles[0].width + 16, 0))
    return strip


def compose(size, out, title_size, tag_size, show_strip, logo_height):
    w, h = size
    canvas = background(size)
    badge = logo(logo_height)
    margin = round(w * 0.065)
    text_width = w - badge.width - margin - round(w * 0.02)
    draw = ImageDraw.Draw(canvas)
    y = round(h * (0.14 if show_strip else 0.24))
    draw.text((margin, y), "FABRIC  ·  MINECRAFT 26.3  ·  SERVER-SIDE", font=font("Geist.ttf", round(tag_size * 0.55), 600),
              fill=(170, 180, 198))
    y = title(draw, (margin, y + round(tag_size * 0.95)), title_size, text_width) + round(tag_size * 0.5)
    for line in ("Whole roofs from the tops", "of your walls, in one click."):
        draw.text((margin, y), line, font=font("Geist.ttf", tag_size, 560), fill=(238, 240, 245))
        y += round(tag_size * 1.22)
    if show_strip:
        strip = before_after(round(w * 0.5))
        y += round(tag_size * 0.6)
        canvas.alpha_composite(strip, (margin, y))
        labels = font("Geist.ttf", round(tag_size * 0.5), 600)
        draw.text((margin, y + strip.height + 12), "before", font=labels, fill=(200, 206, 218))
        draw.text((margin + strip.width // 2 + 8, y + strip.height + 12), "after  /roof place", font=labels, fill=(200, 206, 218))
    canvas.alpha_composite(badge, (w - badge.width + round(badge.height * 0.06), (h - badge.height) // 2))
    canvas.convert("RGB").save(out, optimize=True)
    print("wrote", out.relative_to(ROOT), canvas.size)


compose((1920, 1080), MEDIA / "banner.png", title_size=150, tag_size=54, show_strip=True, logo_height=640)
compose((1280, 640), MEDIA / "social.png", title_size=104, tag_size=38, show_strip=False, logo_height=420)
compose((1600, 480), MEDIA / "readme-header.png", title_size=96, tag_size=34, show_strip=False, logo_height=340)
