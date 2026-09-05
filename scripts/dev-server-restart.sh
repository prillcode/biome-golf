#!/usr/bin/env bash
# Restart the Docker dev server (same containers, same world).
set -euo pipefail
cd "$(dirname "$0")/../dev-server"

if [[ ! -f mods/minecraft-golf.jar ]]; then
	echo "ERROR: mods/minecraft-golf.jar missing. Run ../scripts/dev-server-sync.sh first." >&2
	exit 1
fi

docker compose restart minecraft
