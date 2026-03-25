import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// Load local.properties for custom config (api.base.url, etc.)
val localPropsFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { stream -> localProperties.load(stream) }
}
val apiBaseUrl: String = localProperties.getProperty("api.base.url", "http://10.0.2.2:8080/")

android {
    namespace = "com.wattson"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wattson"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // API base URL from local.properties - defaults to emulator localhost
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        // Google Web Client ID for Credential Manager
        val googleWebClientId: String = localProperties.getProperty("google.web.client.id", "")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Image Loading
    implementation(libs.coil.compose)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // ML Kit Barcode Scanning
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.tesseract4android)

    // DataStore
    implementation(libs.datastore.preferences)

    // Security (Encrypted SharedPreferences)
    implementation(libs.security.crypto)

    // Google Sign-In (Credential Manager)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.google.id)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Stripe PaymentSheet
    implementation(libs.stripe.android)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization (for type-safe navigation)
    implementation(libs.kotlinx.serialization.json)

    // Unit tests
    testImplementation(libs.junit4)
}
