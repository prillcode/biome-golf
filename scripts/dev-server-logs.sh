#!/usr/bin/env bash
# Stream the Docker dev server logs.
set -euo pipefail
cd "$(dirname "$0")/../dev-server"
docker compose logs -f
