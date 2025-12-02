// 文件路径: cc/bbq/xq/service/download/KtorDownloader.kt
package cc.bbq.xq.service.download

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.onDownload
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
import io.ktor.utils.io.core.*
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException

/**
 * 简化版的Ktor下载器，整合Droid-ify稳定逻辑
 */
class KtorDownloader {
    
    companion object {
        private const val TAG = "KtorDownloader"
        private const val CONNECTION_TIMEOUT = 30_000L
        private const val SOCKET_TIMEOUT = 60_000L
        private const val REQUEST_TIMEOUT = Long.MAX_VALUE  // 大文件下载不超时
        private const val BUFFER_SIZE = 8192
        private const val PACKET_SIZE = 16384  // Larger packet for efficiency
    }

    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status: StateFlow<DownloadStatus> = _status.asStateFlow()

    // 简化的HTTP客户端，去掉不必要的插件
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT
            connectTimeoutMillis = CONNECTION_TIMEOUT
            socketTimeoutMillis = SOCKET_TIMEOUT
        }
    }

    /**
     * 开始下载 - 主入口方法
     */
    suspend fun startDownload(config: DownloadConfig) {
        _status.value = DownloadStatus.Pending
        
        try {
            val file = File(config.savePath, config.fileName)
            file.parentFile?.mkdirs()

            // 检查文件是否已存在（部分下载）
            val existingSize = if (file.exists()) file.length() else 0L

            // 1. 获取文件信息（支持断点续传）
            val fileInfo = getFileInfo(config.url)
            val totalLength = fileInfo.contentLength
            val supportRange = fileInfo.acceptRanges

            Log.d(TAG, "File info: total=$totalLength, range=$supportRange, existing=$existingSize")

            // 2. 决定下载策略
            when {
                // 文件已完整下载
                existingSize >= totalLength && totalLength > 0 -> {
                    Log.d(TAG, "File already downloaded completely")
                    _status.value = DownloadStatus.Success(file)
                    return
                }
                
                // 支持断点续传且需要多线程
                supportRange && config.threadCount > 1 && totalLength > 1024 * 1024 -> { // 大于1MB才用多线程
                    downloadWithResume(config.url, file, totalLength, existingSize, config.threadCount)
                }
                
                // 支持断点续传（单线程续传）
                supportRange && existingSize > 0 -> {
                    downloadWithResume(config.url, file, totalLength, existingSize, 1)
                }
                
                // 普通下载
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

    /**
     * 获取文件信息（模仿Droid-ify的headCall）
     */
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
            FileInfo() // 返回默认值
        }
    }

    /**
     * 支持断点续传的下载（多线程或单线程）
     */
    private suspend fun downloadWithResume(
        url: String,
        file: File,
        totalLength: Long,
        existingSize: Long,
        threadCount: Int
    ) = coroutineScope {
        Log.d(TAG, "Starting resume download: total=$totalLength, existing=$existingSize, threads=$threadCount")
        
        // 预分配文件大小（如果需要）
        if (totalLength > 0 && file.length() < totalLength) {
            withContext(Dispatchers.IO) {
                RandomAccessFile(file, "rw").use { raf ->
                    raf.setLength(totalLength)
                }
            }
        }

        val downloadedTotal = AtomicLong(existingSize)
        val startTime = System.currentTimeMillis()
        
        if (threadCount > 1) {
            // 多线程分块下载
            val chunkSize = (totalLength - existingSize) / threadCount
            val chunks = ArrayList<Chunk>()
            
            for (i in 0 until threadCount) {
                val start = existingSize + (i * chunkSize)
                val end = if (i == threadCount - 1) 
                    totalLength - 1 
                else 
                    existingSize + ((i + 1) * chunkSize) - 1
                
                if (start < totalLength) {
                    chunks.add(Chunk(i, start, end, start))
                }
            }

            // 并发下载所有分块
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
            // 单线程续传
            downloadChunk(url, file, 
                Chunk(0, existingSize, totalLength - 1, existingSize)
            ) { bytesRead ->
                val currentTotal = downloadedTotal.addAndGet(bytesRead.toLong())
                updateProgress(currentTotal, totalLength, startTime)
            }
        }
        
        _status.value = DownloadStatus.Success(file)
        Log.d(TAG, "Download completed successfully")
    }

    /**
     * 简单下载（不支持断点续传）
     */
    private suspend fun downloadSimple(url: String, file: File, totalLength: Long) {
        Log.d(TAG, "Starting simple download")
        
        val startTime = System.currentTimeMillis()
        var downloadedBytes = 0L
        
        val response = client.get(url) {
            onDownload { bytesSentTotal, contentLength ->
                downloadedBytes = bytesSentTotal
                val total = contentLength ?: totalLength
                updateProgress(bytesSentTotal, total, startTime)
            }
        }
        
        if (!response.status.isSuccess()) {
            throw Exception("Download failed: ${response.status}")
        }
        
        // 使用Droid-ify的稳定方式写入文件
        file.outputStream().use { output ->
            response.bodyAsChannel().copyTo(output)
        }
        
        _status.value = DownloadStatus.Success(file)
    }

/**
 * 下载单个分块 - 修复版本
 */
private suspend fun downloadChunk(
    url: String,
    file: File,
    chunk: Chunk,
    onBytesRead: (Int) -> Unit
) {
    Log.d(TAG, "Downloading chunk ${chunk.id}: ${chunk.start}-${chunk.end}")
    
    if (chunk.current > chunk.end) {
        Log.d(TAG, "Chunk ${chunk.id} already completed")
        return
    }

    val response = client.get(url) {
        if (chunk.start > 0 || chunk.end < Long.MAX_VALUE) {
            header(HttpHeaders.Range, "bytes=${chunk.start}-${chunk.end}")
        }
    }

    if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent) {
        throw Exception("Chunk ${chunk.id} failed: ${response.status}")
    }

    withContext(Dispatchers.IO) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(chunk.current)
            
            // 将 FileChannel 转换为 OutputStream
            val outputStream = Channels.newOutputStream(raf.channel)
            
            // 使用正确的 copyTo 方法（针对 OutputStream）
            // 这个版本只接受 limit 参数，不接受回调
            val bytesCopied = response.bodyAsChannel().copyTo(outputStream, limit = chunk.end - chunk.current + 1)
            
            chunk.current += bytesCopied
            onBytesRead(bytesCopied.toInt())
        }
    }
    
    Log.d(TAG, "Chunk ${chunk.id} completed at ${chunk.current}")
}

    /**
     * 更新进度（带限流）
     */
    private fun updateProgress(current: Long, total: Long, startTime: Long) {
        if (total <= 0) return
        
        val progress = current.toFloat() / total.toFloat()
        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f
        val speed = if (elapsedSeconds > 0) {
            val bytesPerSecond = current / elapsedSeconds
            formatSpeed(bytesPerSecond)
        } else {
            ""
        }
        
        // 限流：只有进度变化明显或下载完成时才更新
        val currentStatus = _status.value
        val lastProgress = if (currentStatus is DownloadStatus.Downloading) currentStatus.progress else 0f
        
        if (currentStatus !is DownloadStatus.Downloading || 
            (progress - lastProgress) > 0.01f ||
            progress >= 1.0f) {
            
            _status.value = DownloadStatus.Downloading(
                progress = progress,
                downloadedBytes = current,
                totalBytes = total,
                speed = speed
            )
        }
    }
    
    /**
     * 格式化下载速度
     */
    private fun formatSpeed(bytesPerSecond: Float): String {
        return when {
            bytesPerSecond >= 1024 * 1024 -> {
                String.format("%.1f MB/s", bytesPerSecond / (1024 * 1024))
            }
            bytesPerSecond >= 1024 -> {
                String.format("%.1f KB/s", bytesPerSecond / 1024)
            }
            else -> {
                String.format("%.0f B/s", bytesPerSecond)
            }
        }
    }

    /**
     * 取消下载（不关闭client，允许重用）
     */
    fun cancel() {
        _status.value = DownloadStatus.Idle
        Log.d(TAG, "Download cancelled")
    }

    /**
     * 清理资源
     */
    fun close() {
        client.close()
    }
}

/**
 * 文件信息数据类（简化版）
 */
private data class FileInfo(
    val contentLength: Long = -1L,
    val acceptRanges: Boolean = false,
    val lastModified: String? = null,
    val etag: String? = null
)