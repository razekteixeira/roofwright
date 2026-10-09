#!/usr/bin/env bash
# Regenerates site/media from a real client run.
# Usage: caffeinate -dimsu ./gradlew runClientGameTest && branding/make_media.sh && python3 -P branding/make_banners.py
# (caffeinate keeps macOS from sleeping the display, which stalls the client window)
# Needs ffmpeg and ImageMagick. The chat text quoted on the site and in the CurseForge description must be
# copied from the same run's log line "[System] [CHAT] Hip, 1:1, overhang 1 roof in ...".
set -euo pipefail
cd "$(dirname "$0")/.."
shots=build/run/clientGameTest/screenshots
media=site/media
frames=$(mktemp -d)
trap 'rm -rf "$frames"' EXIT
mkdir -p "$media"

one() { find "$shots" -name "*_$1.png" | head -n 1; }

cp "$(one before)" "$media/before.png"
cp "$(one after)" "$media/after.png"
cp "$(one preview)" "$media/preview.png"
for shot in barn tower valleys dusk; do
	cp "$(one "gallery_$shot")" "$media/gallery-$shot.png"
done
styles=(gable hip dutch_gable gambrel mansard flat)
for style in "${styles[@]}"; do
	cp "$(one "style_$style")" "$media/style-${style//_/-}.png"
done

# Styles grid, 3 x 2, each tile cropped to the house and labelled by its file name order.
tiles=()
for style in "${styles[@]}"; do
	tile="$frames/tile_$style.png"
	magick "$media/style-${style//_/-}.png" -gravity center -crop 1280x720+0+40 +repage -resize 640x360 "$tile"
	tiles+=("$tile")
done
magick \( "${tiles[0]}" "${tiles[1]}" "${tiles[2]}" +append \) \( "${tiles[3]}" "${tiles[4]}" "${tiles[5]}" +append \) -append "$media/styles.png"

# The chat summary, cropped from the bottom left (no hand, no hotbar in the crop).
magick "$(one chat)" -crop 1010x110+0+858 +repage "$media/chat.png"

# The roof going up: hold the bare house and the ghost, then every frame, then hold the finished roof.
i=0
add() { cp "$1" "$frames/$(printf '%03d' "$i").png"; i=$((i + 1)); }
for f in $(find "$shots" -name '*_grow_[0-9]*.png' | sort); do add "$f"; done
last=$(find "$shots" -name '*_grow_end_*.png' | sort | tail -n 1)
for _ in 1 2 3 4 5 6 7 8 9 10 11 12; do add "$last"; done
ffmpeg -loglevel error -y -framerate 10 -i "$frames/%03d.png" \
	-vf "crop=1280:720:320:200,scale=800:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=160[p];[b][p]paletteuse=dither=bayer:bayer_scale=4" \
	-loop 0 "$media/grow.gif"

for f in "$media"/*.png; do
	magick "$f" -strip -define png:compression-level=9 "$f"
done
# WebP for the site: full size and a 640 px thumbnail for the gallery grid.
for f in "$media"/*.png; do
	base="${f%.png}"
	magick "$f" -quality 86 "$base.webp"
	case "$(basename "$base")" in
		gallery-*|style-*|before|after|preview) magick "$f" -resize 640x -quality 82 "$base-640.webp" ;;
	esac
done
echo "media updated from $i grow frames"
