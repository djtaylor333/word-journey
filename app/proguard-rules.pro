# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep data classes used with kotlinx.serialization
-keepattributes *Annotation*
-keep @kotlinx.serialization.Serializable class * {*;}
-keep class kotlinx.serialization.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
    @javax.inject.Inject *;
}

# Google Play Games
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker { public <init>(android.content.Context, androidx.work.WorkerParameters); }
-keepclassmembers class * extends androidx.work.CoroutineWorker { public <init>(android.content.Context, androidx.work.WorkerParameters); }

# DataStore
-keep class androidx.datastore.** { *; }

# App-specific: never obfuscate model classes used in serialization
-keep class com.djtaylor.wordjourney.domain.model.** { *; }
-keep class com.djtaylor.wordjourney.data.db.** { *; }
