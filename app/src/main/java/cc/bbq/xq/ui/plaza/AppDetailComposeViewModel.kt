//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.plaza

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class AppDetailComposeViewModel(application: Application) : AndroidViewModel(application) {
    private val _appDetail = MutableStateFlow<KtorClient.AppDetail?>(null)
    val appDetail: StateFlow<KtorClient.AppDetail?> = _appDetail.asStateFlow()

    private val _comments = MutableStateFlow<List<KtorClient.Comment>>(emptyList())
    val comments: StateFlow<List<KtorClient.Comment>> = _comments.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _showCommentDialog = MutableStateFlow(false)
    val showCommentDialog: StateFlow<Boolean> = _showCommentDialog.asStateFlow()

    private val _showReplyDialog = MutableStateFlow(false)
    val showReplyDialog: StateFlow<Boolean> = _showReplyDialog.asStateFlow()

    private val _currentReplyComment = MutableStateFlow<KtorClient.Comment?>(null)
    val currentReplyComment: StateFlow<KtorClient.Comment?> = _currentReplyComment.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 状态跟踪 - 防止重复加载
    private var _currentAppId: Long = -1L
    private var _currentVersionId: Long = -1L
    private var _isInitialized = false

    // 核心初始化方法 - 类似其他ViewModel的模式
    fun initializeData(appId: Long, versionId: Long) {
        if (this._currentAppId != appId || this._currentVersionId != versionId) {
            this._currentAppId = appId
            this._currentVersionId = versionId
            this._isInitialized = false
            resetState()
            // 直接调用加载而不是通过条件检查
            loadAppDetail()
            loadComments()
        }
    }

    private fun resetState() {
        _appDetail.value = null
        _comments.value = emptyList()
        _errorMessage.value = ""
        // 移除 _isLoading.value = true
    }

    private fun loadDataIfNeeded() {
        if (!_isInitialized && _currentAppId != -1L && _currentVersionId != -1L) {
            _isInitialized = true
            loadAppDetail()
            loadComments()
        }
    }

    // 提供手动刷新方法
    fun refresh() {
        if (_currentAppId != -1L && _currentVersionId != -1L) {
            loadAppDetail()
            loadComments()
        }
    }

    private fun loadAppDetail() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = getApplication<Application>().applicationContext
                val credentials = AuthManager.getCredentials(context)
                val token = credentials?.third ?: ""

                val result = KtorClient.ApiServiceImpl.getAppsInformation(
                    token = token,
                    appsId = _currentAppId,
                    appsVersionId = _currentVersionId
                )

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    if (response.code == 1) {
                        _appDetail.value = response.data
                    } else {
                        _errorMessage.value = "加载失败: ${response.msg}"
                    }
                } else {
                    _errorMessage.value = "加载失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadComments(page: Int = 1) {
        viewModelScope.launch {
            try {
                val result = KtorClient.ApiServiceImpl.getAppsCommentList(
                    appsId = _currentAppId,
                    appsVersionId = _currentVersionId,
                    limit = 20,
                    page = page,
                    sortOrder = "desc"
                )

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    if (response.code == 1) {
                        val appComments = response.data.list
                        _comments.value = appComments.map { appComment ->
                            KtorClient.Comment(
                                id = appComment.id,
                                content = appComment.content,
                                userid = appComment.userid,
                                time = appComment.time,
                                username = appComment.username,
                                nickname = appComment.nickname,
                                usertx = appComment.usertx,
                                hierarchy = appComment.hierarchy,
                                parentid = appComment.parentid,
                                parentnickname = appComment.parentnickname,
                                parentcontent = appComment.parentcontent,
                                image_path = appComment.image_path,
                                sub_comments_count = 0
                            )
                        }
                    } else {
                        _errorMessage.value = "加载评论失败: ${response.msg}"
                    }
                } else {
                    _errorMessage.value = "加载评论失败: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载评论出错: ${e.message}"
            }
        }
    }

    fun openCommentDialog() { _showCommentDialog.value = true; _currentReplyComment.value = null }
    fun closeCommentDialog() { _showCommentDialog.value = false }
    fun openReplyDialog(comment: KtorClient.Comment) { _currentReplyComment.value = comment; _showReplyDialog.value = true }
    fun closeReplyDialog() { _showReplyDialog.value = false; _currentReplyComment.value = null }

    fun submitAppComment(content: String, imageUrl: String? = null) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val appDetail = _appDetail.value ?: return@launch
                val parentId = _currentReplyComment.value?.id ?: 0L // 确保 parentId 不为空

                val credentials = AuthManager.getCredentials(context)
                val token = credentials?.third.orEmpty()

                val result = KtorClient.ApiServiceImpl.postAppComment(
                    token = token,
                    content = content,
                    appsId = appDetail.id,
                    appsVersionId = appDetail.apps_version_id,
                    parentId = parentId, // 这里确保传递了 parentId
                    imageUrl = imageUrl
                )

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    if (response.code == 1) {
                        loadComments()
                        if (parentId == 0L) closeCommentDialog() else closeReplyDialog()
                    } else {
                        _errorMessage.value = response.msg
                    }
                } else {
                    _errorMessage.value = "提交失败: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "提交失败: ${e.message}"
            }
        }
    }

    fun deleteAppComment(commentId: Long) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val token = AuthManager.getCredentials(context)?.third.orEmpty()

                val result = KtorClient.ApiServiceImpl.deleteAppComment(token = token, commentId = commentId)
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    if (response.code == 1) {
                        val appDetail = _appDetail.value
                        if (appDetail != null) {
                            loadComments()
                        }
                    } else {
                        _errorMessage.value = response.msg
                    }
                } else {
                    _errorMessage.value = "删除失败: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除失败: ${e.message}"
            }
        }
    }

    fun deleteApp(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val app = _appDetail.value ?: return@launch
            val context = getApplication<Application>().applicationContext
            val token = AuthManager.getCredentials(context)?.third ?: ""

            if (token.isEmpty()) {
                _errorMessage.value = "未登录"
                return@launch
            }

            try {
                val result = KtorClient.ApiServiceImpl.deleteApp(
                    usertoken = token,
                    apps_id = app.id,
                    app_version_id = app.apps_version_id
                )
                
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    if (response.code == 1) {
                        _errorMessage.value = response.msg
                        withContext(Dispatchers.Main) { onSuccess() }
                    } else {
                        _errorMessage.value = response.msg
                    }
                } else {
                    _errorMessage.value = "删除失败: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            }
        }
    }
}