# kotlinx.serialization: @Serializable sınıfların serializer'ları korunur.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.mevlutcelik.kelimeler.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.mevlutcelik.kelimeler.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Credential Manager / Google ile giriş
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
