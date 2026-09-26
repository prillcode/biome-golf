#!/usr/bin/env bash
# Build the BirdieBiome server deployment bundle consumed by apcode.dev.
#
# The bundle is a single, CONSISTENTLY NAMED tar.gz containing:
#   - docker-compose.yml          (the parameterised server compose)
#   - docker-compose.geyser.yml   (Bedrock/unmodified-Java visitor-mode overlay)
#   - mods/<mod jar>
#
# Because the asset name never changes, the deployment can point at a stable URL:
#   https://github.com/prillcode/biome-golf/releases/latest/download/birdie-biome-server.tar.gz
# so a mod release needs no deployment-side configuration change.
#
# Usage:
#   ./scripts/build-server-bundle.sh <version> /path/to/biome-golf-<version>.jar [outdir]
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="${1:-}"
JAR="${2:-}"
OUTDIR="${3:-$PWD/dist-server}"

if [[ -z "$VERSION" || -z "$JAR" || ! -f "$JAR" ]]; then
	echo "Usage: $0 <version> /path/to/biome-golf-<version>.jar [outdir]" >&2
	exit 2
fi

mkdir -p "$OUTDIR"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

mkdir -p "$STAGE/mods"
cp dev-server/docker-compose.yml dev-server/docker-compose.geyser.yml "$STAGE/"
cp -- "$JAR" "$STAGE/mods/$(basename "$JAR")"

ARCHIVE="$OUTDIR/birdie-biome-server.tar.gz"
# Deterministic archive so identical inputs yield identical bytes/checksum.
tar --sort=name --mtime='UTC 2020-01-01' --owner=0 --group=0 --numeric-owner \
	-czf "$ARCHIVE" -C "$STAGE" .

sha256sum "$ARCHIVE" | tee "$ARCHIVE.sha256"
echo "Built $ARCHIVE (mod version ${VERSION})"
