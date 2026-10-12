plugins {
    id("hughandkettle.kotlin-library")
}

dependencies {
    api(project(":midi"))
    implementation(libs.kotlinx.serialization.json)
}
