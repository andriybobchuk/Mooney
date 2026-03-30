# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.andriybobchuk.mooney.**$$serializer { *; }
-keepclassmembers class com.andriybobchuk.mooney.** { *** Companion; }
-keepclasseswithmembers class com.andriybobchuk.mooney.** { kotlinx.serialization.KSerializer serializer(...); }

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# DataStore
-keep class androidx.datastore.** { *; }

# Compose Navigation (Route serialization)
-keep class com.andriybobchuk.mooney.app.Route$* { *; }

# SQLite bundled driver
-keep class androidx.sqlite.driver.bundled.** { *; }
