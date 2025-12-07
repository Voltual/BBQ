// 文件路径: cc/bbq.xq.ui.plaza.AppDetailComposeViewModel.kt
package cc.bbq.xq.ui.plaza

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.data.repository.IAppStoreRepository
import cc.bbq.xq.data.unified.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import cc.bbq.xq.service.download.DownloadService

@KoinViewModel
class AppDetailComposeViewModel(
    application: Application,
    private val repositories: Map<AppStore, IAppStoreRepository>
) : AndroidViewModel(application) {

    private val _appDetail = MutableStateFlow<UnifiedAppDetail?>(null)
    val appDetail: StateFlow<UnifiedAppDetail?> = _appDetail.asStateFlow()

    private val _comments = MutableStateFlow<List<UnifiedComment>>(emptyList())
    val comments: StateFlow<List<UnifiedComment>> = _comments.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _showCommentDialog = MutableStateFlow(false)
    val showCommentDialog: StateFlow<Boolean> = _showCommentDialog.asStateFlow()

    private val _showReplyDialog = MutableStateFlow(false)
    val showReplyDialog: StateFlow<Boolean> = _showReplyDialog.asStateFlow()

    private val _currentReplyComment = MutableStateFlow<UnifiedComment?>(null)
    val currentReplyComment: StateFlow<UnifiedComment?> = _currentReplyComment.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentStore: AppStore = AppStore.XIAOQU_SPACE
    private var currentAppId: String = ""
    private var currentVersionId: Long = 0L

    private val _downloadSources = MutableStateFlow<List<UnifiedDownloadSource>>(emptyList())
    val downloadSources: StateFlow<List<UnifiedDownloadSource>> = _downloadSources.asStateFlow()

    private val _showDownloadDrawer = MutableStateFlow(false)
    val showDownloadDrawer: StateFlow<Boolean> = _showDownloadDrawer.asStateFlow()

    // 新增：Snackbar 事件
    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()
    
    // 新增：导航事件
    private val _navigateToDownloadEvent = MutableSharedFlow<Boolean>()
    val navigateToDownloadEvent: SharedFlow<Boolean> = _navigateToDownloadEvent.asSharedFlow()

    // 新增：用于发送一次性事件（如打开浏览器）
    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    // 移除：版本列表相关状态
    //private val _versions = MutableStateFlow<List<UnifiedAppItem>>(emptyList())
    //val versions: StateFlow<List<UnifiedAppItem>> = _versions.asStateFlow()

    //private val _isVersionListLoading = MutableStateFlow(false)
    //val isVersionListLoading: StateFlow<Boolean> = _isVersionListLoading.asStateFlow()

    //private val _versionListError = MutableStateFlow<String?>(null)
    //val versionListError: StateFlow<String?> = _versionListError.asStateFlow()

    private val _currentTab = MutableStateFlow(0) // 0: 详情, 1: 版本列表
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    //private val _showVersionList = MutableStateFlow(false)
    //val showVersionList: StateFlow<Boolean> = _showVersionList.asStateFlow()

    private val repository: IAppStoreRepository
        get() = repositories[currentStore] ?: throw IllegalStateException("Repository not found")

    // 新增：获取设备SDK版本
    val deviceSdkVersion: Int
        get() = Build.VERSION.SDK_INT

    fun initializeData(appId: String, versionId: Long, storeName: String) {
        val store = try {
            AppStore.valueOf(storeName)
        } catch (e: Exception) {
            AppStore.XIAOQU_SPACE
        }

        if (currentAppId == appId && currentVersionId == versionId && currentStore == store && _appDetail.value != null) {
            return
        }

        currentAppId = appId
        currentVersionId = versionId
        currentStore = store

        loadData()
    }

    fun refresh() {
        loadData()
    }

    fun handleDownloadClick() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getAppDownloadSources(currentAppId, currentVersionId)
            _isLoading.value = false

            if (result.isSuccess) {
                val sources = result.getOrThrow()
                if (sources.isEmpty()) {
                    _errorMessage.value = "未找到下载源"
                } else if (sources.size == 1) {
                    // 只有一个源，直接触发下载
                    startDownload(sources.first().url)
                } else {
                    // 多个源，显示抽屉
                    _downloadSources.value = sources
                    _showDownloadDrawer.value = true
                }
            } else {
                _errorMessage.value = "获取下载链接失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun closeDownloadDrawer() {
        _showDownloadDrawer.value = false
    }

    private fun loadData() {
        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                val detailResult = repository.getAppDetail(currentAppId, currentVersionId)

                if (detailResult.isSuccess) {
                    var detail = detailResult.getOrThrow()
                    
                    // 如果是弦应用商店，需要在 raw 数据中添加设备SDK信息
                    if (currentStore == AppStore.SIENE_SHOP && detail.raw is cc.bbq.xq.SineShopClient.SineShopAppDetail) {
                        val raw = detail.raw as cc.bbq.xq.SineShopClient.SineShopAppDetail
                        // 这里我们不需要修改 raw 对象，因为在 Composable 中会动态计算
                    }
                    
                    _appDetail.value = detail
                    loadComments()
                    
                    // 移除：不再在此处加载版本列表
                    //if (currentStore == AppStore.SIENE_SHOP) {
                    //    loadVersionList()
                    //}
                } else {
                    _errorMessage.value = "加载详情失败: ${detailResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadComments() {
        viewModelScope.launch {
            // 传入 currentVersionId
            val result = repository.getAppComments(currentAppId, currentVersionId, 1)
            if (result.isSuccess) {
                _comments.value = result.getOrThrow().first
            }
        }
    }

    // 移除：加载版本列表
    //private fun loadVersionList() { ... }

    fun openCommentDialog() {
        _showCommentDialog.value = true
        _currentReplyComment.value = null
    }

    fun closeCommentDialog() {
        _showCommentDialog.value = false
    }

    fun openReplyDialog(comment: UnifiedComment) {
        _currentReplyComment.value = comment
        _showReplyDialog.value = true
    }

    fun closeReplyDialog() {
        _showReplyDialog.value = false
        _currentReplyComment.value = null
    }

    fun submitComment(content: String) {
        viewModelScope.launch {
            val parentId = _currentReplyComment.value?.id
            // 修正：传递 currentVersionId
            val result = repository.postComment(currentAppId, currentVersionId, content, parentId, null)

            if (result.isSuccess) {
                loadComments()
                if (parentId == null) closeCommentDialog() else closeReplyDialog()
            } else {
                _errorMessage.value = "提交失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            val result = repository.deleteComment(commentId)
            if (result.isSuccess) {
                loadComments()
            } else {
                _errorMessage.value = "删除失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteApp(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteApp(currentAppId, currentVersionId)
            if (result.isSuccess) {
                onSuccess()
            } else {
                _errorMessage.value = "删除应用失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }
    
    // 新增：启动下载
    private fun startDownload(downloadUrl: String) {
        viewModelScope.launch {
            try {
                // 触发 Service 开始下载
                val appName = _appDetail.value?.name ?: "未命名应用"
                getApplication<Application>().startDownload(downloadUrl, appName)
                
                // 发送 Snackbar 事件
                _snackbarEvent.emit("开始下载: $appName")
                
                // 发送导航到下载管理界面的事件
                _navigateToDownloadEvent.emit(true)
                
            } catch (e: Exception) {
                _errorMessage.value = "启动下载失败: ${e.message}"
            }
        }
    }

    // 扩展函数：启动下载服务
    private fun Application.startDownload(downloadUrl: String, fileName: String) {
        val intent = Intent(this, cc.bbq.xq.service.download.DownloadService::class.java)
        intent.putExtra("url", downloadUrl)
        intent.putExtra("fileName", fileName)
        startService(intent)
    }
}