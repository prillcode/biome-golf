#!/usr/bin/env bash
# Reset the local development world to its baseline.
#
# A baseline is this world's known-good origin: the state captured by
# `world-sync.sh pull` (with the container already stopped, so it is pristine).
# Reset restores that state so local development starts over from a known point.
# It does NOT delete or regenerate the world.
#
# This is a purely local operation; it never contacts the live server. To
# refresh the baseline from the live world, run world-sync.sh pull.
#
# A world with no baseline (a local-only world, or one never pulled) cannot be
# reset. Creating a new world is a separate action: use a new directory name.
set -euo pipefail
cd "$(dirname "$0")/../dev-server"

resolve_env_value() {
	local key="$1" file="$2"
	[[ -f "$file" ]] || return 1
	grep -E "^[[:space:]]*${key}[[:space:]]*=" "$file" | tail -1 \
		| cut -d= -f2- | tr -d "\"'" | tr -d '[:space:]'
}

# World directory: explicit environment, then dev-server/.env, then fail.
DATA_DIR="${MINECRAFT_DATA_DIR:-}"
if [[ -z "$DATA_DIR" ]]; then
	DATA_DIR="$(resolve_env_value MINECRAFT_DATA_DIR .env || true)"
fi
if [[ -z "$DATA_DIR" ]]; then
	echo "ERROR: MINECRAFT_DATA_DIR is not set (environment or dev-server/.env)." >&2
	echo "Reset restores the world pulled by world-sync.sh and needs its bind directory." >&2
	exit 1
fi
DATA_DIR="${DATA_DIR/#\~/$HOME}"
if [[ "$DATA_DIR" == "/" || "$DATA_DIR" == "$HOME" ]]; then
	echo "ERROR: refusing to operate on '$DATA_DIR'." >&2
	exit 1
fi

# Baselines live beside the world dir; keep this in step with world-sync.sh.
BASELINE_DIR="${MINECRAFT_BASELINE_DIR:-${DATA_DIR}-baselines}"

baselines=()
mapfile -t baselines < <(find "$BASELINE_DIR" -maxdepth 1 -name 'baseline-*.tar.gz' \
	-printf '%T@ %p\n' 2>/dev/null | sort -rn | cut -d' ' -f2-)
if [[ "${#baselines[@]}" -eq 0 ]]; then
	echo "ERROR: no baseline found in $BASELINE_DIR." >&2
	echo "Run: world-sync.sh pull    (captures a pristine baseline on the way down)" >&2
	exit 1
fi
BASELINE="${baselines[0]}"

echo "This will REPLACE the local development world with its baseline:"
echo "  world:    $DATA_DIR/world"
echo "  baseline: $BASELINE"
if [[ -f "${BASELINE%.tar.gz}.meta" ]]; then
	sed 's/^/    /' "${BASELINE%.tar.gz}.meta"
fi
echo
echo "Any local course work since that pull is DISCARDED."
echo "Source code, the staged mod JAR, and the live server are NOT touched."
echo
read -r -p "Type 'reset' to confirm: " confirm
if [[ "$confirm" != "reset" ]]; then
	echo "Aborted."
	exit 1
fi

docker compose down || true
rm -rf "$DATA_DIR/world"
tar xzf "$BASELINE" -C "$DATA_DIR"
echo "World reset to baseline: $(basename "$BASELINE")"
echo "Start it with ../scripts/dev-server-up.sh"
