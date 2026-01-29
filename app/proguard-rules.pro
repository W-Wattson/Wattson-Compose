# Wattson ProGuard Rules

# Keep Kotlin metadata for reflection
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep data classes
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Keep kotlinx-serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.wattson.**$$serializer { *; }
-keepclassmembers class com.wattson.** {
    *** Companion;
}
-keepclasseswithmembers class com.wattson.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Hilt
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.* <fields>;
}

# Navigation Compose
-keep class androidx.navigation.** { *; }

# Coil
-dontwarn coil.**

# Keep domain models
-keep class com.wattson.domain.model.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Composables for Preview
-keep class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}
