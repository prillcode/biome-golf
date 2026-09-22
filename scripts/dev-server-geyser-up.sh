#!/usr/bin/env bash
# Start the Docker dev server with the Geyser/Floodgate Bedrock overlay (M10.0 spike).
# Detached. The base compose file is unchanged; this layers docker-compose.geyser.yml
# on top of it.
set -euo pipefail
cd "$(dirname "$0")/../dev-server"

if [[ ! -f mods/minecraft-golf.jar ]]; then
	echo "ERROR: mods/minecraft-golf.jar missing. Run ../scripts/dev-server-sync.sh first." >&2
	exit 1
fi

COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.geyser.yml)
"${COMPOSE[@]}" up -d

# Geyser generates its config on first boot. Pin Floodgate auth so Bedrock players do
# not need a linked Java account. Idempotent: only patches the generated default once.
CONFIG=/data/config/Geyser-Fabric/config.yml
for _ in $(seq 1 90); do
	if docker exec minecraft-golf-dev test -f "$CONFIG" 2>/dev/null; then
		break
	fi
	sleep 2
done

if docker exec minecraft-golf-dev grep -qE '^[[:space:]]*auth-type: online' "$CONFIG" 2>/dev/null; then
	docker exec minecraft-golf-dev sed -i -E 's/^([[:space:]]*)auth-type: online/\1auth-type: floodgate/' "$CONFIG"
	echo "Geyser auth-type -> floodgate; restarting to apply"
	"${COMPOSE[@]}" restart minecraft
else
	echo "Geyser auth-type already configured (or config not found); no restart needed"
fi
