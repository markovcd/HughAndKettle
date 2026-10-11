// The Android app: Compose UI over the JVM modules. AGP's built-in Kotlin
// compiles it; the Kotlin Android plugin is not applied.
import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("hughandkettle.ktlint")
}

val version = buildVersion()

configure<ApplicationExtension> {
    compileSdk = catalog.version("compileSdk").toInt()

    defaultConfig {
        minSdk = catalog.version("minSdk").toInt()
        targetSdk = catalog.version("targetSdk").toInt()
        versionName = version.name
        versionCode = version.code
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        // Dependabot owns upgrades; a build must not go red because a newer
        // version was published since the last commit.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

kotlin {
    jvmToolchain(catalog.version("jdk").toInt())
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

// Android unit tests run on JUnit 4, so kotlin-test's JUnit 4 flavor is named rather than chosen.
dependencies {
    "testImplementation"(kotlin("test-junit"))
    "testImplementation"(catalog.findLibrary("junit4").get())
}

dependencyLocking {
    lockAllConfigurations()
}
