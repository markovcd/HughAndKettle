import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * The version the build stamps on its artifacts.
 *
 * A release passes `-PreleaseVersion=X.Y.Z` (release.sh reads it from the tag);
 * every other build is `0.0.0-dev+<commit>` and can never pass for a release.
 */
data class BuildVersion(val name: String, val code: Int) {
    val isRelease: Boolean get() = !name.contains("-dev")
}

fun Project.buildVersion(): BuildVersion {
    // The gate image always sets the property; an empty value is "not a release".
    val release = providers.gradleProperty("releaseVersion").orNull?.takeIf(String::isNotBlank)
    if (release != null) {
        val match = Regex("""^(\d+)\.(\d+)\.(\d+)$""").matchEntire(release)
            ?: error("releaseVersion must be X.Y.Z, got '$release'")
        val (major, minor, patch) = match.destructured.toList().map(String::toInt)
        require(minor < 100 && patch < 100) { "releaseVersion parts above 99 do not fit the version code" }
        return BuildVersion(release, major * 10_000 + minor * 100 + patch)
    }
    return BuildVersion("0.0.0-dev+${commit().get()}", 1)
}

private fun Project.commit(): Provider<String> =
    providers.exec {
        commandLine("git", "rev-parse", "--short=12", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim().ifEmpty { "unknown" } }
