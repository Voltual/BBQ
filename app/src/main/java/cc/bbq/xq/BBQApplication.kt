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
import coil3.request.ImageRequest
import coil3.request.cachePolicy
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
import coil3.request.CachePolicy

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
            modules(appModule)
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
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .components {
                add(CustomCacheInterceptor())
            }
            .build()
    }

    // 扩展函数：File 转 Okio Path
    private fun File.toOkioPath(): Path = absolutePath.toPath()
    
    companion object {
        lateinit var instance: BBQApplication
            private set
    }
}

/**
 * 修正后的拦截器实现
 * 
 * 关键点：
 * 1. 使用 `chain.request` 获取原始请求
 * 2. 创建新请求时使用 `newBuilder()` 克隆并修改
 * 3. 使用无参的 `proceed()` 方法继续执行
 */
class CustomCacheInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val url = request.data.toString() // 安全转换为字符串

        // 检查是否是用户头像URL
        if (url.startsWith("https://static.market.sineworld.cn/images/user_avatar/")) {
            // 创建禁用磁盘缓存的新请求
            val newRequest = request.newBuilder()
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
            
            // 使用新请求继续执行
            return chain.proceed(newRequest)
        }
        
        // 其他请求正常处理
        return chain.proceed(request)
    }
}