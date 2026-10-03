buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        // AGP 9.x has built-in Kotlin. Pin the newer KGP used by the Compose compiler.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
}
