plugins {
    id("hughandkettle.desktop-application")
}

compose.desktop.application {
    mainClass = "io.github.markovcd.hughandkettle.desktop.MainKt"
}

dependencies {
    implementation(project(":ui"))
    implementation(libs.kotlinx.coroutines.core)
}
