/**
 * build.gradle.kts (backend)
 * Главный Gradle-файл бэкенда WhatsMAX.
 * Подключает Ktor, Exposed (ORM для PostgreSQL), Firebase Admin SDK,
 * корутины, логирование и тестовые зависимости.
 */

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project

plugins {
    kotlin("jvm") version "2.1.0"
    id("io.ktor.plugin") version "2.3.12"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

group = "com.whatsmax"
version = "1.0.0"

application {
    mainClass.set("com.whatsmax.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    // ─── Ktor Server Core ─────────────────────────────────────────────────
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-partial-content-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-request-validation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-rate-limit-jvm:$ktor_version")

    // ─── База данных (PostgreSQL + Exposed ORM) ───────────────────────────
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // ─── Firebase Admin SDK (верификация токенов) ─────────────────────────
    implementation("com.google.firebase:firebase-admin:9.2.0")

    // ─── Логирование ──────────────────────────────────────────────────────
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // ─── Корутины ─────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ─── Объектное хранилище (MinIO / S3) ────────────────────────────────
    implementation("io.minio:minio:8.5.12")

    // ─── Redis (pub/sub для multi-instance WebSocket) ────────────────────
    implementation("io.lettuce:lettuce-core:6.4.0.RELEASE")

    // ─── Тестирование ─────────────────────────────────────────────────────
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.mockk:mockk:1.13.9")
}

ktor {
    fatJar {
        archiveFileName.set("whatsmax-backend.jar")
    }
}
