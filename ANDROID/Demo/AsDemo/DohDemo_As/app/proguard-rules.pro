# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\android_develop\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#不去掉无用方法，有些方法是给外部提供使用的
#-dontshrink
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclassmembers
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-dontwarn android.support.**
-dontwarn android.support.**
-dontwarn com.polites.android.**

-ignorewarnings                # 抑制警告

#solution : @link http://stackoverflow.com/a/14463528
-dontnote com.google.vending.licensing.ILicensingService
-dontnote **ILicensingService

# For BatteryStats
-dontwarn android.os.**
-dontwarn android.app.**
-dontwarn com.android.internal.**
-dontwarn com.qq.e.**

-keep class android.support.** { *; }
-keep class android.support.** { *; }


# beacon SDK -- Start
-keep class com.tencent.beacon.** { *; }
# beacon SDK -- End


# libs/wup-1.0.0-SNAPSHOT.jar
-keep class com.qq.taf.** { *; }
-keep class com.qq.jce.wup.** { *; }


# XG SDK -- Start
# libs/Xg_sdk_v2.46.jar
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep class com.tencent.android.tpush.**  {* ;}
-keep public class com.tencent.android.tpush.service.channel.security.TpnsSecurity {* ;}
-keep class com.tencent.mid.**  {* ;}
# XG SDK -- End




-keep class android.** {
    <fields>;
    <methods>;
}
-keep class com.android.** {
    <fields>;
    <methods>;
}
-keep class com.google.** {
    <fields>;
    <methods>;
}


# gdt -- Start
-keep class com.qq.e.** { *; }
# gdt -- End


# wx share -- Start
-keep class com.tencent.mm.sdk.** { *; }
# wx share -- End


# NOTE: keep Serializable & Parcelable class
-keep class * implements java.io.Serializable { *; }
-keep class * implements android.os.Parcelable { *; }

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes Signature

-keepclasseswithmembernames class android.os.SystemProperties {
    public static <methods>;
}

-keep class org.xml.bind.annotation.*

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-assumenosideeffects class android.util.Log {
    public static *** e(...);
    public static *** w(...);
    public static *** i(...);
    public static *** d(...);
    public static *** v(...);
}


 #不混淆资源类
-keep class **.R$* {
*;
}

#腾讯地图
-keepattributes *Annotation*
-keepclassmembers class ** {
    public void on*Event(...);
}
-keepclasseswithmembernames class * {
    native <methods>;
}
-dontwarn  org.eclipse.jdt.annotation.**

# sdk版本小于18时需要以下配置, 建议使用18或以上版本的sdk编译
-dontwarn  android.location.Location
-dontwarn  android.net.wifi.WifiManager

-dontnote ct.**
#腾讯地图


#腾讯云
-keep class com.tencent.upload.** {*;}
#腾讯云


#doh混淆
-keep class com.tencent.doh.** {
   *;
}


# --------------------------------------------------------------------------
# Addidional for x5.sdk classes for apps

-keep class com.tencent.smtt.**{
    *;
}
-keep class com.tencent.tbs.video.interfaces.**{
    *;
}
-keep class MTT.**{
    *;
}

#---------------------------------------------------------------------------

-keep class com.polites.android.**{
    *;
}
