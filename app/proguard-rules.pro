# Safe R8 / ProGuard rules to reduce APK size without breaking reflection or serialization
-dontobfuscate
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# Keep all app classes (Entities, SyncModels, Moshi models, ViewModels, UI)
-keep class com.aistudio.ecotobacco.kfzqw.** { *; }
-keepclassmembers class com.aistudio.ecotobacco.kfzqw.** { *; }

# Kotlin & Coroutines
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Moshi & Kotlin reflection
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# OkHttp & AppAuth & Google Play Services & Firebase & ML Kit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn net.openid.appauth.**
-dontwarn com.google.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
