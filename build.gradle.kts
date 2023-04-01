// Top-level build file where you can add configuration options common to all sub-projects/modules.

@Suppress("SpellCheckingInspection")
plugins {
    id("com.android.application") version "7.4.2" apply false
    id("com.android.library") version "7.4.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("com.google.devtools.ksp") version "1.8.10-1.0.9" apply false
    id("com.google.protobuf") version "0.9.1" apply false // 0.9.2 is broken
    id("com.mikepenz.aboutlibraries.plugin") version "10.6.1" apply false
}

buildscript {
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.45")
    }
    repositories {
        google()
    }
}


tasks {
    create<Delete>("clean") {
        delete(buildDir)
    }
}