# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\android-sdk-windows-as/tools/proguard/proguard-android.txt
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

-keepclassmembers class com.tencent.photoview.ImageViewerActivity {
    public static <fields>;
}
-keep class com.tencent.photoview.ImageLoaderInit {
    *;
}
-keep class com.nostra13.universalimageloader.** {
    *;
}

# support
-dontwarn android.support.**
-keep class android.support.** { *; }
-keep interface android.support.** { *; }
-keep class android.support.** { *; }

#solution : @link http://stackoverflow.com/a/14463528
-dontnote com.google.vending.licensing.ILicensingService
-dontnote **ILicensingService