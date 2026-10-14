// Formatting is checked, not discussed: `./gradlew ktlintCheck` is part of `build`,
// and `./gradlew ktlintFormat` fixes what it can. Style comes from .editorconfig.
plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    version.set(catalog.version("ktlint"))
    // Code a plugin generates (Compose resources) is not ours to format.
    filter {
        exclude { it.file.path.contains("/build/generated/") }
    }
}
