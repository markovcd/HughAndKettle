#!/usr/bin/env bash
# The gate: restore, build, lint and every test, in a container that carries the JDK and the Android
# SDK, so a pass means the same thing on a laptop, in a cloud session and in CI. Output goes to
# build/gate.log and is filtered afterwards, never in the pipe. GATE_BUILD_ARGS, if set, is extra
# `docker build` arguments as one space-separated string (release.sh passes the version this way).
set -euo pipefail
cd "$(dirname "$0")"

mkdir -p build
log=build/gate.log
commit=$(git rev-parse --short=12 HEAD 2>/dev/null || echo unknown)

# Unquoted on purpose: the string is split into separate arguments, none of which hold spaces.
extra_args=${GATE_BUILD_ARGS:-}

echo "gate: building the image and running ./gradlew build (log: $log)"
if docker build --file gate.Dockerfile --target gate --tag hughandkettle-gate \
    --build-arg "COMMIT=$commit" $extra_args --progress=plain . > "$log" 2>&1; then
    echo "gate: passed"
else
    echo "gate: FAILED. First lines that say why:"
    grep -n -E "^#[0-9]+ [0-9.]+ (e: |w: |FAILED|.*BUILD FAILED|.*What went wrong|.*Test.*FAILED|.*error:)" "$log" | head -20 || true
    echo "gate: the whole log is in $log"
    exit 1
fi

rm -rf build/gate-artifacts
docker build --file gate.Dockerfile --target artifacts --output build/gate-artifacts \
    --build-arg "COMMIT=$commit" $extra_args . >> "$log" 2>&1
echo "gate: artifacts in build/gate-artifacts"
