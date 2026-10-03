pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TapScript"
include(
    ":app",
    ":engine-api",
    ":engine-core",
    ":platform-android",
    ":recognition-mlkit",
    ":scripting-rhino",
    ":storage-json",
)
