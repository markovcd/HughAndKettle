import org.gradle.api.Project

/**
 * Compose's desktop runtime with Skia's native libraries for Linux and Windows on x64, both on every machine.
 *
 * One set for all, so the lock files hold the same thing whether the gate (Linux) or the MSIX job (Windows)
 * resolves them; each package carries the other platform's native too, a few megabytes it never loads.
 */
val Project.desktopSkia: List<String>
    get() {
        val version = catalog.version("composeMultiplatform")
        return listOf("linux-x64", "windows-x64").map { "org.jetbrains.compose.desktop:desktop-jvm-$it:$version" }
    }
