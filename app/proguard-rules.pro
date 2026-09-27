# ==============================================================================
# UangKu - ProGuard & R8 Optimization and Obfuscation Rules
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. General Optimization & Debugging Info
# ------------------------------------------------------------------------------
# Preserve source file and line numbers for readable crash logs and stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve standard annotations and generic signatures for reflection/serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Allow aggressive optimizations where safe
-allowaccessmodification
-dontpreverify

# Suppress warnings from third-party build-time annotations
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn java.lang.invoke.**
-dontwarn org.checkerframework.**

# ------------------------------------------------------------------------------
# 2. Kotlin & Coroutines Rules
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @kotlin.jvm.JvmField *;
}

# Preserve Kotlin enum mappings
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Kotlin Coroutines internal reflection/atomic field updaters
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# 3. Android Jetpack & Architecture Components
# ------------------------------------------------------------------------------
# Jetpack Compose
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Jetpack ViewModel & Lifecycle
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}
-keep class androidx.lifecycle.ViewModelProvider$Factory { *; }

# ------------------------------------------------------------------------------
# 4. Room Database Rules
# ------------------------------------------------------------------------------
# Prevent obfuscation of Room Entities, DAOs, and Database classes
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-keep class * extends androidx.room.TypeConverter { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep class *_Impl { *; }

-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# 5. Application Data Models, Entities & DTOs
# ------------------------------------------------------------------------------
# Ensure Room database tables, backup serializer, and OCR entities are preserved
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

-keep class com.example.data.backup.** { *; }
-keepclassmembers class com.example.data.backup.** { *; }

-keep class com.example.network.ParsedReceipt { *; }
-keepclassmembers class com.example.network.ParsedReceipt { *; }

# Preserve local database package
-keep class com.example.data.local.** { *; }

# ------------------------------------------------------------------------------
# 6. Networking & Serialization (Moshi, Retrofit, OkHttp)
# ------------------------------------------------------------------------------
# Moshi reflection & codegen adapters
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep class *JsonAdapter { *; }
-dontwarn com.squareup.moshi.**

# Retrofit
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# OkHttp & Okio
-keepattributes EnclosingMethod
-keepclassmembers class okhttp3.internal.** {
    <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# ------------------------------------------------------------------------------
# 7. Firebase, Google Identity & Credential Manager
# ------------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**

# ------------------------------------------------------------------------------
# 8. Coil Image Loader
# ------------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**
