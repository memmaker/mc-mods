#!/usr/bin/env bash
# Publishes mods to Modrinth: creates the project as a draft, then uploads the jar as a version.
# Usage: ./publish.sh [mod-folder ...]   (no arguments publishes every mod)
# Drafts are NOT submitted for review — open the project page and submit it yourself, which is the
# step that eventually makes it searchable.
set -euo pipefail
cd "$(dirname "$0")"

API=https://api.modrinth.com/v2
: "${MODRINTH_TOKEN:?set MODRINTH_TOKEN to a Modrinth token with PROJECT_CREATE and VERSION_CREATE scopes (https://modrinth.com/settings/pats)}"

prop() { sed -n "s/^$2=//p" "$1/gradle.properties" | tr -d '\r'; }

api() { curl -sS -H "Authorization: $MODRINTH_TOKEN" "$@"; }

# Prints the "id" of an API response, or the API's error and exits.
id_of() {
    python3 -c '
import json, sys
d = json.load(sys.stdin)
if "id" not in d:
    sys.exit("    " + sys.argv[1] + " failed: " + str(d.get("description") or d))
print(d["id"])
' "$1"
}

publish() {
    local dir=$1 slug=$2 title=$3 summary=$4 categories=$5 dependencies=${6:-[]}

    local version mc jarbase jar icon tmp status
    version=$(prop "$dir" mod_version)
    mc=$(prop "$dir" minecraft_version)
    jarbase=$(prop "$dir" archives_base_name)
    jar="$dir/build/libs/$jarbase-$version.jar"

    if [ ! -f "$jar" ]; then
        echo "!! $dir: $jar missing, run ./build.sh first" >&2
        return 1
    fi

    icon=$(ls "$dir/icon.png" "$dir"/src/main/resources/assets/*/icon.png 2>/dev/null | head -1 || true)
    tmp=$(mktemp -d)

    echo "==> $title ($slug) $version for Minecraft $mc"

    # README is the project page body; the one-liner is the search-result summary.
    DIR="$dir" SLUG="$slug" TITLE="$title" SUMMARY="$summary" CATEGORIES="$categories" \
        python3 - "$tmp/project.json" <<'PY'
import json, os, pathlib, sys

readme = pathlib.Path(os.environ["DIR"], "README.md")
json.dump({
    "project_type": "mod",
    "slug": os.environ["SLUG"],
    "title": os.environ["TITLE"],
    "description": os.environ["SUMMARY"],
    "body": readme.read_text() if readme.exists() else os.environ["SUMMARY"],
    "categories": json.loads(os.environ["CATEGORIES"]),
    "client_side": "required",
    "server_side": "required",
    "license_id": "MIT",
    "is_draft": True,
    "initial_versions": [],
}, open(sys.argv[1], "w"))
PY

    SLUG="$slug" TITLE="$title" VERSION="$version" MC="$mc" DEPS="$dependencies" \
        python3 - "$tmp/version.json" <<'PY'
import json, os, sys

json.dump({
    "project_id": os.environ["SLUG"],
    "name": os.environ["TITLE"] + " " + os.environ["VERSION"],
    "version_number": os.environ["VERSION"],
    "version_type": "release",
    "loaders": ["fabric"],
    "game_versions": [os.environ["MC"]],
    "featured": True,
    "dependencies": json.loads(os.environ["DEPS"]),
    "file_parts": ["file"],
    "primary_file": "file",
}, open(sys.argv[1], "w"))
PY

    local project_id
    status=$(curl -sS -o /dev/null -w "%{http_code}" "$API/project/$slug")
    if [ "$status" = "404" ]; then
        local args=(-X POST "$API/project" -F "data=<$tmp/project.json;type=application/json")
        [ -n "$icon" ] && args+=(-F "icon=@$icon")
        project_id=$(api "${args[@]}" | id_of "create project")
        echo "    created draft $project_id"
    else
        project_id=$(curl -sS "$API/project/$slug" | id_of "look up project")
        echo "    project $slug already exists ($project_id), adding a version to it"
    fi

    # The version payload needs the base62 project id, which only exists once the project does.
    python3 -c '
import json, sys
data = json.load(open(sys.argv[1]))
data["project_id"] = sys.argv[2]
json.dump(data, open(sys.argv[1], "w"))
' "$tmp/version.json" "$project_id"

    local version_id
    version_id=$(api -X POST "$API/version" \
        -F "data=<$tmp/version.json;type=application/json" \
        -F "file=@$jar" | id_of "upload version")
    echo "    uploaded version $version_id"

    rm -rf "$tmp"
    echo "    then submit it at https://modrinth.com/mod/$slug/settings"
}

TARGETS=("$@")

wanted() {
    if [ ${#TARGETS[@]} -eq 0 ]; then return 0; fi
    local target
    for target in "${TARGETS[@]}"; do
        if [ "$target" = "$1" ]; then return 0; fi
    done
    return 1
}

if wanted white-flag; then
    publish white-flag white-flag "White Flag" \
    "Craft a white flag. Carry it in your hotbar and every hostile mob stops attacking you." \
        '["game-mechanics","mobs"]'
fi

if wanted plush-boots; then
    publish plush-boots plush-boots "Plush Boots" \
        "Craft plush boots. Wear them and falling stops hurting." \
        '["equipment","game-mechanics"]'
fi

if wanted photo-safari; then
    publish photo-safari photo-safari "Photo Safari" \
    "Photograph every kind of living creature in the world. Turns Camerapture photography into a collection game." \
        '["adventure","game-mechanics"]' \
        '[{"project_id":"9dzLWnmZ","dependency_type":"required"}]'
fi

if wanted glider; then
    publish glider explorercraft-glider "Glider" \
        "Craft a glider. Hold it as you fall and the canopy opens: you drift down slowly and steer where you like." \
        '["adventure","equipment","game-mechanics"]'
fi

if wanted miniature-rebreather; then
    publish miniature-rebreather miniature-rebreather "Miniature Rebreather" \
        "Craft a miniature rebreather. Wear it in the helmet slot and you never run out of air underwater." \
        '["equipment","game-mechanics"]'
fi

if wanted climbing-claws; then
    publish climbing-claws climbing-claws "Climbing Claws" \
        "Craft climbing claws from any metal. Keep them in your hotbar and every vertical wall becomes a ladder." \
        '["equipment","game-mechanics"]'
fi

if wanted fireproof-suit; then
    publish fireproof-suit fireproof-suit "Fireproof Suit" \
        "Craft a magma-forged armour set. Each piece cuts fire and lava damage by a quarter; the full suit makes you immune." \
        '["equipment","game-mechanics"]'
fi

if wanted fx-globals; then
    publish fx-globals fx-globals "FX Globals" \
        "World pacing tweaks: days last 1.5x longer and hunger drains at a quarter of the usual rate." \
        '["game-mechanics"]' \
        '[{"project_id":"mOgUt4GM","dependency_type":"optional"}]'
fi
