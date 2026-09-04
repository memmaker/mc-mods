#!/usr/bin/env bash
# Builds every Fabric mod in the subfolders and drops the release jars next to this script.
set -uo pipefail
shopt -s nullglob

cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@25}"
export GRADLE_OPTS="${GRADLE_OPTS:-} --enable-native-access=ALL-UNNAMED"

failed=()

for gradlew in */gradlew; do
    mod=$(dirname "$gradlew")
    echo "==> $mod"

    # stale jars from earlier version bumps live on in build/libs; drop them so
    # whatever remains after the build is exactly this version
    rm -f "$mod"/build/libs/*.jar

    if ! (cd "$mod" && ./gradlew build --quiet); then
        failed+=("$mod")
        continue
    fi

    for jar in "$mod"/build/libs/*.jar; do
        case "$jar" in
            *-sources.jar | *-dev.jar | *-shadow.jar) continue ;;
        esac
        # drop older versions of this same mod from the repo root
        rm -f "$(basename "$jar" | sed -E 's/-[0-9][^-]*\.jar$//')"-*.jar
        cp "$jar" .
        echo "    $(basename "$jar")"
    done
done

if [ ${#failed[@]} -gt 0 ]; then
    echo "FAILED: ${failed[*]}" >&2
    exit 1
fi
