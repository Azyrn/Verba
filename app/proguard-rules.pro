# R8 full-mode rules for Verba release build.

# ---- kotlinx.serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep generated serializers for @Serializable classes in this app.
-keep,includedescriptorclasses class com.skeler.verba.**$$serializer { *; }
-keepclassmembers class com.skeler.verba.** {
    *** Companion;
}
-keepclasseswithmembers class com.skeler.verba.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep the serializer() lookup on generated companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# ---- Retrofit / OkHttp ----
# Retrofit does reflection on generic method signatures and interface annotations.
-keepattributes Signature, Exceptions, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation interface com.skeler.verba.data.remote.ChatApi { *; }
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Platform-specific / optional deps that R8 warns about but the app never touches.
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# Dagger/Hilt references errorprone annotations that are compile-only.
-dontwarn com.google.errorprone.annotations.**

# Hilt/Dagger ship their own consumer proguard rules — no manual keeps needed.
