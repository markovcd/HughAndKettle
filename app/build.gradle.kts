plugins {
    id("hughandkettle.android-application")
}

android {
    namespace = "io.github.markovcd.hughandkettle"

    defaultConfig {
        // Persisted the moment a build is installed anywhere; never renamed.
        applicationId = "io.github.markovcd.hughandkettle"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":amp"))
    implementation(project(":wmi"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)
}
