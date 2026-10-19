#!/usr/bin/env bash
# The release, as one script. It checks and stops with a reason before anything moves, runs the gate
# on the exact commit, signs what the gate built, and only with --publish (and real keys) tags,
# pushes and creates the GitHub release. Without --publish it builds with throwaway keys and
# publishes nothing, which is how a release is tried.
#
#   ./release.sh [X.Y.Z] [--publish]
#   ./release.sh [X.Y.Z] --print-version     prints the version a release would have, and stops
#
# The Windows MSIX is built and signed on a Windows runner (desktop/packaging/msix.ps1), since jpackage
# cannot cross-build; MSIX_FILE names it, and a published release needs it.
#
# Real keys come from the environment, never from the tree:
#   RELEASE_SSH_KEY_FILE          private key pairing with release/allowed_signers (signs SHA256SUMS)
#   ANDROID_KEYSTORE_FILE         keystore that signs the APK
#   ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD
set -euo pipefail
cd "$(dirname "$0")"

publish=false
print_version=false
version=""
for arg in "$@"; do
    case "$arg" in
        --publish) publish=true ;;
        --print-version) print_version=true ;;
        *) version="$arg" ;;
    esac
done

refuse() { echo "release: refused: $*" >&2; exit 1; }

# --- Version: the next patch after the latest vX.Y.Z tag, or the one given.
git fetch --tags --quiet origin 2>/dev/null || true
latest=$(git tag --list 'v[0-9]*.[0-9]*.[0-9]*' --sort=-version:refname | head -1)
if [ -z "$version" ]; then
    if [ -z "$latest" ]; then version="0.1.0"; else
        IFS=. read -r major minor patch <<< "${latest#v}"
        version="$major.$minor.$((patch + 1))"
    fi
fi
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || refuse "version must be X.Y.Z, got '$version'"
tag="v$version"
if $print_version; then echo "$version"; exit 0; fi

# --- Checks, stopping on the first that fails.
[ -z "$(git status --porcelain)" ] || refuse "the tree is not clean"
git rev-parse --verify --quiet "refs/tags/$tag" > /dev/null && refuse "tag $tag already exists"
git ls-remote --exit-code --tags origin "refs/tags/$tag" > /dev/null 2>&1 && refuse "tag $tag already exists on origin"
branch=$(git rev-parse --abbrev-ref HEAD)
[ "$branch" = "main" ] || refuse "releases are cut from main, not '$branch'"
if git rev-parse --verify --quiet origin/main > /dev/null; then
    [ "$(git rev-parse HEAD)" = "$(git rev-parse origin/main)" ] || refuse "HEAD is not the tip of origin/main"
fi
notes=$(awk '/^## Unreleased/{on=1; next} /^## /{on=0} on' CHANGELOG.md | sed '/^[[:space:]]*$/d')
[ -n "$notes" ] || refuse "CHANGELOG.md has no Unreleased entries"

# --- Keys: real ones must pair with what is committed; otherwise throwaway ones, and no publishing.
out=build/release
rm -rf "$out" && mkdir -p "$out"
throwaway=false
if [ -n "${RELEASE_SSH_KEY_FILE:-}" ]; then
    [ -f release/allowed_signers ] || refuse "release/allowed_signers is missing; see TODO.md"
    grep -q -F "$(ssh-keygen -y -f "$RELEASE_SSH_KEY_FILE" | awk '{print $2}')" release/allowed_signers \
        || refuse "RELEASE_SSH_KEY_FILE does not pair with release/allowed_signers"
    ssh_key="$RELEASE_SSH_KEY_FILE"
    signers=release/allowed_signers
else
    throwaway=true
    ssh-keygen -q -t ed25519 -N "" -C hughandkettle-throwaway -f "$out/throwaway" > /dev/null
    ssh_key="$out/throwaway"
    echo "hughandkettle $(cat "$out/throwaway.pub")" > "$out/throwaway_signers"
    signers="$out/throwaway_signers"
fi
if [ -n "${ANDROID_KEYSTORE_FILE:-}" ]; then
    : "${ANDROID_KEYSTORE_PASSWORD:?}" "${ANDROID_KEY_ALIAS:?}" "${ANDROID_KEY_PASSWORD:?}"
    keystore="$ANDROID_KEYSTORE_FILE"
else
    throwaway=true
    export ANDROID_KEYSTORE_PASSWORD=throwaway ANDROID_KEY_ALIAS=throwaway ANDROID_KEY_PASSWORD=throwaway
    keytool -genkeypair -keystore "$out/throwaway.jks" -storepass throwaway -keypass throwaway -alias throwaway \
        -keyalg RSA -keysize 2048 -validity 1 -dname "CN=throwaway" > /dev/null 2>&1
    keystore="$out/throwaway.jks"
fi
if $publish && $throwaway; then refuse "--publish needs the real keys; see the header of this script"; fi
if [ -n "${MSIX_FILE:-}" ]; then
    [ -f "$MSIX_FILE" ] || refuse "MSIX_FILE '$MSIX_FILE' does not exist"
elif $publish; then
    refuse "--publish needs MSIX_FILE, the Windows package; see the header of this script"
fi

# --- The gate, on this exact commit, stamped with the version.
GATE_BUILD_ARGS="--build-arg RELEASE_VERSION=$version" ./gate.sh
cp build/gate-artifacts/apk/release/app-release-unsigned.apk "$out/unsigned.apk"
cp build/gate-artifacts/cli/hk-*.zip "$out/hk-$version.zip" 2>/dev/null || cp build/gate-artifacts/cli/*.zip "$out/hk-$version.zip"
cp build/gate-artifacts/desktop-packages/hughandkettle_*.deb "$out/hughandkettle-$version.deb"
cp build/gate-artifacts/desktop-packages/hughandkettle-linux-x64.tar.gz "$out/hughandkettle-$version-linux-x64.tar.gz"
packages=("hughandkettle-$version.apk" "hk-$version.zip" "hughandkettle-$version.deb" "hughandkettle-$version-linux-x64.tar.gz")
if [ -n "${MSIX_FILE:-}" ]; then
    cp "$MSIX_FILE" "$out/hughandkettle-$version.msix"
    packages+=("hughandkettle-$version.msix")
fi

# --- Sign the APK with apksigner from the gate image's build tools, and the checksums with the SSH key.
docker run --rm \
    -v "$PWD/$out:/release" -v "$(realpath "$keystore"):/keys/release.jks:ro" \
    -e ANDROID_KEYSTORE_PASSWORD -e ANDROID_KEY_PASSWORD \
    hughandkettle-gate sh -c 'apksigner sign --ks /keys/release.jks --ks-key-alias "$0" \
        --ks-pass env:ANDROID_KEYSTORE_PASSWORD --key-pass env:ANDROID_KEY_PASSWORD \
        --out /release/hughandkettle-'"$version"'.apk /release/unsigned.apk' "$ANDROID_KEY_ALIAS"
rm "$out/unsigned.apk"
(cd "$out" && sha256sum "${packages[@]}" > SHA256SUMS)
ssh-keygen -Y sign -f "$ssh_key" -n hughandkettle "$out/SHA256SUMS" > /dev/null
ssh-keygen -Y verify -f "$signers" -I hughandkettle -n hughandkettle -s "$out/SHA256SUMS.sig" < "$out/SHA256SUMS" > /dev/null \
    || refuse "the signature does not verify against $signers"

echo "release: $tag built and signed in $out"
if ! $publish; then
    $throwaway && echo "release: throwaway keys; nothing is published. Notes would be:"
    echo "$notes"
    exit 0
fi

# --- Publish: changelog section, commit, tag, push, GitHub release. Only here does anything move.
today=$(date -u +%Y-%m-%d)
awk -v v="$version" -v d="$today" '
    /^## Unreleased/ { print; print ""; print "## " v " (" d ")"; next }
    { print }
' CHANGELOG.md > "$out/CHANGELOG.md" && mv "$out/CHANGELOG.md" CHANGELOG.md
git add CHANGELOG.md
git commit -q -m "Release $tag" -m "The Unreleased section of the changelog is now the section for $tag."
git tag -a "$tag" -m "Release $tag"
git push origin main "$tag"
gh release create "$tag" --title "$tag" --notes "$notes" \
    "${packages[@]/#/$out/}" "$out/SHA256SUMS" "$out/SHA256SUMS.sig"
echo "release: $tag published"
