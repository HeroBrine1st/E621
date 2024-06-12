import com.android.build.api.dsl.VariantDimension
import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.protobuf")
    id("com.mikepenz.aboutlibraries.plugin")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

val kotlinVersion = "1.9.21"
val composeCompilerVersion = "1.5.14"
val protobufVersion = "4.27.0"

val ktorVersion = "2.3.11"

val applicationId = "ru.herobrine1st.e621"
val versionCode = getCommitIndexNumber()
val versionName = "1.0.0-alpha-5"

android {
    compileSdk = 34

    defaultConfig {
        applicationId = this@Build_gradle.applicationId
        minSdk = 27
        targetSdk = 34
        versionCode = this@Build_gradle.versionCode
        versionName = this@Build_gradle.versionName

        vectorDrawables {
            useSupportLibrary = true
        }

        // Application properties
        resValue("string", "deep_link_host", "e621.net")
        buildConfigField("int", "PAGER_PAGE_SIZE", "500")
        buildConfigField("int", "PAGER_PREFETCH_DISTANCE", "50")
        // Replaces all underscores in tags with spaces
        buildConfigField("boolean", "HIDE_UNDERSCORES_FROM_USER", "true")
        stringBuildConfigField("DATABASE_NAME", "DATABASE")
        stringBuildConfigField("API_BASE_URL", "https://e621.net/")
        stringBuildConfigField("DEEP_LINK_BASE_URL", "https://e621.net")
        stringBuildConfigField(
            "USER_AGENT_TEMPLATE",
            "${applicationId}/${versionName} (Android/%s; %s build; +https://github.com/HeroBrine1st/E621) Ktor/$ktorVersion"
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
    @Suppress("UnstableApiUsage")
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
    implementation("androidx.core:core-ktx:1.13.1") // Apache 2.0
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0") // Apache 2.0

    // Jetpack Compose (Apache 2.0)
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.material3:material3:1.3.0-beta02")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("androidx.activity:activity-compose:1.9.0")
//    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.ui:ui-util:1.6.7")

    // Decompose
    val decomposeVersion = "2.2.2"
    implementation("com.arkivanov.decompose:decompose:$decomposeVersion") // Apache 2.0
    implementation("com.arkivanov.decompose:extensions-compose-jetpack:$decomposeVersion")

    // Jetpack Room
    val roomVersion = "2.7.0-alpha03"
    implementation("androidx.room:room-runtime:$roomVersion") // Apache 2.0
    implementation("androidx.room:room-ktx:$roomVersion") // Apache 2.0
    ksp("androidx.room:room-compiler:$roomVersion") // Not included in binary result

    // Jetpack Datastore
    implementation("androidx.datastore:datastore:1.1.1") // Apache 2.0
    implementation("com.google.protobuf:protobuf-javalite:$protobufVersion") // BSD 3-clause

    // Coroutine Image Loader (Apache 2.0)
    val coilVersion = "2.6.0"
    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-compose:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // KTor
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-resources:$ktorVersion")

    // Profiling
    "profileableImplementation"("androidx.compose.runtime:runtime-tracing:1.0.0-beta01")
    "profileableImplementation"("androidx.tracing:tracing-perfetto:1.0.0")
    "profileableImplementation"("androidx.tracing:tracing-perfetto-binary:1.0.0")

    // Jetpack Media3
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.3.1")

    // Other libraries
    implementation("org.jsoup:jsoup:1.17.2") // Expat License
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.2.0") // Apache 2.0
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.6.7")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.7")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
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
