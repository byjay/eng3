# Release R8 / ProGuard rules for SpeakQuest / DadKingdom Android

# Preserve @Keep annotated classes and members (Firestore POJOs & serialization models)
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Preserve Firestore data models and their default no-arg constructors + getters/setters
-keepclassmembers class com.example.model.** {
    public <init>(...);
    public *** get*(...);
    public void set*(...);
    public boolean is*(...);
}
-keep class com.example.model.** { *; }

# Preserve Moshi generated adapters & Kotlin metadata required for reflection
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter {
    public <init>(...);
}

# Preserve JavaScript interface callbacks used in WebView bridges
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve stack trace line numbers without exposing original source file names
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
