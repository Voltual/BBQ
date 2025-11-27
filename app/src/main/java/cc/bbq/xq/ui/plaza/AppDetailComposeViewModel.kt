//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.plaza

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import cc.bbq.xq.SineShopClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

class AppDetailComposeViewModel(application: Application) : AndroidViewModel(application) {
    private val _appDetail = MutableStateFlow<Any?>(null) // 使用 Any? 类型
    val appDetail: StateFlow<Any?> = _appDetail.asStateFlow()

    private val _comments = MutableStateFlow<List<Any>>(emptyList()) // 使用 Any 类型
    val comments: StateFlow<List<Any>> = _comments.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _showCommentDialog = MutableStateFlow(false)
    val showCommentDialog: StateFlow<Boolean> = _showCommentDialog.asStateFlow()

    private val _showReplyDialog = MutableStateFlow(false)
    val showReplyDialog: StateFlow<Boolean> = _showReplyDialog.asStateFlow()

    private val _currentReplyComment = MutableStateFlow<Any?>(null) // 使用 Any? 类型
    val currentReplyComment: StateFlow<Any?> = _currentReplyComment.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _appStore = MutableStateFlow(AppStore.XIAOQU_SPACE) // 默认商店
    val appStore: StateFlow<AppStore> = _appStore.asStateFlow()

    // 状态跟踪 - 防止重复加载
    private var _currentAppId: Long = -1L
    private var _currentVersionId: Long = -1L
    private var _isInitialized = false

    // 核心初始化方法 - 类似其他ViewModel的模式
    fun initializeData(appId: Long, versionId: Long, appStore: AppStore = AppStore.XIAOQU_SPACE) {
        if (this._currentAppId != appId || this._currentVersionId != versionId || this._appStore.value != appStore) {
            this._currentAppId = appId
            this._currentVersionId = versionId
            this._isInitialized = false
            _appStore.value = appStore
            resetState()
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
                // 检查 _currentVersionId 是否为 0，以确定是否处于弦应用商店模式
                if (_currentVersionId == 0L) {
                    // 弦应用商店模式
                    val result = SineShopClient.getSineShopAppInfo(_currentAppId.toInt())
                    if (result.isSuccess) {
                        _appDetail.value = result.getOrThrow()
                    } else {
                        _errorMessage.value = "加载失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"
                    }
                } else {
                    // 小趣空间模式
                    val context = getApplication<Application>().applicationContext
                    val userCredentialsFlow = AuthManager.getCredentials(context)
                    val userCredentials = userCredentialsFlow.first()
                    val token = userCredentials?.token ?: ""

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
                // 检查 _currentVersionId 是否为 0，以确定是否处于弦应用商店模式
                if (_currentVersionId == 0L) {
                    // 弦应用商店模式
                    val result = SineShopClient.getSineShopAppComments(_currentAppId.toInt(), page = page)
                    if (result.isSuccess) {
                        _comments.value = result.getOrThrow().list
                    } else {
                        _errorMessage.value = "加载评论失败: ${result.exceptionOrNull()?.message}"
                    }
                } else {
                    // 小趣空间模式
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
                            _comments.value = response.data.list
                        } else {
                            _errorMessage.value = "加载评论失败: ${response.msg}"
                        }
                    } else {
                        _errorMessage.value = "加载评论失败: ${result.exceptionOrNull()?.message}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载评论出错: ${e.message}"
            }
        }
    }

    fun openCommentDialog() { _showCommentDialog.value = true; _currentReplyComment.value = null }
    fun closeCommentDialog() { _showCommentDialog.value = false }
    fun openReplyDialog(comment: Any) { _currentReplyComment.value = comment; _showReplyDialog.value = true }
    fun closeReplyDialog() { _showReplyDialog.value = false; _currentReplyComment.value = null }

    fun submitAppComment(content: String, imageUrl: String? = null) {
        viewModelScope.launch {
            try {
                // 检查 _currentVersionId 是否为 0，以确定是否处于弦应用商店模式
                 if (_currentVersionId == 0L) {
                    // 弦应用商店模式
                    val appDetail = _appDetail.value as? SineShopClient.SineShopAppDetail ?: return@launch
                    val parentComment = _currentReplyComment.value as? SineShopClient.SineShopComment
                    val parentId = parentComment?.id ?: 0
                    val result: Result<Int> = if (parentId == 0) {
                        // 发送根评论
                        SineShopClient.postSineShopAppRootComment(appId = appDetail.id, content = content)
                    } else {
                        // 发送回复评论
                        SineShopClient.postSineShopAppReplyComment(commentId = parentId, content = content)
                    }

                    if (result.isSuccess) {
                        loadComments()
                        if (parentId == 0) closeCommentDialog() else closeReplyDialog()
                    } else {
                        _errorMessage.value = "提交评论失败: ${result.exceptionOrNull()?.message}"
                    }
                } else {
                    val context = getApplication<Application>().applicationContext
                    val userCredentialsFlow = AuthManager.getCredentials(context)
                    val userCredentials = userCredentialsFlow.first()
                    val token = userCredentials?.token ?: ""
                    val appDetail = _appDetail.value as? KtorClient.AppDetail ?: return@launch
                    val parentId = (_currentReplyComment.value as? KtorClient.Comment)?.id ?: 0L // 确保 parentId 不为空

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
                }
            } catch (e: Exception) {
                _errorMessage.value = "提交失败: ${e.message}"
            }
        }
    }

    fun deleteAppComment(commentId: Long) {
        viewModelScope.launch {
            try {
                // 检查 _currentVersionId 是否为 0，以确定是否处于弦应用商店模式
                if (_currentVersionId == 0L) {
                    // 弦应用商店模式
                     val result = SineShopClient.deleteSineShopComment(commentId.toInt())
                        if (result.isSuccess) {
                            loadComments()
                        } else {
                            _errorMessage.value = "删除评论失败: ${result.exceptionOrNull()?.message}"
                        }
                } else {
                    val context = getApplication<Application>().applicationContext
                    val userCredentialsFlow = AuthManager.getCredentials(context)
                    val userCredentials = userCredentialsFlow.first()
                    val token = userCredentials?.token ?: ""

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
            val userCredentialsFlow = AuthManager.getCredentials(context)
            val userCredentials = userCredentialsFlow.first()
            val token = userCredentials?.token ?: ""

            if (token.isEmpty()) {
                _errorMessage.value = "未登录"
                return@launch
            }

            try {
                // 检查 _currentVersionId 是否为 0，以确定是否处于弦应用商店模式
                if (_currentVersionId == 0L) {
                    // 弦应用商店模式
                    // 弦应用商店没有删除应用的API，这里可以显示一个提示或者不执行任何操作
                    _errorMessage.value = "弦应用商店不支持删除应用"
                } else {
                    val appDetail = app as? KtorClient.AppDetail ?: return@launch
                    val result = KtorClient.ApiServiceImpl.deleteApp(
                        usertoken = token,
                        apps_id = appDetail.id,
                        app_version_id = appDetail.apps_version_id
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
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = ""
    }
}