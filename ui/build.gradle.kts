plugins {
    id("hughandkettle.compose-library")
}

kotlin {
    extensions.configure<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>("android") {
        namespace = "io.github.markovcd.hughandkettle.ui"
    }
}

compose.resources {
    // The Android app reads its text through it until its screens move here.
    publicResClass = true
    packageOfResClass = "io.github.markovcd.hughandkettle.ui"
}
