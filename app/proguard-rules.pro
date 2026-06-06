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

# --- Gson TypeToken: generic type arguments must survive R8. Gson 2.8.5 ships no
#     consumer rules, so without this an inline `object : TypeToken<...>(){}` loses its
#     type argument and throws at runtime (e.g. opening the Tinker tab in the inspector). ---
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# --- Particle Cloud SDK Gson models / API DTOs (serialized by name via reflection) ---
-keep class io.particle.android.sdk.cloud.models.** { *; }
-keep class io.particle.android.sdk.cloud.Responses$** { *; }
-keep class io.particle.android.sdk.cloud.DeviceState { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Tinker pin definitions are deserialized from a JSON asset by Kotlin property name,
#     so their fields must not be renamed/stripped. ---
-keep class io.particle.android.sdk.tinker.pinreader.** { *; }

# --- Cloud SSE event model (deserialized by Gson by field name) ---
-keep class io.particle.android.sdk.cloud.ParticleEvent { *; }
-keep class io.particle.android.sdk.cloud.ParticleEvent$** { *; }

# --- Kaazing SSE client: loaded reflectively via Class.forName, drives the live-events
#     stream. Keep the whole library (and silence its missing-dependency warnings). ---
-keep class org.kaazing.** { *; }
-dontwarn org.kaazing.**

# --- Device-setup soft-AP command/response DTOs (Gson-serialized over the socket
#     during Photon Wi-Fi setup), deserialized by field name. ---
-keep class io.particle.android.sdk.devicesetup.commands.** { *; }

# --- AndroidX Navigation: fragment destinations and custom argument types are referenced by
#     name in the nav-graph XML (not from code), so R8 must not rename/remove them — otherwise
#     graph inflation throws "Error inflating class fragment" (e.g. opening the Control Panel,
#     or later steps of Gen 3 device setup). Keep the fragments, Parcelable nav args, and the
#     custom argType enum used by the control-panel graph. (The protobuf argType is kept below.)
-keep public class * extends androidx.fragment.app.Fragment
-keepnames class * extends android.os.Parcelable
-keep class io.particle.mesh.setup.flow.SimStatusChangeMode { *; }

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

# --- SpongyCastle crypto (device-setup public-key handshake). ASN.1 + JCE provider
#     classes are partly resolved by name/service lookup, so keep the library. ---
-keep class org.spongycastle.** { *; }
-dontwarn org.spongycastle.**

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
