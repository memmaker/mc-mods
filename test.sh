#!/usr/bin/env bash
# Runs every mod's game tests: a headless Minecraft server per mod, no client and no EULA.
# One mod failing does not stop the others; failures are listed at the end.
set -uo pipefail
shopt -s nullglob

cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@25}"
export GRADLE_OPTS="${GRADLE_OPTS:-} --enable-native-access=ALL-UNNAMED"

failed=()

for gradlew in */gradlew; do
    mod=$(dirname "$gradlew")

    if ! grep -q fabric-gametest "$mod/src/main/resources/fabric.mod.json" 2>/dev/null; then
        echo "==> $mod (no game tests)"
        continue
    fi

    echo "==> $mod"

    if (cd "$mod" && ./gradlew runGametest --quiet); then
        echo "    passed"
    else
        failed+=("$mod")
    fi
done

if [ ${#failed[@]} -gt 0 ]; then
    echo "FAILED: ${failed[*]}" >&2
    exit 1
fi
