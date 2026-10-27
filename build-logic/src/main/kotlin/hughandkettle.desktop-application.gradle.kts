// The desktop app: a JVM program over the shared Compose screens, started with `./gradlew :desktop:run`.
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("hughandkettle.ktlint")
}

kotlin {
    jvmToolchain(catalog.version("jdk").toInt())
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    desktopSkia.forEach { implementation(it) }
    testImplementation(kotlin("test"))
    testImplementation(catalog.findLibrary("compose-mp-ui-test").get())
    testImplementation(catalog.findLibrary("junit-jupiter").get())
    testRuntimeOnly(catalog.findLibrary("junit-platform-launcher").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED)
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    reports.junitXml.required.set(true)
}

dependencyLocking {
    lockAllConfigurations()
}

// A folder that runs on any machine with Java 21, the way `hk`'s does: lib/ and the start scripts in bin/.
// Built with every build, so the gate's artifacts carry it. The packages below carry a runtime of their own.
val folderLib =
    tasks.register<Sync>("folderLib") {
        into(layout.buildDirectory.dir("folder-lib"))
        from(tasks.named("jar"))
        // androidx and JetBrains both publish a runtime-desktop-1.x.jar, so each library's jar carries its group.
        val named =
            configurations
                .named("runtimeClasspath")
                .flatMap { it.incoming.artifacts.resolvedArtifacts }
                .map { all ->
                    all.associate {
                        val module = it.id.componentIdentifier as? ModuleComponentIdentifier
                        it.file to (module?.let { m -> "${m.group}-${it.file.name}" } ?: it.file.name)
                    }
                }
        from(named.map { it.keys }) {
            eachFile { name = named.get().getValue(file) }
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

val folderScripts =
    tasks.register<CreateStartScripts>("folderScripts") {
        applicationName = "hughandkettle"
        outputDir = layout.buildDirectory.dir("folder-scripts").get().asFile
        classpath = files(folderLib.map { fileTree(it.destinationDir) })
    }

val installDist =
    tasks.register<Sync>("installDist") {
        into(layout.buildDirectory.dir("install/hughandkettle"))
        into("lib") { from(folderLib) }
        into("bin") { from(folderScripts) }
    }

tasks.named("assemble") { dependsOn(installDist) }

// The packages, each with its own Java runtime: a .deb and a portable folder (`createDistributable`) on Linux,
// on Windows the folder the MSIX is packed from, and on macOS an app bundle and a .dmg. The version is the
// release's, or 0.0.0 for any other build.
val desktop = the<ComposeExtension>().extensions.getByType<DesktopExtension>()
desktop.application.nativeDistributions {
    targetFormats(TargetFormat.Deb, TargetFormat.Dmg)
    packageName = "hughandkettle"
    packageVersion = buildVersion().name.substringBefore('-')
    description = "Remote for Hughes & Kettner amps over a WMI-1 or a USB MIDI cable"
    vendor = "Hugh & Kettle"
    licenseFile.set(rootProject.file("LICENSE"))
    // What the app reaches beyond java.base: AWT and Java's sound and MIDI, preferences, and Skia's unsafe access.
    modules("java.desktop", "java.prefs", "jdk.unsupported")
    linux {
        iconFile.set(project.file("src/main/resources/icon.png"))
        appCategory = "sound"
        menuGroup = "AudioVideo;Audio"
        shortcut = true
    }
    windows {
        iconFile.set(project.file("packaging/icon.ico"))
    }
    macOS {
        bundleID = "io.github.markovcd.hughandkettle"
        // macOS refuses a version whose first number is 0.
        packageVersion = buildVersion().name.substringBefore('-').takeUnless { it.startsWith("0.") } ?: "1.0.0"
        // macOS opens the microphone, for the loudness match, only to an app that says why it wants it.
        infoPlist {
            extraKeysRawXml =
                """
                <key>NSMicrophoneUsageDescription</key>
                <string>The loudness match listens to each preset to bring them to one loudness.</string>
                """.trimIndent()
        }
    }
}

// The portable Linux folder as one archive, which keeps the launcher's execute bit that a zip of the folder loses.
val portableLinux =
    tasks.register<Tar>("portableLinux") {
        from(tasks.named("createDistributable")) {
            // Gradle writes the files without their execute bit; the launcher and the runtime's tools need it.
            eachFile { if (file.canExecute()) permissions { unix("rwxr-xr-x") } }
        }
        archiveFileName.set("hughandkettle-linux-x64.tar.gz")
        destinationDirectory.set(layout.buildDirectory.dir("packages"))
        compression = Compression.GZIP
    }

// The module's build file names the main class after this plugin applies.
afterEvaluate {
    val main = desktop.application.mainClass
    folderScripts.configure { mainClass.set(main) }
}
