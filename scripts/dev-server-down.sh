#!/usr/bin/env bash
# Stop the Docker dev server.
set -euo pipefail
cd "$(dirname "$0")/../dev-server"
docker compose down
