// Compose screens shared by the Android app and the desktop app: Kotlin Multiplatform with an
// Android target (AGP's KMP library plugin) and a desktop JVM target, drawn by Compose Multiplatform.
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    // Android Lint, which the KMP library plugin leaves out; `lint` runs as part of `check`.
    id("com.android.lint")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("hughandkettle.ktlint")
}

kotlin {
    jvmToolchain(catalog.version("jdk").toInt())
    // Every declaration states its visibility; the default is internal, by rule.
    explicitApi()
    compilerOptions {
        allWarningsAsErrors.set(true)
    }

    extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
        compileSdk = catalog.version("compileSdk").toInt()
        minSdk = catalog.version("minSdk").toInt()
        androidResources.enable = true
        lint {
            warningsAsErrors = true
            abortOnError = true
            // Dependabot owns upgrades; a build must not go red because a newer
            // version was published since the last commit.
            disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            // The shells draw with the same Compose the shared screens are made of.
            api(catalog.findLibrary("compose-mp-runtime").get())
            api(catalog.findLibrary("compose-mp-foundation").get())
            api(catalog.findLibrary("compose-mp-ui").get())
            api(catalog.findLibrary("compose-mp-material3").get())
            api(catalog.findLibrary("compose-mp-resources").get())
        }
        getByName("desktopTest").dependencies {
            implementation(kotlin("test"))
            implementation(catalog.findLibrary("junit-jupiter").get())
            runtimeOnly(catalog.findLibrary("junit-platform-launcher").get())
            implementation(catalog.findLibrary("compose-mp-ui-test").get())
            // Skia's native library for the machine running the tests.
            implementation(desktopSkia)
        }
    }
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
