// The desktop app: a JVM program over the shared Compose screens, started with `./gradlew :desktop:run`.
import org.gradle.api.tasks.testing.logging.TestLogEvent

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
    // Skia's native library for the machine building it; packaging per platform is a later step.
    implementation(desktopSkia)
    testImplementation(kotlin("test"))
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
