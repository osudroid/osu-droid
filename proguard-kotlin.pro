# =============================================================================
# osu!droid Kotlin-specific ProGuard configuration
# =============================================================================

-flattenpackagehierarchy
-allowaccessmodification
-dontskipnonpubliclibraryclassmembers
-keepattributes Exceptions,InnerClasses,Signature,SourceFile,LineNumberTable

# ----- Kotlin ----------------------------------------------------------------
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ----- Native methods --------------------------------------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}

# ----- Android ---------------------------------------------------------------
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keep class **.R$* { *; }
-keepclassmembers enum * { *; }
