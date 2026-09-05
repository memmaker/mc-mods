#!/usr/bin/env bash
set -euo pipefail
UPSTREAM_REPO="https://github.com/chrrs/camerapture.git"
UPSTREAM_REF="26.2"
TMPDIR=$(mktemp -d)
DEST_DIR="$(pwd)/photo-safari/vendor/camerapture"

echo "Cloning upstream Camerapture (${UPSTREAM_REF}) into $TMPDIR"
git clone --depth=1 --branch "${UPSTREAM_REF}" "${UPSTREAM_REPO}" "$TMPDIR/camerapture" >/dev/null

echo "Preparing destination: $DEST_DIR"
rm -rf "$DEST_DIR"
mkdir -p "$DEST_DIR"

echo "Copying common and fabric subprojects"
# Copy the common and fabric folders (sources + resources). Keep their internal layout.
rsync -a --progress "$TMPDIR/camerapture/common" "$DEST_DIR/" --exclude '.git' --exclude 'build' --exclude '.gradle'
rsync -a --progress "$TMPDIR/camerapture/fabric" "$DEST_DIR/" --exclude '.git' --exclude 'build' --exclude '.gradle'

# Copy gradle.properties and mixin/accesswidener if needed (optional)
if [ -f "$TMPDIR/camerapture/LICENSE" ]; then
  cp "$TMPDIR/camerapture/LICENSE" "$DEST_DIR/LICENSE"
fi

# Package rename: me.chrr.camerapture -> me.memmaker.photosafari.camerapture
echo "Renaming Java packages and imports (me.chrr.camerapture -> me.memmaker.photosafari.camerapture)"
pushd "$DEST_DIR" >/dev/null
# Replace package declarations
find . -type f -name "*.java" -print0 | xargs -0 sed -i '' -e 's/package me\.chrr\.camerapture/package me.memmaker.photosafari.camerapture/g' 2>/dev/null || \
find . -type f -name "*.java" -print0 | xargs -0 sed -i -e 's/package me\.chrr\.camerapture/package me.memmaker.photosafari.camerapture/g'
# Replace imports
find . -type f -name "*.java" -print0 | xargs -0 sed -i '' -e 's/import me\.chrr\.camerapture/import me.memmaker.photosafari.camerapture/g' 2>/dev/null || \
find . -type f -name "*.java" -print0 | xargs -0 sed -i -e 's/import me\.chrr\.camerapture/import me.memmaker.photosafari.camerapture/g'
# Also update package references in mixin jsons or other Java code that uses strings
# Replace occurrences of "me.chrr.camerapture" in text files
grep -RIl "me.chrr.camerapture" . | xargs -r sed -i '' -e 's/me\.chrr\.camerapture/me.memmaker.photosafari.camerapture/g' 2>/dev/null || \
grep -RIl "me.chrr.camerapture" . | xargs -r sed -i -e 's/me\.chrr\.camerapture/me.memmaker.photosafari.camerapture/g'
popd >/dev/null

echo "Wrote upstream LICENSE to $DEST_DIR/LICENSE"
echo "Writing NOTICE file"
cat > "$DEST_DIR/NOTICE" <<'EOF'
This directory contains vendored sources from chrrs/camerapture (MIT).
Original upstream: https://github.com/chrrs/camerapture (branch 26.2)
The original code is MIT-licensed; LICENSE is included.
We changed the Java package namespace to me.memmaker.photosafari.camerapture.
EOF

echo "Done. Please inspect $DEST_DIR, then run './gradlew build' from photo-safari to test the build."