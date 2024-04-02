// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.3.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false
    id("com.google.protobuf") version "0.9.4" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "11.1.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23" apply false
    id("androidx.room") version "2.6.1" apply false
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
    create<Delete>("clean") {
        delete(layout.buildDirectory)
    }
}