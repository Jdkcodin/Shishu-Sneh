/*
 * build.gradle.kts (App Module)
 *
 * This is where we configure:
 * - App package name, SDK versions, version code
 * - All library dependencies (Room, WorkManager, MPAndroidChart, etc.)
 * - Build features like ViewBinding
 */

plugins {
    // Apply the plugins declared in the project-level build.gradle.kts
    id("com.android.application")       // This is an Android app (not a library)
    id("org.jetbrains.kotlin.android")  // We're using Kotlin
    id("com.google.devtools.ksp")       // KSP for Room's annotation processing
}

android {
    // Unique package name — identifies your app on Google Play
    namespace = "com.mindmatrix.shishusneh"

    // compileSdk = which Android API level to compile against
    // Use the latest stable SDK for access to newest APIs
    compileSdk = 35

    defaultConfig {
        // applicationId = unique ID on Google Play Store
        applicationId = "com.mindmatrix.shishusneh"

        // minSdk = oldest Android version your app supports
        // API 24 = Android 7.0 (covers ~97% of devices)
        minSdk = 24

        // targetSdk = the API level you've tested against
        targetSdk = 35

        // versionCode = integer version (increment for each Play Store release)
        versionCode = 1

        // versionName = human-readable version shown to users
        versionName = "1.0"

        // Test runner for unit tests
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // ProGuard shrinks and obfuscates code in release builds
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Use Java 17 — required by latest Android Gradle Plugin
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // ViewBinding generates a binding class for each XML layout
    // So instead of: findViewById(R.id.myButton)
    // We can write:   binding.myButton
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ============================================================
    // CORE ANDROID LIBRARIES
    // ============================================================

    // Kotlin extensions for Android (provides kotlinx.coroutines support, etc.)
    implementation("androidx.core:core-ktx:1.13.1")

    // Android 12+ Splash Screen (Phase 4)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // AppCompat — backward-compatible versions of Android UI components
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Material Design 3 — Google's modern UI component library
    // Provides: MaterialButton, TextInputLayout, DatePicker, Cards, etc.
    implementation("com.google.android.material:material:1.12.0")

    // ConstraintLayout — flexible layout for complex UIs
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Activity KTX — provides the 'by viewModels()' delegate
    implementation("androidx.activity:activity-ktx:1.9.3")

    // ============================================================
    // ROOM DATABASE (Local storage for baby profiles)
    // ============================================================

    val roomVersion = "2.6.1"

    // Room runtime — the core library
    implementation("androidx.room:room-runtime:$roomVersion")

    // Room KTX — adds Kotlin coroutines support (suspend functions in DAOs)
    implementation("androidx.room:room-ktx:$roomVersion")

    // Room compiler — generates database code from annotations (@Entity, @Dao)
    // Uses KSP instead of kapt for faster builds
    ksp("androidx.room:room-compiler:$roomVersion")

    // ============================================================
    // LIFECYCLE (ViewModel + LiveData)
    // ============================================================

    val lifecycleVersion = "2.8.7"

    // ViewModel — survives screen rotation, manages UI-related data
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")

    // LiveData — observable data holder, auto-updates UI when data changes
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    // Lifecycle runtime — coroutine support for lifecycle-aware components
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    // ============================================================
    // WORKMANAGER (Vaccination reminders — Phase 3)
    // ============================================================

    // WorkManager — reliable background task scheduling
    // Even if user closes the app, reminders will still fire!
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // ============================================================
    // MPANDROIDCHART (Growth visualization — Phase 2)
    // ============================================================

    // Beautiful charts library for the 6-month growth trend line
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ============================================================
    // TESTING
    // ============================================================

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
