// 文件路径: cc/bbq/xq/data/db/AppDatabase.kt
package cc.bbq.xq.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cc.bbq.xq.service.download.DownloadTask
import cc.bbq.xq.ui.community.BrowseHistory

@Database(
    entities = [LogEntry::class, BrowseHistory::class, NetworkCacheEntry::class, PostDraft::class, DownloadTask::class],
    version = 5, // 增加数据库版本号
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun browseHistoryDao(): BrowseHistoryDao
    abstract fun networkCacheDao(): NetworkCacheDao
    abstract fun postDraftDao(): PostDraftDao
    abstract fun downloadTaskDao(): DownloadTaskDao // 添加 DownloadTaskDao 抽象方法

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 修正参数名称以匹配父类
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `browse_history` (`postId` INTEGER NOT NULL, `title` TEXT NOT NULL, `previewContent` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`postId`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `network_cache` (`requestKey` TEXT NOT NULL, `responseJson` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`requestKey`))"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `post_draft` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `imageUris` TEXT NOT NULL, `imageUrls` TEXT NOT NULL, `subsectionId` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        // 添加新的迁移脚本，用于创建 download_tasks 表
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `download_tasks` (`url` TEXT NOT NULL, `fileName` TEXT NOT NULL, `savePath` TEXT NOT NULL, `totalBytes` INTEGER NOT NULL, `downloadedBytes` INTEGER NOT NULL, `status` TEXT NOT NULL, `progress` REAL NOT NULL, PRIMARY KEY(`url`))"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qubot_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5) // 添加新的迁移脚本
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}