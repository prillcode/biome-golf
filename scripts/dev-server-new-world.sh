#!/usr/bin/env bash
# Create a fresh local-only dev world and bank its generated starting point.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DEV_SERVER_DIR="$REPO_DIR/dev-server"
REGISTRY="${WORLDS_CONF:-$REPO_DIR/../apcode-dev/minecraft/tools/worlds.conf}"
ENV_FILE="$DEV_SERVER_DIR/.env"

usage() {
  echo "Usage: $(basename "$0") <world-name> <seed>" >&2
  echo "Creates ~/\<world-name\>, registers it as local-only, generates it, and captures a baseline." >&2
}
fail() { echo "ERROR: $*" >&2; exit 1; }

[[ $# -eq 2 ]] || { usage; exit 2; }
NAME="$1"
SEED="$2"
[[ "$NAME" =~ ^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$ ]] || fail "invalid world name '$NAME' (use lowercase letters, digits, and hyphens)."
[[ "$SEED" =~ ^-?[0-9]+$ ]] || fail "seed must be an integer."
[[ -f "$REGISTRY" ]] || fail "world registry not found: $REGISTRY (set WORLDS_CONF to override)."
command -v python3 >/dev/null || fail "python3 is required."
command -v docker >/dev/null || fail "docker is required."

DATA_DIR="$HOME/$NAME"
BASELINE_DIR="${DATA_DIR}-baselines"
[[ "$DATA_DIR" != "$HOME" && "$DATA_DIR" != / ]] || fail "unsafe target directory: $DATA_DIR"
[[ ! -e "$DATA_DIR" || ( -d "$DATA_DIR" && -z "$(find "$DATA_DIR" -mindepth 1 -maxdepth 1 -print -quit)" ) ]] \
  || fail "target directory already exists and is non-empty: $DATA_DIR"
[[ ! -e "$BASELINE_DIR" || ( -d "$BASELINE_DIR" && -z "$(find "$BASELINE_DIR" -mindepth 1 -maxdepth 1 -print -quit)" ) ]] \
  || fail "baseline directory already exists and is non-empty: $BASELINE_DIR"

python3 - "$REGISTRY" "$NAME" <<'PY' || fail "world name is already registered or the registry is invalid."
import configparser, sys
path, name = sys.argv[1:]
cp = configparser.ConfigParser()
cp.read(path)
if cp.has_section(name):
    print(f"ERROR: [{name}] is already registered in {path}", file=sys.stderr)
    raise SystemExit(1)
PY

# Do not switch the bind mount while the current dev server is running.
cd "$DEV_SERVER_DIR"
if [[ -n "$(docker compose ps --status running -q 2>/dev/null)" ]]; then
  fail "the dev server is running; stop it with ../scripts/dev-server-down.sh first."
fi

mkdir -p "$DATA_DIR" "$BASELINE_DIR"
python3 - "$REGISTRY" "$NAME" "$DATA_DIR" <<'PY'
import configparser, os, sys, tempfile
path, name, directory = sys.argv[1:]
cp = configparser.ConfigParser()
cp.read(path)
if cp.has_section(name):
    raise SystemExit(f"ERROR: [{name}] was registered while this command was starting")
with open(path) as source:
    contents = source.read()
block = f"[{name}]\ndir = {directory}\nbaselines = {directory}-baselines\n"
contents = contents.rstrip() + "\n\n" + block
fd, tmp = tempfile.mkstemp(prefix=".worlds.conf.", dir=os.path.dirname(path), text=True)
try:
    with os.fdopen(fd, "w") as out:
        out.write(contents)
    os.chmod(tmp, os.stat(path).st_mode & 0o777)
    os.replace(tmp, path)
except BaseException:
    try: os.unlink(tmp)
    except FileNotFoundError: pass
    raise
PY

# Preserve unrelated local settings, replacing only the two compose inputs.
touch "$ENV_FILE"
python3 - "$ENV_FILE" "$DATA_DIR" "$SEED" <<'PY'
import os, sys, tempfile
path, directory, seed = sys.argv[1:]
with open(path) as f:
    lines = f.readlines()
updates = {"MINECRAFT_DATA_DIR": directory, "MINECRAFT_SEED": seed}
seen = set()
result = []
for line in lines:
    key = line.partition("=")[0].strip()
    if key in updates:
        if key not in seen:
            result.append(f"{key}={updates[key]}\n")
            seen.add(key)
    else:
        result.append(line)
for key, value in updates.items():
    if key not in seen:
        result.append(f"{key}={value}\n")
fd, tmp = tempfile.mkstemp(prefix=".env.", dir=os.path.dirname(path), text=True)
try:
    with os.fdopen(fd, "w") as out:
        out.writelines(result)
    os.chmod(tmp, 0o600)
    os.replace(tmp, path)
except BaseException:
    try: os.unlink(tmp)
    except FileNotFoundError: pass
    raise
PY

# Validate registration before starting any container.
WORLD_SYNC_TOOL="${WORLD_SYNC_TOOL:-$REPO_DIR/../apcode-dev/minecraft/tools/world-sync.sh}"
[[ -x "$WORLD_SYNC_TOOL" ]] || fail "world-sync tool not found or not executable: $WORLD_SYNC_TOOL"
WORLDS_CONF="$REGISTRY" "$WORLD_SYNC_TOOL" worlds --check

cleanup_after_failure() {
  local status=$?
  if [[ "$status" -ne 0 ]]; then docker compose down || true; fi
}
trap cleanup_after_failure EXIT

echo "Generating world '$NAME' (seed $SEED) in $DATA_DIR ..."
docker compose up -d

echo "Waiting for Minecraft to generate $DATA_DIR/world/level.dat ..."
for _ in $(seq 1 180); do
  if [[ -s "$DATA_DIR/world/level.dat" ]]; then break; fi
  if ! docker compose ps --status running -q | grep -q .; then
    docker compose logs --tail=100 >&2
    fail "Minecraft stopped before generating the world."
  fi
  sleep 2
done
[[ -s "$DATA_DIR/world/level.dat" ]] || fail "timed out waiting for world generation."

# Stop cleanly before taking the baseline, then leave the selected world stopped
# so the developer can begin authoring from a pristine baseline.
docker compose down
WORLDS_CONF="$REGISTRY" "$WORLD_SYNC_TOOL" baseline "$NAME"
echo "Created local-only world '$NAME'; baseline stored in $BASELINE_DIR."
echo "Start it with: $SCRIPT_DIR/dev-server-up.sh"
