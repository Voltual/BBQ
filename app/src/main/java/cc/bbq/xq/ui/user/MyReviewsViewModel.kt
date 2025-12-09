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
class MyReviewsViewModel(
    application: Application,
    private val repositories: Map<AppStore, IAppStoreRepository>
) : AndroidViewModel(application) {

    private val _reviews = MutableStateFlow<List<UnifiedComment>>(emptyList())
    val reviews: StateFlow<List<UnifiedComment>> = _reviews.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentPage = 1
    private var isLoadingMore = false
    private var hasMore = true

    // 硬编码为弦应用商店，因为只有它支持我的评价功能
    private val _selectedStore = MutableStateFlow(AppStore.SIENE_SHOP)
    val selectedStore: StateFlow<AppStore> = _selectedStore.asStateFlow()

    // 新增：评价删除确认对话框状态
    private val _showDeleteReviewDialog = MutableStateFlow<String?>(null)
    val showDeleteReviewDialog: StateFlow<String?> = _showDeleteReviewDialog.asStateFlow()

    private fun getRepository(): IAppStoreRepository {
        return repositories[selectedStore.value] ?: 
            throw IllegalStateException("Repository not found for ${selectedStore.value}")
    }

    fun initialize() {
        loadReviews()
    }

    // 在 MyReviewsViewModel.kt 中，修正 loadReviews 方法
private fun loadReviews() {
    if (isLoading.value || isLoadingMore) return

    _isLoading.value = true
    _error.value = null

    viewModelScope.launch {
        try {
            // 修正：调用 getMyReviews 方法（返回的是 Pair<List<UnifiedComment>, Int>）
            val result = getRepository().getMyReviews(currentPage)//这是61行
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null) {
                    val (newReviews, totalPages) = data // 修正：data 是 Pair 类型//这是65行
                    if (currentPage == 1) {
                        _reviews.value = newReviews
                    } else {
                        _reviews.value = _reviews.value + newReviews
                    }
                    hasMore = currentPage < totalPages
                }
            } else {
                _error.value = "加载评价失败: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "加载评价失败: ${e.message}"
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
        loadReviews()
    }

    fun refresh() {
        currentPage = 1
        hasMore = true
        loadReviews()
    }

    // 新增：显示删除评价确认对话框
    fun showDeleteReviewDialog(reviewId: String) {
        _showDeleteReviewDialog.value = reviewId
    }

    // 新增：隐藏删除评价确认对话框
    fun hideDeleteReviewDialog() {
        _showDeleteReviewDialog.value = null
    }

    // 新增：删除评价
    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            try {
                val result = getRepository().deleteReview(reviewId)
                if (result.isSuccess) {
                    // 从列表中移除已删除的评价
                    _reviews.value = _reviews.value.filter { it.id != reviewId }
                    _error.value = null
                } else {
                    _error.value = "删除评价失败: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "删除评价失败: ${e.message}"
            } finally {
                hideDeleteReviewDialog()
            }
        }
    }
}