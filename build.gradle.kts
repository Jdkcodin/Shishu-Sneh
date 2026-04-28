/*
 * build.gradle.kts (Project-level)
 *
 * This is the TOP-LEVEL build file.
 * It declares which Gradle plugins are available to ALL modules.
 *
 * "apply false" means: "Make this plugin available, but don't apply it here."
 * Each module (like :app) will apply the plugins it needs individually.
 */

plugins {
    // Android Application plugin — needed to build Android apps
    id("com.android.application") version "8.7.3" apply false

    // Kotlin for Android — adds Kotlin language support
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false

    // KSP (Kotlin Symbol Processing) — used by Room to generate code
    // NOTE: KSP version MUST match your Kotlin version (2.1.0)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}
