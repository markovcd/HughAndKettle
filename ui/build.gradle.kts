plugins {
    id("hughandkettle.compose-library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":amp"))
        api(project(":wmi"))
        api(libs.datastore.preferences.core)
        implementation(libs.kotlinx.coroutines.core)
    }
    sourceSets.androidMain.dependencies {
        // The document picker and permission asks behind the Android side of the platform seams.
        implementation(libs.activity.compose)
    }
    extensions.configure<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>("android") {
        namespace = "io.github.markovcd.hughandkettle.ui"
    }
}

compose.resources {
    // The Android app reads its text through it until its screens move here.
    publicResClass = true
    packageOfResClass = "io.github.markovcd.hughandkettle.ui"
}
