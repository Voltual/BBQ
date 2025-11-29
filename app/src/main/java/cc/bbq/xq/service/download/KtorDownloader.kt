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
import io.ktor.client.request.url
import io.ktor.client.statement.HttpHeaders
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentRange
import io.ktor.http.HttpHeaders.ContentLength
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

class KtorDownloader {

    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status: StateFlow<DownloadStatus> = _status.asStateFlow()

    private var downloadJob: Job? = null
    
    // 专门用于下载的 Client，超时时间设置较长
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
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
        // 使用 RandomAccessFile 进行随机写入
        // 注意：RandomAccessFile 不是线程安全的，但如果是向不同的 offset 写入，
        // 且每个线程有自己的 RandomAccessFile 实例指向同一个文件，这在 OS 层面通常是安全的。
        // 为保险起见，这里我们在 use 块内操作。
        
        Log.d("KtorDownloader", "Starting chunk ${chunk.id}: ${chunk.start}-${chunk.end}")

        val response = client.get(url) {
            header(HttpHeaders.Range, "bytes=${chunk.start}-${chunk.end}")
        }

        val channel: ByteReadChannel = response.body()
        val bufferSize = 8192
        val buffer = ByteBuffer.allocate(bufferSize)
        
        withContext(Dispatchers.IO) {
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(chunk.start)
                
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer)
                    if (read == -1) break
                    
                    buffer.flip() // 切换到读模式
                    // 将 ByteBuffer 写入文件
                    raf.write(buffer.array(), buffer.position(), buffer.remaining())
                    
                    onBytesRead(read)
                    chunk.current += read
                    
                    buffer.clear() // 清空 buffer 供下次使用
                }
            }
        }
    }

    /**
     * 单线程下载（兜底方案）
     */
    private suspend fun downloadSingleThreaded(url: String, file: File, totalLength: Long) {
        val response = client.get(url) {
            onDownload { bytesSentTotal, contentLength ->
                 updateProgress(bytesSentTotal, contentLength)
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
        // 简单的限流更新，避免 StateFlow 更新太频繁（实际生产中可加 debounce）
        if (_status.value !is DownloadStatus.Downloading || 
           (progress - (_status.value as? DownloadStatus.Downloading)?.progress ?: 0f) > 0.01f ||
           progress >= 1.0f) {
            
            _status.value = DownloadStatus.Downloading(
                progress = progress,
                downloadedBytes = current,
                totalBytes = total,
                speed = "Calculating..." // 速度计算需要额外的时间戳逻辑，暂时留空
            )
        }
    }
    
    fun cancel() {
        // 实现取消逻辑
    }
}