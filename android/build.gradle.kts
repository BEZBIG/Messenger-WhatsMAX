/**
 * build.gradle.kts (project-level)
 * Корневой Gradle-файл Android-проекта WhatsMAX.
 * Объявляет плагины для всего проекта (Hilt, Firebase, Kotlin).
 */
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
    // KSP вместо kapt — совместим с Gradle 8.x
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
