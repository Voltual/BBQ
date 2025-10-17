//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import cc.bbq.xq.RetrofitClient

class CommunityViewModel : ViewModel() { // 移除接口实现
    private val _posts = MutableStateFlow(emptyList<RetrofitClient.models.Post>())
    val posts: StateFlow<List<RetrofitClient.models.Post>> = _posts.asStateFlow() // 移除 override
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow() // 移除 override
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow() // 移除 override
    // 添加下拉刷新状态
    private val _isRefreshing = MutableStateFlow(false)
    
    private var currentPage = 1
    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()
    
    // 新增跳页方法
        fun jumpToPage(page: Int) {
        if (page in 1..totalPages.value) {
            // 新增：跳页时清空当前列表
            _posts.value = emptyList()
            currentPage = page
            loadPosts()
        }
    }

    fun loadInitialData() { // 移除 override
        loadPosts()
    }

    fun loadNextPage() { // 移除 override
        if (currentPage < totalPages.value && !_isLoading.value) {
            currentPage++
            loadPosts()
        }
    }

    fun refresh() { // 移除 override
        currentPage = 1
        loadPosts()
    }

    private fun loadPosts() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val response = RetrofitClient.instance.getPostsList(
                    limit = PAGE_SIZE,
                    page = currentPage
                )
                
                if (response.isSuccessful && response.body()?.code == 1) {
                    response.body()?.data?.let { data ->
                        _totalPages.value = data.pagecount
                        val newPosts = if (currentPage == 1) {
                            data.list
                        } else {
                            _posts.value + data.list
                        }
                        
                        _posts.value = newPosts
                        _errorMessage.value = ""
                    }
                } else {
                    _errorMessage.value = "加载失败: ${response.body()?.msg ?: "未知错误"}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    companion object {
        private const val PAGE_SIZE = 10
    }
}