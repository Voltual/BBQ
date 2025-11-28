// /app/src/main/java/cc/bbq/xq/ui/plaza/AppDetailComposeViewModel.kt
package cc.bbq.xq.ui.plaza

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.AuthManager
import cc.bbq.xq.data.repository.IAppStoreRepository
import cc.bbq.xq.data.unified.UnifiedAppDetail
import cc.bbq.xq.data.unified.UnifiedComment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private fun loadData() {
        _isLoading.value = true
        _errorMessage.value = ""
        
        viewModelScope.launch {
            try {
                // 并行加载详情和评论
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
            val result = repository.getAppComments(currentAppId, 1) // 暂时只加载第一页
            if (result.isSuccess) {
                _comments.value = result.getOrThrow().first
            } else {
                // 评论加载失败不影响详情显示，只显示Toast或Log
                // _errorMessage.value = "加载评论失败" 
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
            // 简单处理：如果是回复，暂时不处理 @mention，因为 UnifiedComment 还没完全统一 mention 逻辑
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
