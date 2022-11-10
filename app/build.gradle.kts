@file:Suppress("SpellCheckingInspection")

import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.protobuf")
}

val kotlinVersion = "1.7.0"
val composeCompilerVersion = "1.2.0"
val protobufVersion = "3.21.2"

val versionCode = 5
val versionName = "1.0.0-alpha-4"

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "ru.herobrine1st.e621"
        minSdk = 27
        targetSdk = 33
        versionCode = this@Build_gradle.versionCode
        versionName = this@Build_gradle.versionName

        testInstrumentationRunner = "ru.herobrine1st.e621.runner.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".test"
        }
        forEach {
            it.buildConfigField("String", "DATABASE_NAME", "\"DATABASE\"")
            it.buildConfigField("String", "API_BASE_URL", "\"https://e621.net\"")
            it.buildConfigField("String", "DEEP_LINK_BASE_URL", "\"https://e621.net\"")
            it.resValue("string", "deep_link_host", "e621.net")
            it.buildConfigField(
                "String",
                "USER_AGENT",
                "\"Android App/${versionName}\""
            )
            it.buildConfigField("int", "PAGER_PAGE_SIZE", "500")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf(
            "-opt-in=kotlin.RequiresOptIn",
//            "-Xjvm-default=all-compatibility"
        )
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeCompilerVersion
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    namespace = "ru.herobrine1st.e621"
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    }
}
@Suppress("SpellCheckingInspection")
dependencies {
    // Android core
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.3.1")
    implementation("androidx.compose.material:material:1.3.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.3.1")
    implementation("androidx.activity:activity-compose:1.6.1")

    // Jetpack Navigation
    implementation("androidx.navigation:navigation-compose:2.5.3")

    // Jetpack Room
    val roomVersion = "2.4.3"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Jetpack Datastore
    implementation("androidx.datastore:datastore:1.0.0")
    implementation("com.google.protobuf:protobuf-javalite:$protobufVersion")

    // Jetpack Paging
    val pagingVersion = "3.1.1"
    implementation("androidx.paging:paging-runtime:$pagingVersion")
    implementation("androidx.paging:paging-compose:1.0.0-alpha17")

    // Coroutine Image Loader
    val coilVersion = "1.4.0"
    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-compose:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")

    // Jackson
    val jacksonVersion = "2.14.0"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Hilt
    val hiltVersion = "2.44"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")


    // G Accompanist
    val accompanistVersion = "0.27.0"
    implementation("com.google.accompanist:accompanist-flowlayout:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-placeholder-material:$accompanistVersion")

    // Retrofit
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.9.0")

    // Other libraries
    implementation("com.google.android.exoplayer:exoplayer:2.18.1")
    implementation("org.jsoup:jsoup:1.14.3")


    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.8.1")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.3.1")
    testImplementation("org.mockito:mockito-core:4.6.1")
    testImplementation("org.mockito:mockito-inline:4.6.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:$hiltVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:1.3.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.3.1")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kapt {
    correctErrorTypes = true
}
