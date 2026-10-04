#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME_DIR="$ROOT/../job-monitor/.tools/jdk-21.0.12.1+1/Contents/Home"
MVN_BIN="$ROOT/../job-monitor/.tools/apache-maven-3.9.11/bin/mvn"

if [[ -x "$JAVA_HOME_DIR/bin/java" ]]; then
  export JAVA_HOME="$JAVA_HOME_DIR"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [[ -x "$MVN_BIN" ]]; then
  MVN="$MVN_BIN"
else
  MVN="mvn"
fi

cd "$ROOT"
exec "$MVN" spring-boot:run "$@"
