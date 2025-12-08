//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq

import android.app.Application
import cc.bbq.xq.ui.theme.ThemeManager
import cc.bbq.xq.ui.theme.ThemeColorStore
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.util.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import cc.bbq.xq.data.db.AppDatabase
import org.koin.core.annotation.KoinApplication
import java.io.File
import okio.Path
import okio.Path.Companion.toPath
import coil3.intercept.Interceptor
import coil3.intercept.Interceptor.Chain
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.ImageResult

@KoinApplication
class BBQApplication : Application(), SingletonImageLoader.Factory {
    // 数据库单例
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化 AuthManager
        AuthManager.initialize(this)
        
        // 初始化所有单例
        database = AppDatabase.getDatabase(this) // 初始化数据库
        
        // 初始化 AuthManager 并执行迁移
        CoroutineScope(Dispatchers.IO).launch {
            AuthManager.migrateFromSharedPreferences(applicationContext)
        }
        
        // 初始化主题管理器
        ThemeManager.initialize(this)
        
        // 加载并应用保存的自定义颜色
        ThemeManager.customColorSet = ThemeColorStore.loadColors(this)
        
        // 初始化 Koin
        startKoin {
            androidContext(this@BBQApplication)
            modules(appModule) // 假设 appModule 已经定义
        }
    }
    
    override fun newImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .crossfade(true)
        .logger(DebugLogger())
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                // ️ 修正: 使用 File.toOkioPath() 扩展函数
                .directory(context.cacheDir.resolve("image_cache").toOkioPath()) 
                .maxSizePercent(0.02)
                .build()
        }
        .components {
            //  修正: 添加 CustomCacheInterceptor，它实现了 Interceptor 接口
            add(CustomCacheInterceptor()) 
        }
        .build()
    }

    // ️ 修正: 扩展函数：File 转 Okio Path
    private fun File.toOkioPath(): Path = absolutePath.toPath()
    
    companion object {
        lateinit var instance: BBQApplication
            private set
    }
}

/**
 *  修正: 将 CacheStrategy 更改为 Interceptor。
 * Interceptor 允许我们在 Coil 执行请求链（包括缓存和网络）时注入自定义逻辑。
 * 目标是禁用特定 URL 的磁盘缓存。
 */
class CustomCacheInterceptor : Interceptor {
    override suspend fun intercept(chain: Chain): ImageResult {
        val request = chain.request
        // Coil 3 通常使用 Any 作为 data，通常是 String URL
        val url = request.data as? String 

        if (url != null && url.startsWith("https://static.market.sineworld.cn/images/user_avatar/15599.jpg")) {
            // 检查URL是否是用户头像，如果是，则创建一个新的 Request，
            // 显式禁用磁盘缓存（读/写）。
            val newRequest = request.newBuilder()
                .diskCachePolicy(CachePolicy.DISABLED) // 禁用磁盘缓存的读取和写入
                .build()
            
            // 继续链，使用新的请求
            return chain.proceed()
        }
        
        // 对于其他所有 URL，使用原始请求继续
        return chain.proceed()
    }
}
