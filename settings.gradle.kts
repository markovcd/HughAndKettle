// Every module shares one dependency catalog (gradle/libs.versions.toml) and
// one set of repositories; a module declaring its own repository fails the build.
pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "hughandkettle"

include(":midi", ":amp", ":wmi", ":cli", ":app")
