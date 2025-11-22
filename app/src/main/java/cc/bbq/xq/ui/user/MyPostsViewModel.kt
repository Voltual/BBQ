//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
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
    private val _posts = MutableStateFlow<List<KtorClient.Post>>(emptyList())
    val posts: StateFlow<List<KtorClient.Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private var currentPage = 1
    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    // 新增：用户ID状态
    private val _userId = MutableStateFlow<Long?>(null)

    // 暴露用户ID状态
    val userId: StateFlow<Long?> = _userId.asStateFlow()

    // 设置用户ID
    fun setUserId(userId: Long) {
        _userId.value = userId
        refresh() // 当用户ID改变时刷新数据
    }

    fun jumpToPage(page: Int) {
        if (page in 1..totalPages.value) {
            _posts.value = emptyList()
            currentPage = page
            loadMyPosts()
        }
    }

    fun loadNextPage() {
        if (currentPage < totalPages.value && !_isLoading.value) {
            currentPage++
            loadMyPosts()
        }
    }

    fun refresh() {
        currentPage = 1
        loadMyPosts()
    }

    private fun loadMyPosts() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""

            try {
                val userId = _userId.value ?: return@launch // 如果没有用户ID，则不加载数据

                // 使用 getPostsList 方法，并传递 userId 参数
                val myPostsResult = KtorClient.ApiServiceImpl.getPostsList(
                    limit = PAGE_SIZE,
                    page = currentPage,
                    userId = userId
                    // 其他参数使用默认值
                )

                if (myPostsResult.isSuccess) {
                    myPostsResult.getOrNull()?.let { response ->
                        if (response.code == 1) {
                            // 修复：直接访问 data，因为它是非空类型
                            val data = response.data
                            _totalPages.value = data.pagecount
                            val newPosts = if (currentPage == 1) {
                                data.list
                            } else {
                                _posts.value + data.list
                            }

                            // 数据去重
                            val distinctPosts = newPosts.distinctBy { it.postid }

                            _posts.value = distinctPosts
                            _errorMessage.value = ""
                        } else {
                            // 修复：正确处理非空字符串
                            _errorMessage.value = "操作失败: ${if (response.msg.isNotEmpty()) response.msg else "服务器错误"}"
                        }
                    } ?: run {
                        _errorMessage.value = "加载失败: 响应为空"
                    }
                } else {
                    val exceptionMessage = myPostsResult.exceptionOrNull()?.message
                    _errorMessage.value = "加载失败: ${exceptionMessage ?: "未知错误"}"
                }
            } catch (e: IOException) {
                _errorMessage.value = "网络异常: ${e.message ?: "未知错误"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}