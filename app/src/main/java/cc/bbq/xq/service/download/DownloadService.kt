// 文件路径: cc/bbq/xq/service/download/DownloadService.kt
package cc.bbq.xq.service.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cc.bbq.xq.MainActivity
import cc.bbq.xq.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {

    private val binder = DownloadBinder()
    private val downloader = KtorDownloader()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 通知相关
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "download_channel"
    private lateinit var notificationManager: NotificationManager

    inner class DownloadBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // 监听下载状态并更新通知
        downloader.status.onEach { status ->
            updateNotification(status)
        }.launchIn(serviceScope)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * 对外暴露的下载方法
     */
    fun startDownload(url: String, fileName: String) {
        // 获取外部存储路径 (Android 10+ Scoped Storage 可能需要调整，这里假设使用应用私有目录或已获得权限)
        // 为了简单起见，我们下载到应用私有缓存目录，或者外部私有目录
        val saveDir = getExternalFilesDir(null)?.absolutePath ?: filesDir.absolutePath
        val config = DownloadConfig(
            url = url,
            savePath = "$saveDir/downloads",
            fileName = fileName
        )
        
        serviceScope.launch {
            downloader.startDownload(config)
        }
    }
    
    /**
     * 获取下载状态 Flow，供 UI 观察
     */
    fun getDownloadStatus() = downloader.status

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows file download progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(status: DownloadStatus) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.dsdownload)
            .setContentTitle("下载服务")
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        when (status) {
            is DownloadStatus.Idle -> {
                // 空闲时不显示通知，或者取消通知
                stopForeground(true)
                return
            }
            is DownloadStatus.Pending -> {
                builder.setContentText("准备下载...")
                    .setProgress(0, 0, true)
                startForeground(NOTIFICATION_ID, builder.build())
            }
            is DownloadStatus.Downloading -> {
                val progressInt = (status.progress * 100).toInt()
                builder.setContentText("下载中: $progressInt%")
                    .setProgress(100, progressInt, false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
            is DownloadStatus.Paused -> {
                builder.setContentText("已暂停")
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setOngoing(false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
            is DownloadStatus.Success -> {
                builder.setContentText("下载完成")
                    .setProgress(0, 0, false)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .setAutoCancel(true)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
                // 下载完成后停止前台服务，但保留通知
                stopForeground(false)
            }
            is DownloadStatus.Error -> {
                builder.setContentText("下载失败: ${status.message}")
                    .setProgress(0, 0, false)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setOngoing(false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
                stopForeground(false)
            }
        }
    }
}