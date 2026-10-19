# syntax=docker/dockerfile:1
# The gate image. Stage `gate` carries JDK 21 and the Android SDK, pinned, and runs `./gradlew build`:
# restore against the lock files, compile, ktlint, Android Lint, every test. Nothing in a later stage
# can exist unless that passed. Stage `artifacts` is what gate.sh and release.sh copy out.
#
# eclipse-temurin:21-jdk, pinned by digest. Dependabot proposes moves within 21; the major is the
# toolchain's `jdk` in gradle/libs.versions.toml and moves with it, since the image carries the only JDK.
FROM eclipse-temurin:21-jdk@sha256:3e3c176ffed168beb42c607be9bc1639b466cf00261a0fb04425562c9d0c5c2b AS gate

# The Android command-line tools zip and its published checksum, from developer.android.com/studio.
ARG CMDLINE_TOOLS_ZIP=commandlinetools-linux-15859902_latest.zip
ARG CMDLINE_TOOLS_SHA256=4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583
# The SDK packages the build needs; compileSdk and build tools are pinned in gradle/libs.versions.toml,
# and these have to agree with it.
ARG SDK_PACKAGES="platform-tools platforms;android-37.0 build-tools;37.0.0"

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# libgl1, libx11-6 and libfontconfig1: Skia's native library, which the desktop UI tests load, links them.
# fakeroot and binutils: jpackage builds the desktop .deb with them.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip ca-certificates libgl1 libx11-6 libfontconfig1 \
       fakeroot binutils \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p $ANDROID_HOME/cmdline-tools \
    && curl -fsSL -o /tmp/cmdline-tools.zip "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP" \
    && echo "$CMDLINE_TOOLS_SHA256  /tmp/cmdline-tools.zip" | sha256sum -c - \
    && unzip -q /tmp/cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools \
    && mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip \
    && yes | sdkmanager --licenses > /dev/null \
    && sdkmanager $SDK_PACKAGES > /dev/null

WORKDIR /src

# The wrapper first, so the Gradle distribution is cached across source changes.
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN ./gradlew --version --no-daemon

# The commit the build stamps on a dev version; gate.sh passes it, since .git is not in the context.
ARG COMMIT=unknown
ENV HK_COMMIT=$COMMIT
# A release passes its version; everything else builds as 0.0.0-dev.
ARG RELEASE_VERSION=
ENV ORG_GRADLE_PROJECT_releaseVersion=$RELEASE_VERSION

COPY . .
RUN ./gradlew build --no-daemon --console=plain
# The desktop app's Linux packages, from what passed: the .deb and the portable folder as a tarball.
RUN ./gradlew :desktop:packageDeb :desktop:portableLinux --no-daemon --console=plain

FROM scratch AS artifacts
COPY --from=gate /src/app/build/outputs/apk /apk
COPY --from=gate /src/cli/build/distributions /cli
COPY --from=gate /src/desktop/build/install /desktop
COPY --from=gate /src/desktop/build/compose/binaries/main/deb /desktop-packages
COPY --from=gate /src/desktop/build/packages /desktop-packages
COPY --from=gate /src/midi/build/test-results /test-results/midi
COPY --from=gate /src/amp/build/test-results /test-results/amp
COPY --from=gate /src/wmi/build/test-results /test-results/wmi
COPY --from=gate /src/cli/build/test-results /test-results/cli
COPY --from=gate /src/ui/build/test-results /test-results/ui
COPY --from=gate /src/app/build/reports /reports/app
