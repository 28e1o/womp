# Keep model classes (parsed manually via JSONObject reflection-free, but keep names for safety in logs)
-keepattributes SourceFile,LineNumberTable
-keep class cloud.wumboing.rpchat.data.** { *; }

# Standard Android/Kotlin
-dontwarn kotlin.**
-dontwarn kotlinx.**
