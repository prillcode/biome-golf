#!/usr/bin/env bash
# Start the Docker dev server (detached).
set -euo pipefail
cd "$(dirname "$0")/../dev-server"

if ! compgen -G 'mods/biome-golf-*.jar' >/dev/null; then
	echo "ERROR: no biome-golf-<version>.jar found in mods/. Run ../scripts/dev-server-sync.sh <VERSION> first." >&2
	exit 1
fi

docker compose up -d
