#!/bin/bash
# Rebuilds dist/ from the original 1.6.0 jar: generates compile stubs, recompiles src/, patches the rest.
# Usage: tools/build.sh path/to/drunkard-forge-1.20.1-1.6.0.jar
set -euo pipefail
ORIG=$(realpath "$1"); ROOT=$(cd "$(dirname "$0")/.." && pwd); W=$(mktemp -d)
M=https://repo1.maven.org/maven2/org/ow2/asm
for a in asm asm-tree; do curl -sSfo "$W/$a.jar" "$M/$a/9.7.1/$a-9.7.1.jar"; done
CP="$W/asm.jar:$W/asm-tree.jar"
javac -cp "$CP" -d "$W/tools" "$ROOT"/tools/*.java
java -cp "$W/tools:$CP" StubGen "$ORIG" "$W/stubs" "$ROOT/tools/overrides.txt"
javac -nowarn -d "$W/stubbin" $(find "$W/stubs" -name '*.java')
mkdir -p "$W/jar" && (cd "$W/jar" && unzip -q "$ORIG")
javac -nowarn --release 17 -proc:none -cp "$W/stubbin:$W/jar" -d "$W/out" $(find "$ROOT/src" -name '*.java')
java -cp "$W/tools:$CP" Patcher "$W/jar" "$W/out"
java -cp "$W/tools:$CP" NameCheck "$W/jar" "$W/out"
cp -r "$W/out/com" "$W/jar/" && rm -f "$W/jar/com/drunkard/BarWorker\$1.class"
sed -i 's/^version="1.6.0"/version="1.6.1"/' "$W/jar/META-INF/mods.toml"
sed -i 's/^Implementation-Version: 1.6.0/Implementation-Version: 1.6.1/' "$W/jar/META-INF/MANIFEST.MF"
OUT="$ROOT/dist/drunkard-forge-1.20.1-1.6.1.jar"; rm -f "$OUT"
(cd "$W/jar" && zip -qrX "$OUT" META-INF/MANIFEST.MF && zip -qrX "$OUT" . -x META-INF/MANIFEST.MF)
echo "built $OUT"
