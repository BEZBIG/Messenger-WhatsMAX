/**
 * settings.gradle.kts (android)
 * Настройки Gradle для Android-проекта WhatsMAX.
 * Подключает репозитории Google и Maven Central.
 */
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WhatsMAX"
include(":app")
