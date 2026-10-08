#!/usr/bin/env bash
# Measures roof placement on the dev server: a 128 x 128 house (a 16,900-block hip roof) is roofed and
# undone 6 times with the default budget (2,000 blocks and 5 ms per tick). Each job reports its own time
# per tick in the server log; the first run is a warm-up. Prints ticks, worst tick and blocks per tick.
# Usage: tools/benchmark.sh   (needs JDK 25 as JAVA_HOME; uses ./run, which is gitignored)
# Ports: server 25567, RCON 25577 on 127.0.0.1 with a random password per run.
set -euo pipefail
cd "$(dirname "$0")/.."
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if ! "$java_bin" -version 2>&1 | grep -q 'version "25'; then
	echo "JDK 25 not found: set JAVA_HOME to a JDK 25 installation" >&2
	exit 1
fi
mkdir -p run
export RCON_PASSWORD
RCON_PASSWORD=$(python3 -c 'import secrets; print(secrets.token_hex(16))')
echo "eula=true" > run/eula.txt
cat > run/server.properties <<PROPS
server-ip=127.0.0.1
server-port=25567
enable-rcon=true
rcon.port=25577
rcon.password=${RCON_PASSWORD}
level-type=minecraft\:flat
online-mode=false
spawn-monsters=false
spawn-protection=0
PROPS
log=$(mktemp)
./gradlew runServer --console=plain > "$log" 2>&1 &
server=$!
trap 'python3 tools/rcon.py stop > /dev/null 2>&1 || true' EXIT
deadline=$((SECONDS + 600))
until grep -q "RCON running" "$log"; do
	if ! kill -0 "$server" 2>/dev/null || grep -q "BUILD FAILED" "$log" || [ "$SECONDS" -gt "$deadline" ]; then
		echo "Dev server failed to start, see $log" >&2
		exit 1
	fi
	sleep 2
done
rcon() { python3 tools/rcon.py "$@"; }

# Walls of a 128 x 128 house, 4 high, on the flat world (ground at y -61).
rcon "forceload add 990 990 1140 1140" \
	"fill 1000 -60 1000 1127 -57 1000 minecraft:stone_bricks" "fill 1000 -60 1127 1127 -57 1127 minecraft:stone_bricks" \
	"fill 1000 -60 1000 1000 -57 1127 minecraft:stone_bricks" "fill 1127 -60 1000 1127 -57 1127 minecraft:stone_bricks" \
	"sleep 2" "roof style hip" "roof detect 1000 -57 1000"

# Waits until the log holds $1 lines matching $2 (the job's completion line), or gives up after 120 s.
wait_for() {
	local until=$((SECONDS + 120))
	while [ "$(grep -c "$2" "$log" || true)" -lt "$1" ]; do
		if ! kill -0 "$server" 2>/dev/null || [ "$SECONDS" -gt "$until" ]; then
			echo "No '$2' line $1 in the server log, see $log" >&2
			exit 1
		fi
		sleep 0.5
	done
}

for run in 1 2 3 4 5 6; do
	rcon "roof place" > /dev/null
	wait_for "$run" "PLACE (hip"
	rcon "roof undo" > /dev/null
	wait_for "$run" "UNDO (hip"
done
grep "PLACE (hip" "$log" | tail -n +2 | sed -E 's/.*: ([0-9]+) placed.*, ([0-9]+) ticks, max ([0-9.]+) ms and ([0-9]+) blocks per tick/\1 \2 \3 \4/' \
	| sort -k3 -n | awk '{p=$1; t[NR]=$2; m[NR]=$3; b=$4} END {
		printf "hip roof, %d blocks: n=%d runs, %d ticks each, worst tick per run min=%.2f median=%.2f max=%.2f ms, up to %d blocks per tick\n",
			p, NR, t[1], m[1], m[int((NR+1)/2)], m[NR], b }'
