#!/usr/bin/env bash
# Build the mod and deploy the resulting JAR into a Minecraft client's mods/ folder.
#
# Usage:
#   ./scripts/deploy-client.sh [GAME_DIR]
#
# GAME_DIR is the client game directory (the folder that contains mods/). It defaults
# to the vanilla launcher location, "$HOME/.minecraft". Pass a custom launcher's
# instance directory to override, for example the bhmc-launcher golf instance:
#
#   GAME_DIR=$(python3 -c "import json,os;i=json.load(open(os.path.expanduser('~/.local/share/bhmc-launcher/instances.json')))['instances'];print(next(v['gameDirectory'] for v in i.values() if v['name']=='BirdieBiome - Golf'))")
#   ./scripts/deploy-client.sh "$GAME_DIR"
#
# The JAR is always written as "minecraft-golf.jar" (the filename custom launchers
# expect). Fully exit and relaunch the game afterwards; Fabric loads mod JARs only at
# startup.
set -euo pipefail
cd "$(dirname "$0")/.."

GAME_DIR="${1:-$HOME/.minecraft}"

# Accept either a game directory (contains mods/) or a mods/ directory directly.
case "$(basename "$GAME_DIR")" in
	mods) MODS_DIR="$GAME_DIR" ;;
	*) MODS_DIR="$GAME_DIR/mods" ;;
esac

echo "Building mod..."
./gradlew build -q

JAR=""
while IFS= read -r candidate; do
	JAR="$candidate"
done < <(printf '%s\n' build/libs/minecraft-golf-*.jar | grep -v -- '-sources\.jar$' | sort -V)

if [[ -z "$JAR" || ! -f "$JAR" ]]; then
	echo "ERROR: no built JAR found in build/libs/" >&2
	exit 1
fi

mkdir -p "$MODS_DIR"
cp "$JAR" "$MODS_DIR/minecraft-golf.jar"

echo "Deployed $(basename "$JAR") -> $MODS_DIR/minecraft-golf.jar"
sha256sum "$JAR" "$MODS_DIR/minecraft-golf.jar"

echo
echo "Fully exit and relaunch the game; Fabric loads mod JARs only at startup."
