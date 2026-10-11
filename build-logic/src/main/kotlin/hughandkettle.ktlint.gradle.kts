// Formatting is checked, not discussed: `./gradlew ktlintCheck` is part of `build`,
// and `./gradlew ktlintFormat` fixes what it can. Style comes from .editorconfig.
plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    version.set(catalog.version("ktlint"))
}
