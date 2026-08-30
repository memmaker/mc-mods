#!/usr/bin/env bash
# Builds every mod (build.sh) and copies the jars into a Minecraft mods folder.
# Usage: ./deploy.sh [mods-folder]
set -euo pipefail
cd "$(dirname "$0")"

MODS=${1:-"$HOME/Library/Application Support/ModrinthApp/profiles/Vanilla with Extras/mods"}
[ -d "$MODS" ] || { echo "no such mods folder: $MODS" >&2; exit 1; }

./build.sh
cp -v *.jar "$MODS/"
