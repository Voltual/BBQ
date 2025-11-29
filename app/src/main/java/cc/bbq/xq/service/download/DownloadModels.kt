// 文件路径: cc/bbq/xq/service/download/DownloadModels.kt
package cc.bbq.xq.service.download

import kotlinx.serialization.Serializable

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