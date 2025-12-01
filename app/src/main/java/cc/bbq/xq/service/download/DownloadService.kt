// 文件路径: cc/bbq/xq/service/download/DownloadService.kt
package cc.bbq.xq.service.download

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cc.bbq.xq.MainActivity
import cc.bbq.xq.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.util.Log  // 添加这行
import java.io.File

/**
 * 下载服务 - 整合新版KtorDownloader
 */
class DownloadService : Service() {

    companion object {
        private const val TAG = "DownloadService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "download_channel"
        
        // Action常量
        const val ACTION_START_DOWNLOAD = "cc.bbq.xq.action.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "cc.bbq.xq.action.CANCEL_DOWNLOAD"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILE_NAME = "extra_file_name"
        const val EXTRA_SAVE_PATH = "extra_save_path"
    }

    private val binder = DownloadBinder()
    private lateinit var downloader: KtorDownloader
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    // 通知相关
    private lateinit var notificationManager: NotificationManager
    private var currentDownloadConfig: DownloadConfig? = null

    inner class DownloadBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
        
        /**
         * 获取下载状态Flow
         */
        fun getDownloadStatus(): StateFlow<DownloadStatus> = downloader.status
        
        /**
         * 开始下载（编程方式）
         */
        fun startDownload(url: String, fileName: String) {
            this@DownloadService.startDownload(url, fileName)
        }
        
        /**
         * 取消当前下载
         */
        fun cancelDownload() {
            downloader.cancel()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DownloadService created")
        
        downloader = KtorDownloader()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // 监听下载状态并更新通知
        setupStatusObserver()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        intent?.let { handleIntent(it) }
        
        // 如果不是通过intent启动，保持服务运行
        return START_STICKY
    }
    
    /**
     * 处理Intent命令
     */
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "download_file"
                val savePath = intent.getStringExtra(EXTRA_SAVE_PATH)
                
                startDownload(url, fileName, savePath)
            }
            
            ACTION_CANCEL_DOWNLOAD -> {
                downloader.cancel()
            }
            
            // 从AppDetailComposeViewModel调用的方式
            else -> {
                val url = intent.getStringExtra("url")
                val fileName = intent.getStringExtra("fileName")
                if (url != null && fileName != null) {
                    startDownload(url, fileName)
                }
            }
        }
    }

    /**
     * 开始下载（主方法）
     */
    fun startDownload(url: String, fileName: String, customSavePath: String? = null) {
        Log.d(TAG, "Starting download: $fileName from $url")
        
        // 确保不会重复下载
        if (downloader.status.value is DownloadStatus.Downloading ||
            downloader.status.value is DownloadStatus.Pending) {
            Log.w(TAG, "Download already in progress")
            return
        }
        
        // 构建下载配置
        val savePath = customSavePath ?: getDefaultDownloadPath()
        val config = DownloadConfig(
            url = url,
            savePath = savePath,
            fileName = fileName,
            threadCount = determineThreadCount(url) // 根据文件类型决定线程数
        )
        
        currentDownloadConfig = config
        
        serviceScope.launch {
            downloader.startDownload(config)
        }
        
        // 更新通知显示下载开始
        updateNotification(DownloadStatus.Pending, fileName)
    }
    
    /**
     * 获取默认下载路径
     */
    private fun getDefaultDownloadPath(): String {
        return getExternalFilesDir(null)?.absolutePath 
            ?: filesDir.absolutePath + "/downloads"
    }
    
    /**
     * 根据URL决定线程数（大文件多线程，小文件单线程）
     */
    private fun determineThreadCount(url: String): Int {
        // 这里可以根据文件扩展名或其他逻辑判断
        // 暂时简单处理：总是用3线程（适中）
        return 3
    }

    /**
     * 设置状态观察器
     */
    private fun setupStatusObserver() {
        downloader.status
            .onEach { status ->
                Log.d(TAG, "Download status changed: $status")
                val fileName = currentDownloadConfig?.fileName ?: "文件"
                updateNotification(status, fileName)
                
                // 下载完成或出错时清理
                when (status) {
                    is DownloadStatus.Success, is DownloadStatus.Error -> {
                        // 延迟清理，让用户看到最终状态
                        serviceScope.launch {
                            delay(5000)
                            if (downloader.status.value is DownloadStatus.Idle ||
                                downloader.status.value is DownloadStatus.Error ||
                                downloader.status.value is DownloadStatus.Success) {
                                stopForeground(false)
                            }
                        }
                    }
                    else -> {
                        // 下载中，保持前台服务
                        if (status is DownloadStatus.Downloading || status is DownloadStatus.Pending) {
                            ensureForegroundService(fileName)
                        }
                    }
                }
            }
            .launchIn(serviceScope)
    }

    /**
     * 确保服务在前台运行
     */
    private fun ensureForegroundService(fileName: String) {
        val status = downloader.status.value
        if (status is DownloadStatus.Downloading || status is DownloadStatus.Pending) {
            val notification = buildNotification(status, fileName)
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * 创建通知渠道（Android O+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "文件下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示文件下载进度"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 更新通知
     */
    private fun updateNotification(status: DownloadStatus, fileName: String) {
        val notification = buildNotification(status, fileName)
        
        when (status) {
            is DownloadStatus.Downloading, is DownloadStatus.Pending -> {
                startForeground(NOTIFICATION_ID, notification)
            }
            is DownloadStatus.Success, is DownloadStatus.Error -> {
                // 下载完成或出错，停止前台但保留通知
                notificationManager.notify(NOTIFICATION_ID, notification)
                stopForeground(false)
            }
            is DownloadStatus.Idle -> {
                // 空闲状态，完全停止前台
                stopForeground(true)
                notificationManager.cancel(NOTIFICATION_ID)
            }
            else -> {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    /**
     * 构建通知
     */
    private fun buildNotification(status: DownloadStatus, fileName: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_fragment", "download") // 跳转到下载管理页
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.dsdownload)
            .setContentTitle("BBQ下载")
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)

        // 添加取消操作按钮
        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        builder.addAction(
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "取消",
                cancelPendingIntent
            ).build()
        )

        when (status) {
            is DownloadStatus.Pending -> {
                builder.setContentText("准备下载: $fileName")
                    .setProgress(0, 0, true)
            }
            
            is DownloadStatus.Downloading -> {
                val progressPercent = (status.progress * 100).toInt()
                val speedText = if (status.speed.isNotEmpty()) " | ${status.speed}" else ""
                
                builder.setContentText("下载中: $fileName")
                    .setContentInfo("$progressPercent%$speedText")
                    .setProgress(100, progressPercent, false)
                    .setSubText(formatFileSize(status.downloadedBytes) + 
                               " / " + formatFileSize(status.totalBytes))
            }
            
            is DownloadStatus.Paused -> {
                builder.setContentText("已暂停: $fileName")
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setOngoing(false)
                    .setProgress(0, 0, false)
            }
            
            is DownloadStatus.Success -> {
                builder.setContentText("下载完成: $fileName")
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setProgress(0, 0, false)
            }
            
            is DownloadStatus.Error -> {
                builder.setContentText("下载失败: ${status.message}")
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setProgress(0, 0, false)
            }
            
            is DownloadStatus.Idle -> {
                builder.setContentText("下载服务就绪")
                    .setOngoing(false)
                    .setProgress(0, 0, false)
            }
        }

        return builder.build()
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "DownloadService destroyed")
        
        // 清理资源
        downloader.close()
        serviceScope.cancel()
        
        super.onDestroy()
    }
    
    /**
     * 获取当前下载状态（兼容ViewModel）
     */
    fun getDownloadStatus(): StateFlow<DownloadStatus> {
        return downloader.status
    }
}