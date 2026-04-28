/*
 * settings.gradle.kts — Project Settings
 *
 * This file tells Gradle:
 * 1. WHERE to find plugins (pluginManagement)
 * 2. WHERE to find libraries (dependencyResolutionManagement)
 * 3. WHAT modules make up this project (include)
 */

pluginManagement {
    repositories {
        google()            // Google's Maven repo (Android, Jetpack libraries)
        mavenCentral()      // Central repo for most Java/Kotlin libraries
        gradlePluginPortal() // Gradle's own plugin repository
    }
}

dependencyResolutionManagement {
    // FAIL_ON_PROJECT_REPOS = Don't allow individual modules to define their own repos
    // This keeps all repo definitions in ONE place (here!)
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack is needed for MPAndroidChart (it's hosted on GitHub)
        maven(url = "https://jitpack.io")
    }
}

// Project name — shows up in Android Studio's title bar
rootProject.name = "Shishu-Sneh"

// Include the 'app' module — this is where all our code lives
include(":app")
