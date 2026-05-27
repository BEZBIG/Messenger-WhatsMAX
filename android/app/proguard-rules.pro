# proguard-rules.pro
# Правила ProGuard для WhatsMAX Android приложения (release сборка)

# Сохраняем Kotlin-метаданные
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Retrofit
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.whatsmax.**$$serializer { *; }
-keepclassmembers class com.whatsmax.** {
    *** Companion;
}
-keepclasseswithmembers class com.whatsmax.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# WebRTC
-keep class org.webrtc.** { *; }
-keep class io.getstream.webrtc.** { *; }

# Domain models (не обфусцировать)
-keep class com.whatsmax.domain.model.** { *; }
-keep class com.whatsmax.data.remote.dto.** { *; }
