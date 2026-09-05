#!/usr/bin/env bash
# Reset the disposable development world.
#
# This deletes ONLY the development server's world/runtime data (the named Docker
# volume "minecraft-golf-data" and any local dev-server/data/ directory).
# It never touches source code, built mods, or any other Minecraft world.
# It must be run explicitly — it is never triggered by builds or restarts.
set -euo pipefail
cd "$(dirname "$0")/../dev-server"

VOLUME="minecraft-golf-dev_minecraft-golf-data"

echo "This will DELETE development server world/runtime data:"
echo "  - Docker volume: $VOLUME"
[[ -d data ]] && echo "  - Local directory: $(pwd)/data"
echo
echo "Source code, built mods (mods/minecraft-golf.jar) and any other worlds are NOT touched."
echo
read -r -p "Type 'reset' to confirm: " confirm
if [[ "$confirm" != "reset" ]]; then
	echo "Aborted."
	exit 1
fi

docker compose down || true
docker volume rm "$VOLUME" 2>/dev/null || echo "Volume $VOLUME not found (already clean)."
rm -rf data
echo "Development world reset."
