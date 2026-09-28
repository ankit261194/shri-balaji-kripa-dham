# ==============================================================================
# Shri Balaji Kripa Dham - Production ProGuard / R8 Obfuscation & Shrinking Rules
# ==============================================================================

# 1. Keep Data Models, DTOs & Sacred Entities
-keep class com.example.shribalajikripadham.data.model.** { *; }
-keepclassmembers class com.example.shribalajikripadham.data.model.** { *; }
-keep class com.example.shribalajikripadham.data.sacred.** { *; }
-keepclassmembers class com.example.shribalajikripadham.data.sacred.** { *; }

# 2. Keep Kotlin Serialization & JSON parsing
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# 3. Keep Jetpack Compose & UI State
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# 4. Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-dontwarn kotlinx.coroutines.**

# 5. Keep TensorFlow Lite & Face Embedding AI Engine
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
-keep class com.example.shribalajikripadham.ai.** { *; }
-keepclassmembers class com.example.shribalajikripadham.ai.** { *; }

# 6. Keep Firebase Cloud Messaging (FCM) & Google Services
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# 7. Keep CameraX & Hardware Fingerprint
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class com.example.shribalajikripadham.hardware.** { *; }
-keepclassmembers class com.example.shribalajikripadham.hardware.** { *; }

# 8. Keep ZXing Barcode / QR Code Scanner & Bluetooth
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
-keep class com.example.shribalajikripadham.util.BluetoothThermalPrinterHelper** { *; }

# 9. General R8 configuration
-dontoptimize
-dontusemixedcaseclassnames
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

