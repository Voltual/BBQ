# 移除过于宽泛的规则
 -keepnames class ** { *; }  # 删除这一行

# 优化配置
-optimizations enum/unboxing/*
-optimizations class/merging/*

# 更精确的 Lambda 规则
-assumenosideeffects class *Lambda {
    public *** get*(...);
    public *** invoke*(...);
}

# Log 移除（这个可以保留）
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# 保留必要的库
-keep class tv.danmaku.ijk.media.player.** { *; }

# 添加缺失的 SLF4J 保留规则
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# Kotlin 协程调试信息
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}

# 添加一些常见的保留规则
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class kotlin.** { *; }
-dontwarn kotlin.**