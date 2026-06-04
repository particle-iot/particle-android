# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/jknutson/Library/ADT/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# --- Logging (slf4j / kotlin-logging / slf4android) ---
-dontwarn org.slf4j.**

# --- Keep attributes needed for Gson reflection, generics and annotations ---
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, RuntimeVisibleAnnotations, AnnotationDefault

# --- Particle Cloud SDK Gson models / API DTOs (serialized by name via reflection) ---
-keep class io.particle.android.sdk.cloud.models.** { *; }
-keep class io.particle.android.sdk.cloud.Responses$** { *; }
-keep class io.particle.android.sdk.cloud.DeviceState { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Retrofit 2 interfaces (annotations drive runtime behaviour) ---
-keep,allowobfuscation,allowshrinking interface io.particle.android.sdk.cloud.ApiDefs$*
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- Protobuf (firmware control messages) ---
-keep class com.google.protobuf.** { *; }
-keep class io.particle.firmwareprotos.** { *; }
-dontwarn com.google.protobuf.**

# --- EventBus subscriber methods ---
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# --- JNI (ecjpake4j native bridge) ---
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keep class io.particle.ecjpake4j.** { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
