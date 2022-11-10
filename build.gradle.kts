// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "7.3.1" apply false
    id("com.android.library") version "7.3.1" apply false
    id("org.jetbrains.kotlin.android") version "1.7.0" apply false
    id("com.google.devtools.ksp") version "1.7.0-1.0.6" apply false
    id("com.google.protobuf") version "0.8.17" apply false
}

buildscript {
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.44")
    }
}


tasks {
    create<Delete>("clean") {
        delete(buildDir)
    }
}