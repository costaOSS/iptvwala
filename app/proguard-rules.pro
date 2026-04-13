# IPTVwala ProGuard Rules

# Keep NanoHTTPD
-keep class org.nanohttpd.** { *; }
-keepclassmembers class org.nanohttpd.** { *; }
-dontwarn org.nanohttpd.**

# Keep Room entities
-keep class com.iptvwala.data.local.entity.** { *; }
-keep class com.iptvwala.data.local.dao.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
}

# Keep Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Coil
-keep class coil.** { *; }

# Keep Retrofit/Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep ZXing
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }

# Keep WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep navigation args
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# General Android
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
