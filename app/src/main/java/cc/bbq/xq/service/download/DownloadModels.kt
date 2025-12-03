// 文件路径: cc/bbq/xq/service/download/DownloadModels.kt
package cc.bbq.xq.service.download

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 下载状态密封接口，用于 UI 响应式更新
 */
sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data object Pending : DownloadStatus
    data class Downloading(
        val progress: Float, // 0.0 - 1.0
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speed: String // e.g., "2.5 MB/s"
    ) : DownloadStatus
    data class Paused(val downloadedBytes: Long, val totalBytes: Long) : DownloadStatus
    data class Success(val file: java.io.File) : DownloadStatus
    data class Error(val message: String, val throwable: Throwable? = null) : DownloadStatus
}

/**
 * 下载配置
 */
data class DownloadConfig(
    val url: String,
    val savePath: String,
    val fileName: String,
    val threadCount: Int = 3 // 默认3线程并发
)

/**
 * 内部使用的分块信息
 */
internal data class Chunk(
    val id: Int,
    val start: Long,
    val end: Long,
    var current: Long
) {
    val size: Long get() = end - start + 1
    val isComplete: Boolean get() = current > end
}

/**
 * 下载任务实体类，用于 Room 数据库持久化存储
 */
@Entity(tableName = "download_tasks")
data class DownloadTask(
    @PrimaryKey val url: String, // 使用 URL 作为主键，保证唯一性
    val fileName: String,
    val savePath: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String, // 使用字符串存储下载状态，方便转换
    val progress: Float
)