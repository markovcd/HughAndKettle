plugins {
    id("hughandkettle.compose-library")
}

kotlin {
    extensions.configure<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>("android") {
        namespace = "io.github.markovcd.hughandkettle.ui"
    }
}

compose.resources {
    packageOfResClass = "io.github.markovcd.hughandkettle.ui"
}
