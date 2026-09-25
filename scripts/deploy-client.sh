#!/usr/bin/env bash
# Build the mod and deploy the resulting JAR into a Minecraft client's mods/ folder.
#
# Usage:
#   ./scripts/deploy-client.sh <VERSION> [GAME_DIR]
#
# GAME_DIR is the client game directory (the folder that contains mods/). It defaults
# to the vanilla launcher location, "$HOME/.minecraft". Pass a custom launcher's
# instance directory to override, for example the bhmc-launcher golf instance:
#
#   GAME_DIR=$(python3 -c "import json,os;i=json.load(open(os.path.expanduser('~/.local/share/bhmc-launcher/instances.json')))['instances'];print(next(v['gameDirectory'] for v in i.values() if v['name']=='BirdieBiome - Golf'))")
#   ./scripts/deploy-client.sh 0.6.1 "$GAME_DIR"
#
# The selected versioned JAR is written as "biome-golf-<VERSION>.jar". Older Biome
# Golf JARs are removed from the target mods/ folder to prevent duplicate loading.
# Fully exit and relaunch the game afterwards; Fabric loads JARs only at startup.
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="${1:-}"
if [[ -z "$VERSION" || ! "$VERSION" =~ ^[A-Za-z0-9][A-Za-z0-9._+-]*$ ]]; then
	echo "Usage: ./scripts/deploy-client.sh <VERSION> [GAME_DIR]" >&2
	echo "Example: ./scripts/deploy-client.sh 0.6.1 \"$HOME/.minecraft\"" >&2
	exit 2
fi

GAME_DIR="${2:-$HOME/.minecraft}"

# Accept either a game directory (contains mods/) or a mods/ directory directly.
case "$(basename "$GAME_DIR")" in
	mods) MODS_DIR="$GAME_DIR" ;;
	*) MODS_DIR="$GAME_DIR/mods" ;;
esac

echo "Building mod..."
./gradlew build -q

JAR="build/libs/biome-golf-${VERSION}.jar"

if [[ ! -f "$JAR" ]]; then
	echo "ERROR: expected build artifact not found: $JAR" >&2
	exit 1
fi

mkdir -p "$MODS_DIR"
DEST="$MODS_DIR/biome-golf-${VERSION}.jar"
for old_jar in "$MODS_DIR"/biome-golf-*.jar "$MODS_DIR/minecraft-golf.jar"; do
	if [[ -e "$old_jar" && "$old_jar" != "$DEST" ]]; then
		rm -f -- "$old_jar"
	fi
done
cp "$JAR" "$DEST"

echo "Deployed $(basename "$JAR") -> $DEST"
sha256sum "$JAR" "$DEST"

echo
echo "Fully exit and relaunch the game; Fabric loads mod JARs only at startup."
