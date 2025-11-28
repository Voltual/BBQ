// /app/src/main/java/cc/bbq/xq/ui/plaza/AppDetailComposeViewModel.kt
package cc.bbq.xq.ui.plaza

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.AuthManager
import cc.bbq.xq.data.repository.IAppStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import cc.bbq.xq.data.unified.*
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    private val repository: IAppStoreRepository
        get() = repositories[currentStore] ?: throw IllegalStateException("Repository not found")

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
    
    fun handleDownloadClick(context: Context) { // 传入 Context 用于跳转
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getAppDownloadSources(currentAppId, currentVersionId)
            _isLoading.value = false

            if (result.isSuccess) {
                val sources = result.getOrThrow()
                if (sources.isEmpty()) {
                    _errorMessage.value = "未找到下载源"
                } else if (sources.size == 1) {
                    // 只有一个源，直接下载
                    openUrl(context, sources.first().url)
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
    
    // 辅助方法：打开URL
    private fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            _errorMessage.value = "无法打开链接: $url"
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
                    _appDetail.value = detailResult.getOrThrow()
                    loadComments()
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
            val result = repository.getAppComments(currentAppId, 1)
            if (result.isSuccess) {
                _comments.value = result.getOrThrow().first
            }
        }
    }

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
            val result = repository.postComment(currentAppId, content, parentId, null)
            
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
}