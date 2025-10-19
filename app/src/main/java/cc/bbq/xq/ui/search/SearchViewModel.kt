//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.


package cc.bbq.xq.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.BBQApplication
import cc.bbq.xq.KtorClient // 导入 KtorClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SearchMode {
    POSTS,
    HISTORY,
    LOGS
}

sealed class SearchResultItem {
    // 修改为 KtorClient.Post
    data class PostItem(val post: KtorClient.Post) : SearchResultItem()
    data class HistoryItem(val history: cc.bbq.xq.ui.community.BrowseHistory) : SearchResultItem()
    data class LogItem(val log: cc.bbq.xq.data.db.LogEntry) : SearchResultItem()
}

class SearchViewModel : ViewModel() {
    private val searchHistoryDataStore = BBQApplication.instance.searchHistoryDataStore
    private val browseHistoryDao = BBQApplication.instance.database.browseHistoryDao()
    private val logDao = BBQApplication.instance.database.logDao()
    //private val apiService = RetrofitClient.instance // 移除 RetrofitClient

    private val _searchMode = MutableStateFlow(SearchMode.POSTS)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    // 新增分页相关状态
    private var currentPage = 1
    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()
    
    private val _hasMoreData = MutableStateFlow(true)
    val hasMoreData: StateFlow<Boolean> = _hasMoreData.asStateFlow()

    val searchHistory: StateFlow<List<String>> = searchHistoryDataStore.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onSearchModeChange(newMode: SearchMode) {
        _searchMode.value = newMode
        // 切换模式时重置分页状态
        resetPagination()
        if (_query.value.isNotBlank()) {
            performSearch()
        }
    }

    fun submitSearch(submittedQuery: String) {
        val query = submittedQuery.ifBlank { _query.value }
        if (query.isBlank()) return

        onQueryChange(query)
        viewModelScope.launch {
            searchHistoryDataStore.addSearchQuery(query)
        }
        // 重置分页状态并执行搜索
        resetPagination()
        performSearch()
    }

    // 新增：加载更多数据
    fun loadMore() {
        // 修复：添加更严格的检查条件
        if (!_isLoading.value && _hasMoreData.value && _query.value.isNotBlank() && _searchMode.value == SearchMode.POSTS) {
            currentPage++
            performSearch(loadMore = true)
        }
    }

    // 新增：跳转到指定页面
    fun jumpToPage(page: Int) {
        if (page in 1.._totalPages.value && !_isLoading.value) {
            currentPage = page
            _searchResults.value = emptyList() // 清空当前结果
            performSearch()
        }
    }

    // 新增：重置分页状态
    private fun resetPagination() {
        currentPage = 1
        _searchResults.value = emptyList()
        _totalPages.value = 1
        _hasMoreData.value = true
        _errorMessage.value = null
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryDataStore.clearAllHistory()
        }
    }

    private fun performSearch(loadMore: Boolean = false) {
        // 修复：防止重复搜索
        if (_isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                when (_searchMode.value) {
                    SearchMode.POSTS -> searchPosts(loadMore)
                    SearchMode.HISTORY -> searchLocalHistory()
                    SearchMode.LOGS -> searchLocalLogs()
                }
            } catch (e: Exception) {
                _errorMessage.value = "搜索时发生错误: ${e.message}"
                // 加载失败时回退页码
                if (loadMore) {
                    currentPage--
                    _hasMoreData.value = true // 重置为true以便重试
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun searchPosts(loadMore: Boolean = false) {
        val response = KtorClient.ApiServiceImpl.searchPosts(
            query = _query.value, 
            page = currentPage, 
            limit = PAGE_SIZE
        )
        
        response.onSuccess { result ->
            result.data.let { data ->
                _totalPages.value = data.pagecount
                _hasMoreData.value = currentPage < data.pagecount
                
                val newPosts = data.list.map { SearchResultItem.PostItem(it) }
                _searchResults.value = if (loadMore && _searchResults.value.isNotEmpty()) {
                    // 修复：确保正确合并结果
                    _searchResults.value + newPosts
                } else {
                    newPosts
                }
                _errorMessage.value = null
            }
        }.onFailure { error ->
            val errorMsg = "帖子搜索失败: ${error.message ?: "未知错误"}"
            _errorMessage.value = errorMsg
            if (loadMore) currentPage-- // 回退页码
        }
    }

    private suspend fun searchLocalHistory() {
        // 本地搜索不分页，一次性加载所有
        val historyList = browseHistoryDao.searchHistory("%${_query.value}%").first()
        _searchResults.value = historyList.map { SearchResultItem.HistoryItem(it) }
        _hasMoreData.value = false
        _totalPages.value = 1
        _errorMessage.value = null
    }

    private suspend fun searchLocalLogs() {
        // 本地搜索不分页，一次性加载所有
        val logList = logDao.searchLogs("%${_query.value}%").first()
        _searchResults.value = logList.map { SearchResultItem.LogItem(it) }
        _hasMoreData.value = false
        _totalPages.value = 1
        _errorMessage.value = null
    }

    companion object {
        private const val PAGE_SIZE = 20 // 每页显示数量
    }
}