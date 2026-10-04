#!/bin/bash
# Rebuilds dist/ from the original 1.6.0 jar: generates compile stubs, recompiles src/,
# patches the rest, overlays resources/ and repacks the jar.
# Usage: tools/build.sh path/to/drunkard-forge-1.20.1-1.6.0.jar
set -euo pipefail
VERSION=1.2.0
ORIG=$(realpath "$1"); ROOT=$(cd "$(dirname "$0")/.." && pwd); W=$(mktemp -d)
M=https://repo1.maven.org/maven2/org/ow2/asm
CACHE="${XDG_CACHE_HOME:-$HOME/.cache}/drunkard-build"; mkdir -p "$CACHE"
for a in asm asm-tree; do
  [ -s "$CACHE/$a.jar" ] || curl -sSf --retry 5 --retry-delay 3 --retry-all-errors -o "$CACHE/$a.jar" "$M/$a/9.7.1/$a-9.7.1.jar"
  cp "$CACHE/$a.jar" "$W/$a.jar"
done
CP="$W/asm.jar:$W/asm-tree.jar"
javac -cp "$CP" -d "$W/tools" "$ROOT"/tools/*.java
java -cp "$W/tools:$CP" StubGen "$ORIG" "$W/stubs" "$ROOT/tools/overrides.txt"
javac -nowarn -d "$W/stubbin" $(find "$W/stubs" -name '*.java')
mkdir -p "$W/jar" && (cd "$W/jar" && unzip -q "$ORIG")
javac -g -nowarn --release 17 -proc:none -cp "$W/stubbin:$W/jar" -d "$W/out" $(find "$ROOT/src" -name '*.java')
java -cp "$W/tools:$CP" Patcher "$W/jar" "$W/out"
java -cp "$W/tools:$CP" NameCheck "$W/jar" "$W/out"
# Replace every recompiled class together with all of its old inner classes.
(cd "$ROOT/src" && find . -name '*.java') | while read -r f; do
  base="${f%.java}"; rm -f "$W/jar/$base.class" "$W/jar/$base"\$*.class
done
cp -r "$W/out/com" "$W/jar/"
cp -r "$ROOT/resources/." "$W/jar/"
sed -i "s/^version=\"1.6.0\"/version=\"$VERSION\"/" "$W/jar/META-INF/mods.toml"
sed -i "s/^Implementation-Version: 1.6.0/Implementation-Version: $VERSION/" "$W/jar/META-INF/MANIFEST.MF"
mkdir -p "$ROOT/dist"; OUT="$ROOT/dist/drunkard-forge-1.20.1-$VERSION.jar"; rm -f "$OUT"
(cd "$W/jar" && zip -qrX "$OUT" META-INF/MANIFEST.MF && zip -qrX "$OUT" . -x META-INF/MANIFEST.MF)
echo "built $OUT"
