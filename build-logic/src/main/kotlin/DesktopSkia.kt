import org.gradle.api.Project

/**
 * Compose's desktop runtime with Skia's native libraries for Linux and Windows on x64 and macOS on arm64, all on
 * every machine.
 *
 * One set for all, so the lock files hold the same thing whether the gate (Linux), the MSIX job (Windows) or a Mac
 * resolves them; each package carries the other platforms' natives too, a few megabytes it never loads.
 */
val Project.desktopSkia: List<String>
    get() {
        val version = catalog.version("composeMultiplatform")
        return listOf("linux-x64", "windows-x64", "macos-arm64").map { "org.jetbrains.compose.desktop:desktop-jvm-$it:$version" }
    }
