@file:Suppress("SpellCheckingInspection", "UnstableApiUsage")

import com.android.build.api.dsl.VariantDimension
import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.protobuf")
    id("com.mikepenz.aboutlibraries.plugin")
    kotlin("plugin.serialization")
}

val kotlinVersion = "1.9.21"
val composeCompilerVersion = "1.5.7"
val protobufVersion = "3.25.1"
val okHttpVersion = "4.12.0"
val retrofitVersion =
    "2.9.0" // https://github.com/square/retrofit/issues/3880 , do not forget to remove rules after update

val applicationId = "ru.herobrine1st.e621"
val versionCode = getCommitIndexNumber()
val versionName = "1.0.0-alpha-4"

android {
    compileSdk = 34

    defaultConfig {
        applicationId = this@Build_gradle.applicationId
        minSdk = 27
        targetSdk = 34
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
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "intellij-idea-does-not-like-these-proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
        // Also good for testing proguard configs
        create("profileable") {
            initWith(getByName("release"))
            matchingFallbacks.add("release")

            proguardFiles("profileable-rules.pro")
            applicationIdSuffix = ".profileable"
            signingConfig = signingConfigs.getByName("debug")
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
    implementation("androidx.core:core-ktx:1.12.0") // Apache 2.0
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") // Apache 2.0

    // Jetpack Compose (Apache 2.0)
    implementation("androidx.compose.ui:ui:1.5.4")
    // PullRefresh is not implemented in m3
    // also many libraries use colors from m2
    implementation("androidx.compose.material:material:1.5.4")
    implementation("androidx.compose.material3:material3:1.2.0-beta01")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.compose.ui:ui-util:1.5.4")

    // Decompose
    val decomposeVersion = "2.2.2"
    implementation("com.arkivanov.decompose:decompose:$decomposeVersion") // Apache 2.0
    implementation("com.arkivanov.decompose:extensions-compose-jetpack:$decomposeVersion")

    // Jetpack Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion") // Apache 2.0
    implementation("androidx.room:room-ktx:$roomVersion") // Apache 2.0
    ksp("androidx.room:room-compiler:$roomVersion") // Not included in binary result

    // Jetpack Datastore
    implementation("androidx.datastore:datastore:1.0.0") // Apache 2.0
    implementation("com.google.protobuf:protobuf-javalite:$protobufVersion") // BSD 3-clause

    // Jetpack Paging
    implementation("androidx.paging:paging-runtime:3.2.1") // Apache 2.0
    implementation("androidx.paging:paging-compose:3.2.1") // Apache 2.0

    // Coroutine Image Loader (Apache 2.0)
    val coilVersion = "2.5.0"
    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-compose:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")

    // Jackson (Apache 2.0)
    val jacksonVersion = "2.16.1"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Hilt (Apache 2.0)
    val hiltVersion = "2.50"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion") // Not included in binary result

    // G Accompanist (Apache 2.0)
    val accompanistVersion = "0.32.0"
    implementation("com.google.accompanist:accompanist-placeholder-material3:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")

    // Retrofit (Apache 2.0)
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-jackson:$retrofitVersion")

    // Profiling
    "profileableImplementation"("androidx.compose.runtime:runtime-tracing:1.0.0-beta01")
    "profileableImplementation"("androidx.tracing:tracing-perfetto:1.0.0")
    "profileableImplementation"("androidx.tracing:tracing-perfetto-binary:1.0.0")

    // Jetpack Media3
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.2.0")
//    implementation("androidx.media3:media3-session:1.0.1")

    // Other libraries
    implementation("org.jsoup:jsoup:1.17.2") // Expat License
    implementation("com.mikepenz:aboutlibraries-compose:10.9.2") // Apache 2.0
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:$hiltVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
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
