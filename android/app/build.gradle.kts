/**
 * app/build.gradle.kts
 * Gradle-файл Android-приложения WhatsMAX.
 * Подключает: Jetpack Compose, Hilt DI, Retrofit, OkHttp WebSocket,
 * Firebase Auth, Room (кэш), Coil (изображения), WebRTC.
 * Использует KSP вместо kapt — совместимо с Gradle 8.x.
 */
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")          // KSP вместо kapt (нет конфликта с Gradle 8.x)
}

android {
    namespace = "com.whatsmax"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.whatsmax.messenger"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // !! ЗАМЕНИТЕ на IP вашего сервера !!
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
        buildConfigField("String", "WS_URL",   "\"ws://10.0.2.2:8080/ws\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // 10.0.2.2 — это localhost с точки зрения эмулятора Android
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
            buildConfigField("String", "WS_URL",   "\"ws://10.0.2.2:8080/ws\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

val hiltVersion = "2.50"

dependencies {
    // ─── Android Core ─────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // ─── Jetpack Compose ──────────────────────────────────────────────────
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ─── Hilt DI (KSP) ────────────────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")   // KSP вместо kapt
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // ─── Firebase ─────────────────────────────────────────────────────────
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // ─── Сеть: Retrofit + OkHttp ─────────────────────────────────────────
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // ─── Room (локальный кэш, KSP) ───────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")                       // KSP вместо kapt

    // ─── DataStore (настройки) ────────────────────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ─── Изображения: Coil ────────────────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ─── WebRTC (звонки и видеозвонки) ───────────────────────────────────
    implementation("io.getstream:stream-webrtc-android:1.1.3")

    // ─── CameraX (превью камеры во время видеозвонка) ─────────────────────
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // ─── Корутины ─────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ─── Сплэш-экран ──────────────────────────────────────────────────────
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ─── Тесты ────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
