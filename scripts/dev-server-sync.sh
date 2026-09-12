#!/usr/bin/env bash
# Build the mod and sync the resulting JAR into dev-server/mods/minecraft-golf.jar
# for the Docker dev server. Fabric API is downloaded automatically by the server image.
set -euo pipefail
cd "$(dirname "$0")/.."

mkdir -p dev-server/mods

echo "Building mod..."
./gradlew build -q

JAR=""
while IFS= read -r candidate; do
	JAR="$candidate"
done < <(printf '%s\n' build/libs/minecraft-golf-*.jar | grep -v -- '-sources\.jar$' | sort -V)

if [[ -z "$JAR" ]]; then
	echo "ERROR: no built JAR found in build/libs/" >&2
	exit 1
fi

cp "$JAR" dev-server/mods/minecraft-golf.jar
echo "Synced $(basename "$JAR") -> dev-server/mods/minecraft-golf.jar"
