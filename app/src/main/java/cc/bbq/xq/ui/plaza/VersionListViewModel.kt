// 文件路径: cc/bbq/xq/ui/plaza/VersionListViewModel.kt
package cc.bbq.xq.ui.plaza

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.data.repository.SineShopRepository
import cc.bbq.xq.data.unified.UnifiedAppItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class VersionListViewModel(
    application: Application,
    private val sineShopRepository: SineShopRepository
) : AndroidViewModel(application) {

    private val _versions = MutableStateFlow<List<UnifiedAppItem>>(emptyList())
    val versions: StateFlow<List<UnifiedAppItem>> = _versions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private var currentAppId: Int = 0

    fun initialize(appId: Int) {
        if (currentAppId != appId) {
            currentAppId = appId
            loadVersions(1)
        }
    }

    fun loadVersions(page: Int) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // 使用专用的 getAppVersionsByAppId 方法
                val result = sineShopRepository.getAppVersionsByAppId(currentAppId, page)
                
                if (result.isSuccess) {
                    val (items, totalPages) = result.getOrThrow()
                    _versions.value = items
                    _currentPage.value = page
                    _totalPages.value = totalPages
                } else {
                    _errorMessage.value = "加载版本列表失败: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        val nextPage = _currentPage.value + 1
        if (nextPage <= _totalPages.value) {
            loadVersions(nextPage)
        }
    }

    fun refresh() {
        loadVersions(1)
    }
}