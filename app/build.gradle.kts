@file:Suppress("SpellCheckingInspection", "UnstableApiUsage")

import com.android.build.api.dsl.VariantDimension
import java.io.ByteArrayOutputStream
import java.util.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.protobuf")
    id("com.mikepenz.aboutlibraries.plugin")
}

val kotlinVersion = "1.8.20"
val composeCompilerVersion = "1.4.6"
val protobufVersion = "3.22.2"
val okHttpVersion = "4.10.0"
val retrofitVersion = "2.9.0"

val applicationId = "ru.herobrine1st.e621"
val versionCode = getCommitIndexNumber()
val versionName = "1.0.0-alpha-4"

android {
    compileSdk = 33

    defaultConfig {
        applicationId = this@Build_gradle.applicationId
        minSdk = 27
        targetSdk = 33
        versionCode = this@Build_gradle.versionCode
        versionName = this@Build_gradle.versionName

        testInstrumentationRunner = "ru.herobrine1st.e621.runner.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Application properties
        resValue("string", "deep_link_host", "e621.net")
        buildConfigField("int", "PAGER_PAGE_SIZE", "500")
        // Implicitly converts spaces to underscores in search
        buildConfigField("boolean", "HIDE_UNDERSCORES_FROM_USER", "true")
        // Will not work if underscores are hidden (the setting above)
        buildConfigField("boolean", "CONVERT_SPACES_TO_UNDERSCORES_IN_SEARCH", "true")
        stringBuildConfigField("DATABASE_NAME", "DATABASE")
        stringBuildConfigField("API_BASE_URL", "https://e621.net")
        stringBuildConfigField("DEEP_LINK_BASE_URL", "https://e621.net")
        stringBuildConfigField(
            "USER_AGENT_TEMPLATE",
            "${applicationId}/${versionName} (Android/%s; %s build; +https://github.com/HeroBrine1st/E621) " +
                    // That's zero. Workaround for their shitty protection (when on earth user-agent could be security header???)
                    // Btw I think it is a cloudflare rule (did a source code review and didn't found that protection in ApplicationController)
                    "0kHttp/$okHttpVersion Retrofit/$retrofitVersion"
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
                "intellij-idea-does-not-like-these-proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".test"
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeCompilerVersion
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    lint {
        fatal += "StopShip"
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
    implementation("androidx.core:core-ktx:1.10.0") // Apache 2.0
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1") // Apache 2.0

    // Jetpack Compose (Apache 2.0)
    implementation("androidx.compose.ui:ui:1.4.2")
    // PullRefresh is not implemented in m3
    // also many libraries use colors from m2
    implementation("androidx.compose.material:material:1.4.2")
    implementation("androidx.compose.material3:material3:1.1.0-rc01")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.2")
    implementation("androidx.compose.material:material-icons-extended:1.4.2")
    implementation("androidx.activity:activity-compose:1.7.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // Decompose
    val decomposeVersion = "2.0.0-alpha-01"
    implementation("com.arkivanov.decompose:decompose:$decomposeVersion") // Apache 2.0
    implementation("com.arkivanov.decompose:extensions-compose-jetpack:$decomposeVersion")

    // Jetpack Room
    val roomVersion = "2.5.1"
    implementation("androidx.room:room-runtime:$roomVersion") // Apache 2.0
    implementation("androidx.room:room-ktx:$roomVersion") // Apache 2.0
    ksp("androidx.room:room-compiler:$roomVersion") // Not included in binary result

    // Jetpack Datastore
    implementation("androidx.datastore:datastore:1.0.0") // Apache 2.0
    implementation("com.google.protobuf:protobuf-javalite:$protobufVersion") // BSD 3-clause

    // Jetpack Paging
    val pagingVersion = "3.1.1"
    implementation("androidx.paging:paging-runtime:$pagingVersion") // Apache 2.0
    implementation("androidx.paging:paging-compose:1.0.0-alpha18") // Apache 2.0

    // Coroutine Image Loader (Apache 2.0)
    val coilVersion = "2.3.0"
    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-compose:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")

    // Jackson (Apache 2.0)
    val jacksonVersion = "2.14.2"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Hilt (Apache 2.0)
    val hiltVersion = "2.45"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion") // Not included in binary result


    // G Accompanist (Apache 2.0)
    val accompanistVersion = "0.30.0"
    implementation("com.google.accompanist:accompanist-placeholder-material:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")

    // Retrofit (Apache 2.0)
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-jackson:$retrofitVersion")

    // Other libraries
    implementation("com.google.android.exoplayer:exoplayer:2.18.6") // Apache 2.0
    implementation("org.jsoup:jsoup:1.15.4") // Expat License
    implementation("com.mikepenz:aboutlibraries-compose:10.6.2") // Apache 2.0

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.9.2")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.4.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:$hiltVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:1.4.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.4.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                // TODO protoc plugin 'java' not defined. Trying to use 'protoc-gen-java' from system path
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

// KAPT and KSP use gradle JDK version for that
// KAPT requires 11, while android is still using 1.8
// Use narrow class names to minimize impact on other tasks
tasks.withType<com.google.devtools.ksp.gradle.KspTaskJvm>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

fun getCommitIndexNumber(revision: String = "HEAD"): Int {
    val byteArrayOutputStream = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "rev-list", "--count", "--first-parent", revision)
        standardOutput = byteArrayOutputStream
        isIgnoreExitValue = false
    }
    return byteArrayOutputStream.toString().trim().toInt()
}

fun VariantDimension.stringBuildConfigField(name: String, value: String) =
    buildConfigField("String", name, "\"${value.replace("\"", "\\\"")}\"")
