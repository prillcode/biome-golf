#!/usr/bin/env bash
# Build the mod and sync the selected version into dev-server/mods/biome-golf-<version>.jar
# for the Docker dev server. Fabric API is downloaded automatically by the server image.
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="${1:-}"
if [[ -z "$VERSION" || ! "$VERSION" =~ ^[A-Za-z0-9][A-Za-z0-9._+-]*$ ]]; then
	echo "Usage: ./scripts/dev-server-sync.sh <VERSION>" >&2
	echo "Example: ./scripts/dev-server-sync.sh 0.6.1" >&2
	exit 2
fi

mkdir -p dev-server/mods

echo "Building mod..."
./gradlew build -q

JAR="build/libs/biome-golf-${VERSION}.jar"

if [[ ! -f "$JAR" ]]; then
	echo "ERROR: expected build artifact not found: $JAR" >&2
	exit 1
fi

DEST="dev-server/mods/biome-golf-${VERSION}.jar"
for old_jar in dev-server/mods/biome-golf-*.jar dev-server/mods/minecraft-golf.jar; do
	if [[ -e "$old_jar" && "$old_jar" != "$DEST" ]]; then
		rm -f -- "$old_jar"
	fi
done
cp "$JAR" "$DEST"
echo "Synced $(basename "$JAR") -> $DEST"
