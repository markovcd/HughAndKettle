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

    // Robolectric renders Compose with the app's real resources.
    testOptions.unitTests.isIncludeAndroidResources = true
}

// Robolectric's Android framework jar, resolved and locked by Gradle, handed to it offline.
val robolectricAndroidAll: Configuration by configurations.creating
val robolectricJars = layout.buildDirectory.dir("robolectric-jars")
val syncRobolectricJars by tasks.registering(Sync::class) {
    from(robolectricAndroidAll)
    into(robolectricJars)
}
tasks.withType<Test>().configureEach {
    dependsOn(syncRobolectricJars)
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.dir", robolectricJars.get().asFile.absolutePath)
    // The newer framework's shared memory reaches a file descriptor's internals, which JDK 21 hides.
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED")
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
    implementation(libs.datastore.preferences)
    debugImplementation(libs.compose.ui.tooling)
    // The empty activity Compose tests host their content in.
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.espresso.core)
    robolectricAndroidAll(libs.robolectric.android.all)
}
