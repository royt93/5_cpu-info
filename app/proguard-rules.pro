# Kotlin
-dontwarn kotlin.**

-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Dagger
-dontwarn com.google.errorprone.annotations.*

# General
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,SourceFile,LineNumberTable,*Annotation*

# AIDL
-keep class android.content.pm.IPackageStatsObserver$** {
    public <fields>;
    public <methods>;
}
-keep class android.content.pm.IPackageStatsObserver$Stub.** {
    public <fields>;
    public <methods>;
}
-keep interface android.content.pm.IPackageStatsObserver$** {*;}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Coroutines
# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Remove logs
-assumenosideeffects class timber.log.Timber* {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** e(...);
}

# VerifyError in Android 4
# https://github.com/material-components/material-components-android/issues/397
-keep class com.google.android.material.tabs.TabLayout$Tab {
*;
}

-ignorewarnings

#google
-keep public class com.google.** {*;}

# Google Cloud message / Play services (ad system — AppLovin/AdMob wrapper depends on these transitively)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.

-keep class com.google.android.gms.ads.identifier.** { *; }

#Gson
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }

# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keepclassmembers enum * { *; }

# MPAndroidChart (F01 realtime dashboard) — no bundled consumer rules, keep everything.
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**
