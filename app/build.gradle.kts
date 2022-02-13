plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val composeVersion = "1.0.5"
val kotlinVersion = "1.5.31"
val roomVersion = "2.4.1"
val pagingVersion = "3.1.0"

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "ru.herobrine1st.e621"
        minSdk = 27
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

        }
        forEach {
            it.buildConfigField("String", "DATABASE_NAME", "\"DATABASE\"")
            it.buildConfigField("String", "API_URL", "\"e621.net\"")
            it.buildConfigField("String", "USER_AGENT", "\"Android App/${android.defaultConfig.versionName} (${properties["E621_USERNAME"]})\"") // set in ~/.gradle/gradle.properties
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    }
}
@Suppress("SpellCheckingInspection")
dependencies {
    // Jetpack Compose
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    implementation("androidx.activity:activity-compose:1.4.0")

    // Jetpack Navigation
    implementation("androidx.navigation:navigation-compose:2.4.1")

    // Jetpack Room
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:2.4.1")

    // Jetpack Datastore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Jetpack Paging
    implementation("androidx.paging:paging-runtime:$pagingVersion")
    implementation("androidx.paging:paging-compose:1.0.0-alpha14")

    // Coroutine Image Loader
    implementation("io.coil-kt:coil:1.4.0")
    implementation("io.coil-kt:coil-compose:1.4.0")
    implementation("io.coil-kt:coil-gif:1.4.0")

    // Other libraries
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.google.accompanist:accompanist-flowlayout:0.20.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}