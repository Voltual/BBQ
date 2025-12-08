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
import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.crossfade
import coil3.util.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import cc.bbq.xq.data.db.AppDatabase
import cc.bbq.xq.data.SearchHistoryDataStore
import cc.bbq.xq.data.StorageSettingsDataStore
import cc.bbq.xq.data.UpdateSettingsDataStore
import cc.bbq.xq.data.UserAgreementDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication

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
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .networkFetcherFactory(KtorNetworkFetcherFactory())
            // 添加自定义缓存策略
            .components {
                add(CustomCacheStrategy())
            }
            .build()
    }
    
    companion object {
        lateinit var instance: BBQApplication
            private set
    }
}

// 自定义缓存策略
class CustomCacheStrategy : CacheStrategy {
    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options
    ): CacheStrategy.ReadResult {
        // 检查是否是用户头像URL
        val url = networkRequest.url
        if (url.startsWith("https://static.market.sineworld.cn/images/user_avatar/")) {
            // 对用户头像URL禁用缓存，总是发起网络请求
            return CacheStrategy.ReadResult(networkRequest)
        }
        
        // 对于其他URL，使用默认的缓存策略
        return CacheStrategy.DEFAULT.read(cacheResponse, networkRequest, options)
    }
    
    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options
    ): CacheStrategy.WriteResult {
        // 检查是否是用户头像URL
        val url = networkRequest.url
        if (url.startsWith("https://static.market.sineworld.cn/images/user_avatar/")) {
            // 对用户头像URL不写入缓存
            return CacheStrategy.WriteResult.DISABLED
        }
        
        // 对于其他URL，使用默认的缓存策略
        return CacheStrategy.DEFAULT.write(cacheResponse, networkRequest, networkResponse, options)
    }
}