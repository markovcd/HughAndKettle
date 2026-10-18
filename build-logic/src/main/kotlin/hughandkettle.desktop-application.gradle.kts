// The desktop app: a JVM program over the shared Compose screens, started with `./gradlew :desktop:run`.
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension

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

// Skia's natives for the runnable folder, so the one folder starts on Linux and on Windows alike.
val folderSkia: Configuration by configurations.creating

dependencies {
    // Skia's native library for the machine building it; packaging per platform is a later step.
    implementation(desktopSkia)
    val compose = catalog.version("composeMultiplatform")
    folderSkia("org.jetbrains.compose.desktop:desktop-jvm-linux-x64:$compose")
    folderSkia("org.jetbrains.compose.desktop:desktop-jvm-windows-x64:$compose")
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
// Built with every build, so the gate's artifacts carry it; installers with their own runtime are the release's.
val folderLib =
    tasks.register<Sync>("folderLib") {
        into(layout.buildDirectory.dir("folder-lib"))
        from(tasks.named("jar"))
        // androidx and JetBrains both publish a runtime-desktop-1.x.jar, so each library's jar carries its group.
        val named =
            configurations
                .named("runtimeClasspath")
                .flatMap { it.incoming.artifacts.resolvedArtifacts }
                .zip(folderSkia.incoming.artifacts.resolvedArtifacts) { a, b -> a + b }
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

// The module's build file names the main class after this plugin applies.
afterEvaluate {
    val main = the<ComposeExtension>().extensions.getByType<DesktopExtension>().application.mainClass
    folderScripts.configure { mainClass.set(main) }
}
