# Proguard rules for ExamRadar India
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Gson & Data Models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.examradar.app.data.model.** { *; }
-keep class com.examradar.app.data.remote.** { *; }
-keep class com.examradar.app.data.preferences.** { *; }
-keep class com.examradar.app.data.cache.** { *; }

# OkHttp & Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Firebase Cloud Messaging
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
