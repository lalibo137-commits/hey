#!/bin/bash
# Rebuilds dist/drunkard-neoforge-1.21.1-1.2.0.jar from the original NeoForge 1.1.0 jar: generates compile
# stubs, recompiles neoforge/src/, patches two game-test expectations, overlays neoforge/resources/ and repacks.
# Usage: neoforge/tools/build.sh path/to/drunkard-neoforge-1.21.1-1.1.0.jar
set -euo pipefail
VERSION=1.2.0
ORIG=$(realpath "$1"); ROOT=$(cd "$(dirname "$0")/.." && pwd); REPO=$(dirname "$ROOT"); W=$(mktemp -d)
M=https://repo1.maven.org/maven2/org/ow2/asm
CACHE="${XDG_CACHE_HOME:-$HOME/.cache}/drunkard-build"; mkdir -p "$CACHE"
for a in asm asm-tree asm-analysis; do
  [ -s "$CACHE/$a.jar" ] || curl -sSf --retry 5 --retry-delay 3 --retry-all-errors -o "$CACHE/$a.jar" "$M/$a/9.7.1/$a-9.7.1.jar"
  cp "$CACHE/$a.jar" "$W/$a.jar"
done
CP="$W/asm.jar:$W/asm-tree.jar:$W/asm-analysis.jar"
javac -nowarn -cp "$CP" -d "$W/tools" "$ROOT"/tools/*.java
# Stubs: the members the NeoForge jar references, plus the 1.21.1 members the port needs (forge-extras.tsv is
# generated from the Forge 1.2.0 jar and the MCPConfig 1.21.1 table; extras.tsv and overrides.txt are hand-checked).
cat "$ROOT/tools/auto-hierarchy.txt" "$ROOT/tools/overrides.txt" > "$W/overrides.txt"
cat "$ROOT/tools/forge-extras.tsv" "$ROOT/tools/extras.tsv" > "$W/extras.tsv"
java -cp "$W/tools:$CP" StubGen "$ORIG" "$W/stubs" "$W/overrides.txt" "$W/extras.tsv"
javac -nowarn -d "$W/stubbin" $(find "$W/stubs" -name '*.java')
mkdir -p "$W/jar" && (cd "$W/jar" && unzip -q "$ORIG")
javac -g -nowarn --release 21 -proc:none -encoding UTF-8 -cp "$W/stubbin:$W/jar" -d "$W/out" $(find "$ROOT/src" -name '*.java')
java -cp "$W/tools:$CP" NeoPatcher "$W/jar" "$W/patched"
java -cp "$W/tools:$CP" BcVerify "$W/out"
java -cp "$W/tools:$CP" BcVerify "$W/patched"
java -cp "$W/tools:$CP" ItfCheck "$ORIG" "$REPO/dist/drunkard-forge-1.20.1-1.2.0.jar" "$W/out"
# Replace every recompiled class together with all of its old inner classes.
(cd "$ROOT/src" && find . -name '*.java') | while read -r f; do
  base="${f%.java}"; rm -f "$W/jar/$base.class" "$W/jar/$base"\$*.class
done
cp -r "$W/out/com" "$W/jar/"
cp -r "$W/patched/com" "$W/jar/"
cp -r "$ROOT/resources/." "$W/jar/"
sed -i "s/^version=\"1.1.0\"/version=\"$VERSION\"/" "$W/jar/META-INF/neoforge.mods.toml"
grep -q "^version=\"$VERSION\"" "$W/jar/META-INF/neoforge.mods.toml"
mkdir -p "$REPO/dist"; OUT="$REPO/dist/drunkard-neoforge-1.21.1-$VERSION.jar"; rm -f "$OUT"
(cd "$W/jar" && zip -qX "$OUT" META-INF/MANIFEST.MF && zip -qrX "$OUT" . -x META-INF/MANIFEST.MF)
java -cp "$W/tools:$CP" LinkCheck "$OUT"
echo "built $OUT"
