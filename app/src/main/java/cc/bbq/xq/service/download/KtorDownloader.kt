// 文件路径: cc/bbq/xq/service/download/KtorDownloader.kt
package cc.bbq.xq.service.download

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
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
import java.util.concurrent.atomic.AtomicLong

class KtorDownloader {

    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status: StateFlow<DownloadStatus> = _status.asStateFlow()

    // 专门用于下载的 Client
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            // 设置无限超时，防止大文件下载中断
            requestTimeoutMillis = Long.MAX_VALUE 
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = Long.MAX_VALUE
        }
    }

    /**
     * 开始下载
     */
    suspend fun startDownload(config: DownloadConfig) {
        _status.value = DownloadStatus.Pending
        
        try {
            val file = File(config.savePath, config.fileName)
            // 确保目录存在
            file.parentFile?.mkdirs()

            // 1. 获取文件信息
            val headResponse = client.head(config.url)
            if (!headResponse.status.isSuccess()) {
                throw Exception("Failed to connect: ${headResponse.status}")
            }

            val totalLength = headResponse.contentLength() ?: -1L
            val acceptRanges = headResponse.headers[HttpHeaders.AcceptRanges]
            val supportRange = acceptRanges == "bytes" && totalLength > 0

            // 2. 预分配文件大小
            withContext(Dispatchers.IO) {
                RandomAccessFile(file, "rw").use { raf ->
                    if (totalLength > 0) raf.setLength(totalLength)
                }
            }

            // 3. 决定下载策略
            if (supportRange && config.threadCount > 1) {
                downloadMultiThreaded(config.url, file, totalLength, config.threadCount)
            } else {
                downloadSingleThreaded(config.url, file, totalLength)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            _status.value = DownloadStatus.Error(e.message ?: "Unknown error", e)
        }
    }

    /**
     * 多线程分块下载
     */
    private suspend fun downloadMultiThreaded(
        url: String, 
        file: File, 
        totalLength: Long, 
        threadCount: Int
    ) = coroutineScope {
        val chunkSize = totalLength / threadCount
        val chunks = ArrayList<Chunk>()
        
        // 计算分块
        for (i in 0 until threadCount) {
            val start = i * chunkSize
            val end = if (i == threadCount - 1) totalLength - 1 else (i + 1) * chunkSize - 1
            chunks.add(Chunk(i, start, end, start))
        }

        val downloadedTotal = AtomicLong(0)
        
        // 并发任务
        val tasks = chunks.map { chunk ->
            async(Dispatchers.IO) {
                downloadChunk(url, file, chunk) { bytesRead ->
                    val currentTotal = downloadedTotal.addAndGet(bytesRead.toLong())
                    updateProgress(currentTotal, totalLength)
                }
            }
        }

        // 等待所有分块完成
        tasks.forEach { it.await() }
        
        _status.value = DownloadStatus.Success(file)
    }

    /**
     * 下载单个分块
     */
    private suspend fun downloadChunk(
        url: String, 
        file: File, 
        chunk: Chunk,
        onBytesRead: (Int) -> Unit
    ) {
        Log.d("KtorDownloader", "Starting chunk ${chunk.id}: ${chunk.start}-${chunk.end}")

        val response = client.get(url) {
            header(HttpHeaders.Range, "bytes=${chunk.start}-${chunk.end}")
        }

        val channel: ByteReadChannel = response.body()
        // 使用 toInputStream 转换，这是处理 IO 最稳健的方式
        val inputStream = channel.toInputStream()
        val buffer = ByteArray(8192)
        
        withContext(Dispatchers.IO) {
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(chunk.start + (chunk.current - chunk.start))
                
                var bytesRead: Int
                // 循环读取直到流结束
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    raf.write(buffer, 0, bytesRead)
                    
                    onBytesRead(bytesRead)
                    chunk.current += bytesRead
                    
                    // 检查协程是否活跃，支持取消
                    if (!isActive) break
                }
            }
            inputStream.close()
        }
    }

    /**
     * 单线程下载（兜底方案）
     */
    private suspend fun downloadSingleThreaded(url: String, file: File, totalLength: Long) {
        val response = client.get(url) {
            onDownload { bytesSentTotal, contentLength ->
                 val total = contentLength ?: totalLength
                 if (total > 0) {
                     updateProgress(bytesSentTotal, total)
                 }
            }
        }
        
        val body: ByteArray = response.body()
        withContext(Dispatchers.IO) {
            file.writeBytes(body)
        }
        _status.value = DownloadStatus.Success(file)
    }

    private fun updateProgress(current: Long, total: Long) {
        if (total <= 0) return
        val progress = current.toFloat() / total.toFloat()
        
        val currentStatus = _status.value
        val lastProgress = if (currentStatus is DownloadStatus.Downloading) currentStatus.progress else 0f
        
        // 简单的限流更新，减少 StateFlow 发射频率
        if (currentStatus !is DownloadStatus.Downloading || 
           (progress - lastProgress) > 0.01f ||
           progress >= 1.0f) {
            
            _status.value = DownloadStatus.Downloading(
                progress = progress,
                downloadedBytes = current,
                totalBytes = total,
                speed = "" 
            )
        }
    }
    
    fun cancel() {
        _status.value = DownloadStatus.Idle
        // 实际取消需要配合 Job 管理，此处仅重置状态
    }
}