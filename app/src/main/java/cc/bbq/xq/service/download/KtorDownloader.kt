// 文件路径: cc/bbq/xq/service/download/KtorDownloader.kt
package cc.bbq.xq.service.download

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.counted
import io.ktor.utils.io.jvm.javaio.copyTo  // 用于 NIO copyTo
import io.ktor.utils.io.readAvailable  // 关键修复：导入扩展函数
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException

/**
 * 简化版的Ktor下载器，整合Droid-ify稳定逻辑（已修复编译错误 + 优化）
 */
class KtorDownloader {
    
    companion object {
        private const val TAG = "KtorDownloader"
        private const val CONNECTION_TIMEOUT = 30_000L
        private const val SOCKET_TIMEOUT = 60_000L
        private const val REQUEST_TIMEOUT = Long.MAX_VALUE
        private const val BUFFER_SIZE = 64 * 1024  // 优化缓冲
    }

    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status: StateFlow<DownloadStatus> = _status.asStateFlow()

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT
            connectTimeoutMillis = CONNECTION_TIMEOUT
            socketTimeoutMillis = SOCKET_TIMEOUT
        }
    }

    suspend fun startDownload(config: DownloadConfig) {
        _status.value = DownloadStatus.Pending
        
        try {
            val file = File(config.savePath, config.fileName)
            file.parentFile?.mkdirs()

            val existingSize = if (file.exists()) file.length() else 0L

            val fileInfo = getFileInfo(config.url)
            val totalLength = fileInfo.contentLength
            val supportRange = fileInfo.acceptRanges

            Log.d(TAG, "File info: total=$totalLength, range=$supportRange, existing=$existingSize")

            when {
                existingSize >= totalLength && totalLength > 0 -> {
                    _status.value = DownloadStatus.Success(file)
                    return
                }
                supportRange && config.threadCount > 1 && (totalLength - existingSize) > BUFFER_SIZE -> {
                    downloadWithResume(config.url, file, totalLength, existingSize, config.threadCount)
                }
                supportRange && existingSize > 0 -> {
                    downloadWithResume(config.url, file, totalLength, existingSize, 1)
                }
                else -> {
                    downloadSimple(config.url, file, totalLength)
                }
            }

        } catch (e: CancellationException) {
            Log.d(TAG, "Download cancelled")
            _status.value = DownloadStatus.Idle
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _status.value = DownloadStatus.Error(
                message = e.message ?: "下载失败",
                throwable = e
            )
        }
    }

    private suspend fun getFileInfo(url: String): FileInfo {
        return try {
            val response = client.head(url)
            if (!response.status.isSuccess()) {
                throw Exception("HTTP ${response.status.value}: ${response.status.description}")
            }
            FileInfo(
                contentLength = response.contentLength() ?: -1L,
                acceptRanges = response.headers[HttpHeaders.AcceptRanges] == "bytes",
                lastModified = response.headers[HttpHeaders.LastModified],
                etag = response.headers[HttpHeaders.ETag]
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get file info, using defaults", e)
            FileInfo()
        }
    }

    private suspend fun downloadWithResume(
        url: String,
        file: File,
        totalLength: Long,
        existingSize: Long,
        threadCount: Int
    ) = coroutineScope {
        Log.d(TAG, "Resume download: total=$totalLength, existing=$existingSize, threads=$threadCount")
        
        if (totalLength > 0) {
            withContext(Dispatchers.IO) {
                RandomAccessFile(file, "rw").use { it.setLength(totalLength) }
            }
        }

        val downloadedTotal = AtomicLong(existingSize)
        val startTime = System.currentTimeMillis()
        
        if (threadCount > 1) {
            val remaining = totalLength - existingSize
            val chunkSize = remaining / threadCount
            val chunks = mutableListOf<Chunk>()
            
            var currentStart = existingSize
            for (i in 0 until threadCount) {
                val start = currentStart
                val end = if (i == threadCount - 1) totalLength - 1 else currentStart + chunkSize - 1
                // 计算 chunk 已下载量（简单验证：从文件实际读取位置）
                val chunkCurrent = computeChunkCurrent(file, start, end)
                if (chunkCurrent < end + 1) {
                    chunks.add(Chunk(i, start, end, chunkCurrent))
                }
                currentStart = end + 1
            }

            val tasks = chunks.map { chunk ->
                async(Dispatchers.IO) {
                    downloadChunk(url, file, chunk) { bytesRead ->
                        val currentTotal = downloadedTotal.addAndGet(bytesRead.toLong())
                        updateProgress(currentTotal, totalLength, startTime)
                    }
                }
            }
            tasks.forEach { it.await() }
        } else {
            val chunkCurrent = computeChunkCurrent(file, existingSize, totalLength - 1)
            downloadChunk(url, file, Chunk(0, existingSize, totalLength - 1, chunkCurrent)) { bytesRead ->
                val currentTotal = downloadedTotal.addAndGet(bytesRead.toLong())
                updateProgress(currentTotal, totalLength, startTime)
            }
        }
        
        _status.value = DownloadStatus.Success(file)
    }

    // 新增：计算 chunk 已下载位置（简单 MD5 校验或位置检查）
    private fun computeChunkCurrent(file: File, start: Long, end: Long): Long {
        return if (file.length() > start) start else 0L  // 简化，实际可加校验和
    }

    private suspend fun downloadSimple(url: String, file: File, totalLength: Long) {
        Log.d(TAG, "Simple download")
        val startTime = System.currentTimeMillis()
        var downloadedBytes = 0L
        
        val response = client.get(url)
        if (!response.status.isSuccess()) {
            throw Exception("Download failed: ${response.status}")
        }
        
        val countedChannel = response.bodyAsChannel().counted()  // 自动计数
        withContext(Dispatchers.IO) {
            file.outputStream().use { output ->
                countedChannel.copyTo(output)  // 高效 copy
            }
            downloadedBytes = countedChannel.totalBytesRead
        }
        updateProgress(downloadedBytes, totalLength.coerceAtLeast(downloadedBytes), startTime)
        _status.value = DownloadStatus.Success(file)
    }

    private suspend fun downloadChunk(
        url: String,
        file: File,
        chunk: Chunk,
        onBytesRead: (Int) -> Unit
    ) {
        if (chunk.current > chunk.end) return

        Log.d(TAG, "Chunk ${chunk.id}: ${chunk.current}-${chunk.end}")

        val response = client.get(url) {
            header(HttpHeaders.Range, "bytes=${chunk.current}-${chunk.end}")
        }

        if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent) {
            throw Exception("Chunk ${chunk.id} failed: ${response.status}")
        }

        val channel = response.bodyAsChannel()
        withContext(Dispatchers.IO) {
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(chunk.current)
                val rafChannel: FileChannel = raf.channel
                val buffer = ByteArray(BUFFER_SIZE)
                var totalChunkRead = 0L

                // 关键修复：suspend loop with readAvailable
                while (isActive && !channel.isClosedForRead()) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead < 0) break
                    raf.write(buffer, 0, bytesRead)
                    chunk.current += bytesRead
                    totalChunkRead += bytesRead
                    onBytesRead(bytesRead)

                    if (chunk.current > chunk.end) break
                }
                Log.d(TAG, "Chunk ${chunk.id} done: read $totalChunkRead bytes")
            }
        }
    }

    private fun updateProgress(current: Long, total: Long, startTime: Long) {
        if (total <= 0) return
        val progress = (current / total.toFloat()).coerceAtMost(1f)
        val elapsed = (System.currentTimeMillis() - startTime) / 1000f
        val speed = if (elapsed > 0) formatSpeed(current / elapsed) else ""

        val currentStatus = _status.value
        val lastProgress = (currentStatus as? DownloadStatus.Downloading)?.progress ?: 0f
        if (currentStatus !is DownloadStatus.Downloading || (progress - lastProgress) > 0.01f || progress >= 1f) {
            _status.value = DownloadStatus.Downloading(progress, current, total, speed)
        }
    }

    private fun formatSpeed(bytesPerSecond: Float): String = when {
        bytesPerSecond >= 1_048_576f -> "%.1f MB/s".format(bytesPerSecond / 1_048_576f)
        bytesPerSecond >= 1024f -> "%.1f KB/s".format(bytesPerSecond / 1024f)
        else -> "%.0f B/s".format(bytesPerSecond)
    }

    fun cancel() {
        client.cancel(CancellationException("User cancelled"))
        _status.value = DownloadStatus.Idle
    }

    suspend fun close() {
        client.close()
    }
}

// 补充缺失数据类
data class DownloadConfig(
    val url: String,
    val savePath: String,
    val fileName: String,
    val threadCount: Int = 3
)

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    object Pending : DownloadStatus()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speed: String
    ) : DownloadStatus()
    data class Success(val file: File) : DownloadStatus()
    data class Error(val message: String, val throwable: Throwable? = null) : DownloadStatus()
}

private data class FileInfo(
    val contentLength: Long = -1L,
    val acceptRanges: Boolean = false,
    val lastModified: String? = null,
    val etag: String? = null
)

private data class Chunk(  // 补充缺失类
    val id: Int,
    val start: Long,
    val end: Long,
    var current: Long
)