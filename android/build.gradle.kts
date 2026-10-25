// Root build file for Kaiju Coach.
//
// AGP 9 compiles Kotlin itself ("built-in Kotlin"), so the org.jetbrains.kotlin.android
// plugin is not applied anywhere. AGP only depends on an older Kotlin Gradle Plugin at
// runtime, so the KGP version is pinned here to match the Compose compiler and
// serialization plugins below.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.roborazzi) apply false
}
