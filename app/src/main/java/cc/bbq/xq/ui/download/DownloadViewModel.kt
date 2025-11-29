// 文件路径: cc/bbq/xq/ui/download/DownloadViewModel.kt
package cc.bbq.xq.ui.download

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.service.download.DownloadService
import cc.bbq.xq.service.download.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private var downloadService: DownloadService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as DownloadService.DownloadBinder
            downloadService = binder.getService()
            isBound = true
            
            // 连上服务后，立即开始观察状态
            observeServiceStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService = null
            isBound = false
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        val intent = Intent(getApplication(), DownloadService::class.java)
        // 启动服务以确保它在后台运行（如果尚未运行）
        getApplication<Application>().startService(intent)
        // 绑定服务以进行通信
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeServiceStatus() {
        viewModelScope.launch {
            downloadService?.getDownloadStatus()?.collect { status ->
                _downloadStatus.value = status
            }
        }
    }

    /**
     * 取消当前下载
     */
    fun cancelDownload() {
        // 这里假设 Service 或 Downloader 有 cancel 方法
        // 目前 KtorDownloader.cancel() 只是重置状态，实际逻辑需完善 Job 管理
        // downloadService?.cancelCurrentTask() 
        // 暂时模拟重置状态
        _downloadStatus.value = DownloadStatus.Idle
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}