-flattenpackagehierarchy
-allowaccessmodification
-keepattributes Exceptions,InnerClasses,Signature,SourceFile,LineNumberTable
-dontskipnonpubliclibraryclassmembers
-ignorewarnings
#kotlin
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

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keep class **.R$* {*;}
-keepclassmembers enum * { *;}

#mars
-keep class com.tencent.mars.** { *; }

#rx
-keep class rx.internal.util.unsafe.** { *; }
-keep class android.databinding.** { *; }

#Gson
-keepclassmembers public class com.google.gson.**
-keepclassmembers public class com.google.gson.** {public private protected *;}
-keepclassmembers public class com.project.mocha_patient.login.SignResponseData { private *; }
-keepclassmembers class sun.misc.Unsafe { *; }
-keep @interface com.google.gson.annotations.SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class **$Properties {*;}

#Qiniu SDK
-keep class com.qiniu.**{*;}
-keep class com.qiniu.**{public <init>();}
-ignorewarnings

#player
-keep public class cn.jzvd.JZMediaSystem {*; }
-keep public class cn.jzvd.demo.CustomMedia.CustomMedia {*; }
-keep public class cn.jzvd.demo.CustomMedia.JZMediaIjk {*; }
-keep public class cn.jzvd.demo.CustomMedia.JZMediaSystemAssertFolder {*; }

-keep class tv.danmaku.ijk.media.player.** {*; }
-dontwarn tv.danmaku.ijk.media.player.*
-keep interface tv.danmaku.ijk.media.player.** { *; }

# ProGuard configurations for Bugtags
-keepattributes LineNumberTable,SourceFile
-keep class com.bugtags.library.** {*;}
-dontwarn com.bugtags.library.**
-keep class io.bugtags.** {*;}
-dontwarn io.bugtags.**
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient