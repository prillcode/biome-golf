#!/usr/bin/env bash
# Restart the Docker dev server (same containers, same world).
set -euo pipefail
cd "$(dirname "$0")/../dev-server"

if ! compgen -G 'mods/biome-golf-*.jar' >/dev/null; then
	echo "ERROR: no biome-golf-<version>.jar found in mods/. Run ../scripts/dev-server-sync.sh <VERSION> first." >&2
	exit 1
fi

# The Docker data volume persists copied mods. Remove the previous Biome Golf JAR
# before restart so the image can stage the new version without loading duplicates.
docker compose exec -T minecraft sh -c 'rm -f /data/mods/minecraft-golf.jar /data/mods/biome-golf-*.jar'
docker compose restart minecraft
