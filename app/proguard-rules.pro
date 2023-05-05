# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

-keep,allowobfuscation class ru.herobrine1st.e621.api.model.** {
    <init>(...);
}

-keepclassmembers class ru.herobrine1st.e621.api.model.** {
    public *** get*();
}

-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* *;
}

-keepclassmembers,allowobfuscation,allowoptimization @kotlinx.parcelize.Parcelize class * {
    *;
}

# https://github.com/square/retrofit/issues/3880
# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response