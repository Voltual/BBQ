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
import kotlinx.coroutines.launch

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

    // 移除 switchStore 方法，因为我们只支持弦应用商店
    // 用户不能切换商店类型
}