import org.gradle.api.Project

/**
 * Compose's desktop runtime with Skia's native library for the machine running the build.
 *
 * The lock files hold the gate's (Linux x64) one; packaging for each platform is the release's job.
 */
val Project.desktopSkia: String
    get() {
        val os = System.getProperty("os.name").lowercase()
        val platform =
            when {
                os.startsWith("windows") -> "windows"
                os.startsWith("mac") -> "macos"
                else -> "linux"
            }
        val arch = if (System.getProperty("os.arch") in setOf("aarch64", "arm64")) "arm64" else "x64"
        return "org.jetbrains.compose.desktop:desktop-jvm-$platform-$arch:${catalog.version("composeMultiplatform")}"
    }
