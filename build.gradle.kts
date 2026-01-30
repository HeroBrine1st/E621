// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false

    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.symbolProcessing) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false

    alias(libs.plugins.aboutlibraries.android) apply false
}

buildscript {
    dependencies {
//        classpath("com.google.dagger:hilt-android-gradle-plugin:2.50")
    }
    repositories {
        google()
    }
}


tasks {
    register<Delete>("clean") {
        delete(layout.buildDirectory)
    }
}