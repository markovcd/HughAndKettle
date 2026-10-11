// A pure-Kotlin module: no Android, testable on the JVM in milliseconds.
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("hughandkettle.ktlint")
}

kotlin {
    jvmToolchain(catalog.version("jdk").toInt())
    // Every declaration states its visibility; the default is internal, by rule.
    explicitApi()
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
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
    // One report per module, with a duration per test, for the tests skill.
    reports.junitXml.required.set(true)
}

dependencyLocking {
    lockAllConfigurations()
}
