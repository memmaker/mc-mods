#!/usr/bin/env bash
# Builds every mod (build.sh) and copies the jars into a Minecraft mods folder.
# Usage: ./deploy.sh [mods-folder]
set -euo pipefail
shopt -s nullglob
cd "$(dirname "$0")"

MODS=${1:-"$HOME/Library/Application Support/ModrinthApp/profiles/Vanilla with Extras/mods"}
[ -d "$MODS" ] || { echo "no such mods folder: $MODS" >&2; exit 1; }

./build.sh

# build.sh leaves exactly one jar per mod here; drop older versions of those
# same mods from the target folder so nothing stale gets loaded alongside
for jar in *.jar; do
    prefix=$(echo "$jar" | sed -E 's/-[0-9][^-]*\.jar$//')
    for old in "$MODS/$prefix"-*.jar; do
        [ "$(basename "$old")" = "$jar" ] || rm -v "$old"
    done
done

cp -v *.jar "$MODS/"
