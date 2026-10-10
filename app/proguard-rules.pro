# R8 rules for the release build. Most libraries ship their own rules (Room, WorkManager,
# kotlinx-serialization, Compose, Filament via @UsedByNative, SceneView keeps
# com.google.android.filament.**). The lines below are explicit safeguards for code that is
# reached by name from native (C++) code or by reflection, where R8 can't see the usage.

# 3D engine: native code calls these Java classes/methods by name.
-keep class com.google.android.filament.** { *; }
-keep class io.github.sceneview.** { *; }

# Keep native method names (JNI looks them up by name).
-keepclasseswithmembernames class * {
    native <methods>;
}

# Backup file classes are (de)serialized by generated serializers; keep them intact so the
# JSON field names never change between builds.
-keep class com.achyutpal.scrolljourney.data.backup.BackupFile** { *; }

# Readable stack traces in crash logs (line numbers kept, file names hidden).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
