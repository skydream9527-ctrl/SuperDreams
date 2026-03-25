# Proguard rules for Super Dreams

# Keep data classes for Gson serialization
-keepattributes Signature
-keep class com.superdreams.app.data.** { *; }
-keepclassmembers class com.superdreams.app.data.** { *; }

# Keep Gson
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
