plugins {
    id("hughandkettle.kotlin-library")
    application
}

application {
    applicationName = "hk"
    mainClass.set("io.github.markovcd.hughandkettle.cli.MainKt")
}

dependencies {
    implementation(project(":amp"))
    implementation(project(":wmi"))
}
