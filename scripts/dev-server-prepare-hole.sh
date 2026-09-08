#!/usr/bin/env bash
# Prepare the bounded practice platform on the Docker dev server.
#
# Creates an explicit, idempotent grass platform for the default M4.5 hole:
#   tee:  entity position (0.5, 63.25, 0.5)  —  floor block at (  0, 62,  0)
#   cup:  entity position (16.5, 63.25, 0.5) —  cup block at  ( 16, 63,  0)
#
# Platform footprint:
#   floor:      fill X[-2..18]  Y=62       Z[-3..3]  minecraft:grass_block
#   air column: fill X[-2..18]  Y[63..80]  Z[-3..3]  minecraft:air
#   cup:        setblock        (16,63,0)             minecraft_golf:golf_cup
#
# Idempotent: fill-replace within this footprint is safe to re-run.
# This script is NOT called on normal server startup; invoke it explicitly
# after dev-server-up.sh or dev-server-restart.sh whenever the practice
# platform needs to be established or restored.
#
# Usage (from repo root):
#   ./scripts/dev-server-prepare-hole.sh

set -euo pipefail

CONTAINER="minecraft-golf-dev"

# ---------- helpers ----------

rcon() {
    docker exec "$CONTAINER" rcon-cli "$@"
}

# Run an RCON command, print the server's response, and abort if the server
# returns a recognisable command-error prefix.
rcon_run() {
    local label="$1"; shift
    local output
    output=$(rcon "$@")
    if echo "$output" | grep -qiE \
        "^(Unknown command|Incorrect argument|Expected|is not a valid|An unexpected error|That position is not loaded)"; then
        echo "  ERROR  $label: $output" >&2
        exit 1
    fi
    # "No blocks were filled" is a normal idempotent result, not an error.
    echo "  $output"
}

# Verify that the block at (x,y,z) matches the expected type by conditionally
# running `list`, whose RCON response is stable and non-empty. A missing player-
# count response means the condition failed and the block is absent or wrong.
verify_block() {
    local label="$1" x="$2" y="$3" z="$4" block="$5"
    local output
    output=$(rcon "execute if block $x $y $z $block run list" 2>&1) || true
    if echo "$output" | grep -q "There are .* of a max of .* players online"; then
        printf "      PASS  %s  (%s,%s,%s) = %s\n" "$label" "$x" "$y" "$z" "$block"
    else
        printf "      FAIL  %s  (%s,%s,%s) expected %s\n" \
               "$label" "$x" "$y" "$z" "$block" >&2
        VERIFY_FAILED=1
    fi
}

check_container() {
    if ! docker inspect "$CONTAINER" > /dev/null 2>&1; then
        echo "ERROR: container '$CONTAINER' not found." >&2
        echo "       Run ./scripts/dev-server-up.sh first." >&2
        exit 1
    fi
}

wait_healthy() {
    local attempts=0 max=30
    echo "[1/7] Waiting for server to be healthy..."
    while (( attempts < max )); do
        local health
        health=$(docker inspect --format='{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null \
                 || echo "not_found")
        case "$health" in
            healthy)
                echo "      Server is healthy."
                return 0
                ;;
            not_found)
                echo "ERROR: container '$CONTAINER' disappeared." >&2
                exit 1
                ;;
        esac
        attempts=$((attempts + 1))
        printf "      Status: %-12s  (%d/%d, retrying in 10s…)\n" "$health" "$attempts" "$max"
        sleep 10
    done
    echo "ERROR: Server not healthy after $((max * 10))s." >&2
    echo "       Check logs: ./scripts/dev-server-logs.sh" >&2
    exit 1
}

check_rcon() {
    local output
    echo "[2/7] Checking RCON command access..."
    if ! output=$(rcon "list" 2>&1); then
        echo "ERROR: RCON is not available in '$CONTAINER': $output" >&2
        echo "       Check logs: ./scripts/dev-server-logs.sh" >&2
        exit 1
    fi
    echo "      RCON is ready: $output"
}

# ---------- main ----------

check_container
wait_healthy
check_rcon

echo "[3/7] Force-loading the bounded practice chunks…"
rcon_run "force-load practice chunks" "forceload add -2 -3 18 3"

echo "[4/7] Clearing air column above platform (X:-2..18, Y:63..80, Z:-3..3)…"
rcon_run "clear air" "fill -2 63 -3 18 80 3 minecraft:air replace"

echo "[5/7] Placing grass floor (X:-2..18, Y:62, Z:-3..3)…"
rcon_run "grass floor" "fill -2 62 -3 18 62 3 minecraft:grass_block replace"

echo "[6/7] Placing cup block at (16, 63, 0)…"
rcon_run "cup block" "setblock 16 63 0 minecraft_golf:golf_cup"

echo "[7/7] Verifying key blocks…"
VERIFY_FAILED=0
verify_block "tee floor"         "0"  "62" "0"  "minecraft:grass_block"
verify_block "cup block"         "16" "63" "0"  "minecraft_golf:golf_cup"
verify_block "platform edge -X"  "-2" "62" "-3" "minecraft:grass_block"
verify_block "platform edge +X"  "18" "62" "3"  "minecraft:grass_block"

if (( VERIFY_FAILED )); then
    echo ""
    echo "ERROR: One or more blocks did not verify." >&2
    echo "       Rerun this script or inspect logs: ./scripts/dev-server-logs.sh" >&2
    exit 1
fi

echo ""
echo "Practice hole ready."
printf "  Footprint:  X[-2..18]  Y=62  Z[-3..3]  (floor surface at Y=63)\n"
printf "  Tee:        tp to 0.5 63.5 0.5  then run  /golf hole start\n"
printf "  Cup:        entity pos (16.5, 63.25, 0.5)  —  block at (16, 63, 0)\n"
