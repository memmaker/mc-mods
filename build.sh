#!/usr/bin/env bash
# Builds every Fabric mod in the subfolders and drops the release jars next to this script.
set -uo pipefail
shopt -s nullglob

cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@25}"

failed=()

for gradlew in */gradlew; do
    mod=$(dirname "$gradlew")
    echo "==> $mod"

    if ! (cd "$mod" && ./gradlew build --quiet); then
        failed+=("$mod")
        continue
    fi

    for jar in "$mod"/build/libs/*.jar; do
        case "$jar" in
            *-sources.jar | *-dev.jar | *-shadow.jar) continue ;;
        esac
        cp "$jar" .
        echo "    $(basename "$jar")"
    done
done

if [ ${#failed[@]} -gt 0 ]; then
    echo "FAILED: ${failed[*]}" >&2
    exit 1
fi
