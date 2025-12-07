package cc.bbq.xq.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.data.repository.IAppStoreRepository
import cc.bbq.xq.data.unified.UnifiedComment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.annotation.KoinViewModel
import kotlinx.coroutines.launch

@KoinViewModel
class MyCommentsViewModel(
    application: Application,
    private val repositories: Map<AppStore, IAppStoreRepository>
) : AndroidViewModel(application) {

    private val _comments = MutableStateFlow<List<UnifiedComment>>(emptyList())
    val comments: StateFlow<List<UnifiedComment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentPage = 1
    private var isLoadingMore = false
    private var hasMore = true

    // 硬编码为弦应用商店，因为只有它支持我的评论功能
    private val _selectedStore = MutableStateFlow(AppStore.SIENE_SHOP)
    val selectedStore: StateFlow<AppStore> = _selectedStore.asStateFlow()

    // 新增：评论删除确认对话框状态
    private val _showDeleteCommentDialog = MutableStateFlow<String?>(null)
    val showDeleteCommentDialog: StateFlow<String?> = _showDeleteCommentDialog.asStateFlow()

    private fun getRepository(): IAppStoreRepository {
        return repositories[selectedStore.value] ?: 
            throw IllegalStateException("Repository not found for ${selectedStore.value}")
    }

    fun initialize() {
        loadComments()
    }

    private fun loadComments() {
        if (isLoading.value || isLoadingMore) return

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = getRepository().getMyComments(currentPage)
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    if (data != null) {
                        val (newComments, totalPages) = data
                        if (currentPage == 1) {
                            _comments.value = newComments
                        } else {
                            _comments.value = _comments.value + newComments
                        }
                        hasMore = currentPage < totalPages
                    }
                } else {
                    _error.value = "加载评论失败: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "加载评论失败: ${e.message}"
            } finally {
                _isLoading.value = false
                isLoadingMore = false
            }
        }
    }

    fun loadMore() {
        if (!hasMore || isLoading.value || isLoadingMore) return
        
        isLoadingMore = true
        currentPage++
        loadComments()
    }

    fun refresh() {
        currentPage = 1
        hasMore = true
        loadComments()
    }

    // 新增：显示删除评论确认对话框
    fun showDeleteCommentDialog(commentId: String) {
        _showDeleteCommentDialog.value = commentId
    }

    // 新增：隐藏删除评论确认对话框
    fun hideDeleteCommentDialog() {
        _showDeleteCommentDialog.value = null
    }

    // 新增：删除评论
    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                val result = getRepository().deleteComment(commentId)
                if (result.isSuccess) {
                    // 从列表中移除已删除的评论
                    _comments.value = _comments.value.filter { it.id != commentId }
                    _error.value = null
                } else {
                    _error.value = "删除评论失败: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "删除评论失败: ${e.message}"
            } finally {
                hideDeleteCommentDialog()
            }
        }
    }
}