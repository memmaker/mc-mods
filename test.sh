#!/usr/bin/env bash
# Runs every mod's game tests: a headless Minecraft server per mod, no client and no prompts.
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

    # The gametest server boots through the dedicated-server entrypoint, which complains about a
    # missing server.properties (with a stack trace) and eula.txt before the gametest path takes
    # over. Both files live in the throwaway run directory, never in the repository.
    mkdir -p "$mod/build/gametest"
    touch "$mod/build/gametest/server.properties"
    printf 'eula=true\n' > "$mod/build/gametest/eula.txt"

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
