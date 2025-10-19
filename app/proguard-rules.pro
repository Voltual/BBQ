-assumenosideeffects class *Lambda {
    public *** get*(...);
    public *** invoke*(...);
}
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

-keep class tv.danmaku.ijk.media.player.** { *; }
# Kotlin 协程调试信息
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}