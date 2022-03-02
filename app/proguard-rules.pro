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


-dontobfuscate
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*, !code/allocation/variable

#keep everything
-keep class *.** {*;}

#zendesk
-keep class zendesk.core.AuthenticationRequestWrapper { *; }
-keep class zendesk.core.PushRegistrationRequestWrapper { *; }
-keep class zendesk.core.PushRegistrationRequest { *; }
-keep class zendesk.core.PushRegistrationResponse { *; }
-keep class zendesk.core.ApiAnonymousIdentity { *; }
-keep class zendesk.support.Comment { *; }
-keep class zendesk.support.CreateRequest { *; }
-keep class zendesk.support.CreateRequestWrapper { *; }
-keep class zendesk.support.EndUserComment { *; }
-keep class zendesk.support.Request { *; }
-keep class zendesk.support.UpdateRequestWrapper { *; }
