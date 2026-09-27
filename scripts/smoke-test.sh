#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
if ! command -v javac >/dev/null 2>&1; then
    echo 'Install JDK 17 (not only a JRE), then retry.' >&2
    exit 1
fi
mkdir -p build/smoke
javac --release 17 -encoding UTF-8 -d build/smoke \
  counter-core/src/main/java/com/gfxiong/gf/core/Counter.java \
  tools/CountImage.java tools/CoreSmokeTest.java
java -Djava.awt.headless=true -cp build/smoke CoreSmokeTest
