//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.community

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AuthManager
import cc.bbq.xq.RetrofitClient
import cc.bbq.xq.data.db.BrowseHistoryRepository // 核心修改 #1: 导入新的 Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class PostDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _postDetail = MutableStateFlow<RetrofitClient.models.PostDetail?>(null)
    val postDetail: StateFlow<RetrofitClient.models.PostDetail?> = _postDetail.asStateFlow()

    private val _comments = MutableStateFlow<List<RetrofitClient.models.Comment>>(emptyList())
    val comments: StateFlow<List<RetrofitClient.models.Comment>> = _comments.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

    private val _deleteCommentSuccess = MutableStateFlow(false)
    val deleteCommentSuccess: StateFlow<Boolean> = _deleteCommentSuccess.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _likeCount = MutableStateFlow(0)
    val likeCount: StateFlow<Int> = _likeCount.asStateFlow()

    private val _commentCount = MutableStateFlow(0)
    val commentCount: StateFlow<Int> = _commentCount.asStateFlow()

    private val _showCommentDialog = MutableStateFlow(false)
    val showCommentDialog: StateFlow<Boolean> = _showCommentDialog.asStateFlow()

    private val _showReplyDialog = MutableStateFlow(false)
    val showReplyDialog: StateFlow<Boolean> = _showReplyDialog.asStateFlow()

    private val _currentReplyComment = MutableStateFlow<RetrofitClient.models.Comment?>(null)
    val currentReplyComment: StateFlow<RetrofitClient.models.Comment?> = _currentReplyComment.asStateFlow()

    private val _showMoreMenu = MutableStateFlow(false)
    val showMoreMenu: StateFlow<Boolean> = _showMoreMenu.asStateFlow()

    private val _currentCommentPage = MutableStateFlow(1)
    val currentCommentPage: StateFlow<Int> = _currentCommentPage.asStateFlow()

    private val _totalCommentPages = MutableStateFlow(1)
    val totalCommentPages: StateFlow<Int> = _totalCommentPages.asStateFlow()

    private val _isLoadingComments = MutableStateFlow(false)
    val isLoadingComments: StateFlow<Boolean> = _isLoadingComments.asStateFlow()

    private val _hasMoreComments = MutableStateFlow(true)
    val hasMoreComments: StateFlow<Boolean> = _hasMoreComments.asStateFlow()

    // 核心修改 #2: 使用新的 Repository 替换旧的 DataStore
    private val browseHistoryRepository = BrowseHistoryRepository()

    fun loadPostDetail(postId: Long) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val credentials = AuthManager.getCredentials(context)!!

                val response = RetrofitClient.instance.getPostDetail(
                    token = credentials.third,
                    postId = postId
                )

                if (response.isSuccessful && response.body()?.code == 1) {
                    val post = response.body()?.data
                    _postDetail.value = post

                    post?.let {
                        // 核心修改 #3: 调用 Repository 的 addHistory 方法
                        browseHistoryRepository.addHistory(
                            BrowseHistory(
                                postId = it.id,
                                title = it.title,
                                previewContent = it.content.take(100)
                            )
                        )

                        _isLiked.value = it.is_thumbs == 1
                        _likeCount.value = it.thumbs.toIntOrNull() ?: 0
                        _commentCount.value = it.comment.toIntOrNull() ?: 0
                    }
                } else {
                    _errorMessage.value = "加载失败: ${response.body()?.msg ?: "未知错误"}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            }
        }
    }

    fun loadComments(postId: Long, page: Int = 1) {
    if (_isLoadingComments.value) return
    if (page > _totalCommentPages.value && page > 1) return

    _isLoadingComments.value = true

    viewModelScope.launch {
        try {
            val response = RetrofitClient.instance.getPostComments(
                postId = postId,
                limit = 20,
                page = page
            )

            if (response.isSuccessful && response.body()?.code == 1) {
                val newComments = response.body()?.data?.list ?: emptyList()
                val totalPages = response.body()?.data?.pagecount ?: 1

                _totalCommentPages.value = totalPages
                _hasMoreComments.value = page < totalPages

                _comments.value = if (page == 1) {
                    newComments
                } else {
                    _comments.value + newComments
                }
                
                // 确保当前页码正确设置
                if (page != _currentCommentPage.value) {
                    _currentCommentPage.value = page
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "加载评论失败: ${e.message}"
        } finally {
            _isLoadingComments.value = false
        }
    }
}

    fun loadNextPageComments(postId: Long) {
    val nextPage = _currentCommentPage.value + 1
    if (nextPage <= _totalCommentPages.value) {
        _currentCommentPage.value = nextPage // 添加这行：更新当前页码
        loadComments(postId, nextPage)
    }
}

    fun refreshComments(postId: Long) {
    _currentCommentPage.value = 1 // 重置为第一页
    _comments.value = emptyList() // 清空现有评论
    loadComments(postId, 1)
}

    fun clearErrorMessage() {
        _errorMessage.value = ""
    }

    fun toggleLike() {
        viewModelScope.launch {
            val postId = postDetail.value?.id ?: return@launch
            val isCurrentlyLiked = _isLiked.value

            try {
                val context = getApplication<Application>().applicationContext
                val credentials = AuthManager.getCredentials(context)!!

                val response = RetrofitClient.instance.likePost(
                    token = credentials.third,
                    postId = postId
                )

                if (response.isSuccessful && response.body()?.code == 1) {
                    _isLiked.value = !isCurrentlyLiked
                    _likeCount.value = if (isCurrentlyLiked) {
                        _likeCount.value - 1
                    } else {
                        _likeCount.value + 1
                    }
                } else {
                    _errorMessage.value = "操作失败: ${response.body()?.msg ?: "未知错误"}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "操作失败: ${e.message}"
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

    fun openReplyDialog(comment: RetrofitClient.models.Comment) {
        _currentReplyComment.value = comment
        _showReplyDialog.value = true
    }

    fun closeReplyDialog() {
        _showReplyDialog.value = false
        _currentReplyComment.value = null
    }

    fun submitComment(content: String, imageUrl: String? = null) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val credentials = AuthManager.getCredentials(context)!!
                val postId = postDetail.value?.id ?: return@launch
                val parentId = _currentReplyComment.value?.id ?: 0L

                val response = RetrofitClient.instance.postComment(
                    token = credentials.third,
                    content = content,
                    postId = postId,
                    parentId = parentId,
                    imageUrl = imageUrl ?: ""
                )

                if (response.isSuccessful && response.body()?.code == 1) {
                    refreshComments(postId)
                    if (parentId == 0L) _commentCount.value += 1
                    closeCommentDialog()
                    closeReplyDialog()
                } else {
                    _errorMessage.value = "提交失败: ${response.body()?.msg ?: "未知错误"}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "提交失败: ${e.message}"
            }
        }
    }

    fun deletePost() {
        viewModelScope.launch {
            val postId = postDetail.value?.id ?: return@launch
            val context = getApplication<Application>().applicationContext
            val credentials = AuthManager.getCredentials(context)!!

            try {
                val response = RetrofitClient.instance.deletePost(
                    token = credentials.third,
                    postId = postId
                )

                if (response.isSuccessful && response.body()?.code == 1) {
                    _deleteSuccess.value = true
                } else {
                    _errorMessage.value = "删除失败: ${response.body()?.msg ?: "未知错误"}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除失败: ${e.message}"
            }
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val credentials = AuthManager.getCredentials(context)!!

            try {
                val response = RetrofitClient.instance.deleteComment(
                    token = credentials.third,
                    commentId = commentId
                )

                if (response.isSuccessful && response.body()?.code == 1) {
                    postDetail.value?.id?.let { loadComments(it) }
                    _commentCount.value = maxOf(0, _commentCount.value - 1)
                } else {
                    _errorMessage.value = "删除失败: ${response.body()?.msg ?: "未知错误"}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除失败: ${e.message}"
            }
        }
    }

    fun toggleMoreMenu() {
        _showMoreMenu.value = !_showMoreMenu.value
    }
}
