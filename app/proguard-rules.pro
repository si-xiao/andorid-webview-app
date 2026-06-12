# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# start --------------------------------- 极光推送 国际版 混淆规则 ----------------------------- start

#Specify the number of optimization passes to perform
-optimizationpasses 5

#Do not generate mixed case class names when confusing, that is, all lowercase
-dontusemixedcaseclassnames

#Specifies that classes that do not ignore non-public libraries
-dontskipnonpubliclibraryclasses

#Specifies that library class members (fields and methods) visible to the package are not ignored.
-dontskipnonpubliclibraryclassmembers

#The method names in the confusion class are also confused, and the unique confusion name is generated for the class to be confused
-useuniqueclassmembernames

#Turn off pre validation
-dontpreverify

#Print process log and output more information during processing
-verbose

#Specify optimization algorithm
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#Turn off optimization
-dontoptimize

#Expand the access permissions of classes and class members to allow access to and modification of classes and class members with modifiers during optimization
-allowaccessmodification

#Preserve parameter names of methods that need to be reserved
-keepparameternames

#Four components shall not be confused
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

#Rename the source of the obfuscation stack trace file to "SourceFile"
-renamesourcefileattribute SourceFile

#Protect annotations. If your code depends on comments, you may want to keep them
-keepattributes *Annotation*

#Preserve source file names, variable names, and line numbers to produce useful confusing stack traces
-keepattributes SourceFile,LineNumberTable

#Reserved exceptions, internal classes/interfaces, generics, deprecated methods
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,EnclosingMethod

#Keep the static members of the R file so that the calling code can access these fields through introspection
-keepclassmembers class **.R$* {
   public static <fields>;
}

#Engagelab
-keep class com.engagelab.** {*;}
-dontwarn com.engagelab.**

#google
-keep class com.google.**{*;}
-dontwarn com.google.**

#huawei
-keep class com.google.**{*;}
-dontwarn com.google.**

#xiaomi
-keep class com.xiaomi.** {*;}
-dontwarn com.xiaomi.**

#meizu，If R8 is used for confusion, the token may not be available
-keep class com.meizu.** {*;}
-dontwarn com.meizu.**

#oppo
-keep class com.heytap.** {*;}
-dontwarn com.heytap.**

#vivo
-keep class com.vivo.** {*;}
-dontwarn com.vivo.**

-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class com.huawei.hianalytics.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}
-keep class com.hihonor.push.**{*; }

# end --------------------------------- 极光推送 国际版 混淆规则 ----------------------------- end
