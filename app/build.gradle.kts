import com.android.build.api.dsl.VariantDimension
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)

    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.symbolProcessing)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.aboutlibraries)
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "ru.herobrine1st.e621"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 781
        versionName = "0.1.0"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Application properties
        resValue("string", "deep_link_host", "e621.net")
        // https://issuetracker.google.com/issues/380805025 120 is max
        buildConfigField("int", "PAGER_PAGE_SIZE", "100")
        buildConfigField("int", "PAGER_PREFETCH_DISTANCE", "25")
        buildConfigField("int", "PAGER_MAX_PAGES_IN_MEMORY", "3")
        // Replaces all underscores in tags with spaces
        buildConfigField("boolean", "HIDE_UNDERSCORES_FROM_USER", "true")
        stringBuildConfigField("DATABASE_NAME", "DATABASE")
        stringBuildConfigField("API_BASE_URL", "https://e621.net/")
        stringBuildConfigField("DEEP_LINK_BASE_URL", "https://e621.net")
        stringBuildConfigField(
            "USER_AGENT_TEMPLATE",
            // application id, version, android version, build type
            "%s/%s (Android/%s; %s build; +https://github.com/HeroBrine1st/E621) Ktor/${libs.versions.ktor.get()}"
        )
    }
    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")

            // Apk will be in app/build/outputs/apk/release/app-release.apk
            // or app-release-unsigned.apk, if unsigned
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "intellij-idea-does-not-like-these-proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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

        create("nightly") {
            initWith(getByName("release"))
            matchingFallbacks.add("release")
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-${getCommitShortHash()}"
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        @Suppress("SpellCheckingInspection")
        jniLibs {
            // There's no use of multiprocess datastore, so this is not needed
            excludes += "/lib/*/libdatastore_shared_counter.so"

            // Required on API33- and on API34 for conic transform
            // https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/graphics/graphics-path/src/main/java/androidx/graphics/path/PathIteratorImpl.kt#48
            // Commit c7092daf5b7199c928c351af99c1ab5179370062
            // excludes += "/lib/*/libandroidx.graphics.path.so"
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
        // some libraries depend on older stdlib
        force("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
    }
}

dependencies {

    implementation(libs.compose.ui.core)
    implementation(libs.compose.material.m3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.util)

    implementation(libs.decompose.core)
    implementation(libs.decompose.extensions.compose)

    implementation(libs.coil.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.okhttp)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore)
    implementation(libs.kotlinx.serialization.protobuf)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotination)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.resources)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)

    implementation(libs.jsoup)
    implementation(libs.aboutlibraries)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.ktx)
    implementation(libs.androidx.activity.compose)

    // Profiling
    "profileableImplementation"(libs.androidx.compose.tracing)
    "profileableImplementation"(libs.androidx.tracing.perfetto.core)
    "profileableImplementation"(libs.androidx.tracing.perfetto.binary)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.junit4)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.test.ktx)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.compose.ui.tooling.core)
    debugImplementation(libs.compose.ui.test.manifest)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

fun executeCommand(vararg argv: String): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    exec {
        commandLine = argv.toList()
        standardOutput = byteArrayOutputStream
        isIgnoreExitValue = false
    }
    return byteArrayOutputStream.toString()
}

fun getCommitShortHash(revision: String = "HEAD") =
    executeCommand("git", "rev-parse", "--short", revision).trim()

fun VariantDimension.stringBuildConfigField(name: String, value: String) =
    buildConfigField("String", name, "\"${value.replace("\"", "\\\"")}\"")
