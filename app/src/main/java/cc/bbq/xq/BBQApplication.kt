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
import cc.bbq.xq.data.ProcessedPostsDataStore
import cc.bbq.xq.ui.theme.ThemeManager
import cc.bbq.xq.ui.theme.ThemeColorStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import cc.bbq.xq.data.db.AppDatabase // 导入 AppDatabase
import cc.bbq.xq.data.SearchHistoryDataStore // 导入
import cc.bbq.xq.data.StorageSettingsDataStore // 导入 StorageSettingsDataStore
import cc.bbq.xq.data.UpdateSettingsDataStore // 导入 UpdateSettingsDataStore

class BBQApplication : Application() {

    // DataStore 单例
    lateinit var processedPostsDataStore: ProcessedPostsDataStore
        private set
        
    lateinit var searchHistoryDataStore: SearchHistoryDataStore // 新增
        private set

    // 新增：数据库单例
    lateinit var database: AppDatabase
        private set
        
    lateinit var storageSettingsDataStore: StorageSettingsDataStore // 新增
        private set
    
    lateinit var updateSettingsDataStore: UpdateSettingsDataStore // 新增
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化所有单例
        processedPostsDataStore = ProcessedPostsDataStore(this)
        database = AppDatabase.getDatabase(this) // 初始化数据库
        searchHistoryDataStore = SearchHistoryDataStore(this) // 新增
        storageSettingsDataStore = StorageSettingsDataStore(this) // 新增
        updateSettingsDataStore = UpdateSettingsDataStore

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
    companion object {
        lateinit var instance: BBQApplication
            private set
    }
}