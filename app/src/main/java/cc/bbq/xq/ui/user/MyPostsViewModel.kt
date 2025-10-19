//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import cc.bbq.xq.KtorClient
import java.io.IOException

class MyPostsViewModel : ViewModel() {
    private val _posts = MutableStateFlow(emptyList<KtorClient.Post>())
    val posts: StateFlow<List<KtorClient.Post>> = _posts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    private var currentPage = 1
    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()
    // 添加下拉刷新状态
    private val _isRefreshing = MutableStateFlow(false)
    private var userId: Long = -1L
    
    // 添加状态跟踪
    private var _isInitialized = false
    private var _currentUserId: Long = -1L
    
    fun setUserId(userId: Long) {
        // 只有当用户ID真正改变时才重置状态
        if (this._currentUserId != userId) {
            this.userId = userId
            this._currentUserId = userId
            this._isInitialized = false
            resetState()
            loadDataIfNeeded()
        }
    }
    
    private fun resetState() {
        _posts.value = emptyList()
        currentPage = 1
        _totalPages.value = 1
        _errorMessage.value = ""
    }
    
    // 内部方法：只在需要时加载数据
    private fun loadDataIfNeeded() {
        if (!_isInitialized && userId != -1L && !_isLoading.value) {
            _isInitialized = true
            loadData()
        }
    }
    
    fun jumpToPage(page: Int) {
        if (page in 1.._totalPages.value) {
            _posts.value = emptyList()
            currentPage = page
            loadData()
        }
    }

    fun loadInitialData() {
        // 这个方法现在只是确保数据已加载
        loadDataIfNeeded()
    }

    fun loadNextPage() {
        if (currentPage < totalPages.value && !_isLoading.value) {
            currentPage++
            loadData()
        }
    }

    fun refresh() {
        // 刷新时重置页面但不重置初始化状态
        currentPage = 1
        loadData()
    }

    private fun loadData() {
        if (_isLoading.value || userId == -1L) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""
            
            try {
                val postsResult = KtorClient.ApiServiceImpl.getPostsList(
                    limit = PAGE_SIZE,
                    page = currentPage,
                    userId = userId
                )
                
                if (postsResult.isSuccess) {
                    postsResult.getOrNull()?.let { postsResponse ->
                        if (postsResponse.code == 1) {
                            postsResponse.data.let { data ->
                                _totalPages.value = data.pagecount
                                val newPosts = if (currentPage == 1) {
                                    data.list
                                } else {
                                    _posts.value + data.list
                                }
                                
                                _posts.value = newPosts
                            }
                        } else {
                            _errorMessage.value = "加载失败: ${postsResponse.msg}"
                        }
                    }
                } else {
                    _errorMessage.value = "加载失败: ${postsResult.exceptionOrNull()?.message ?: "未知错误"}"
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