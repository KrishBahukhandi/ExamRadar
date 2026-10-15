# Proguard rules for ExamRadar India
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.examradar.app.data.model.** { *; }
