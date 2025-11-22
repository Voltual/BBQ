//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。

package cc.bbq.xq.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class UserDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _userData = MutableLiveData<KtorClient.UserInformationData?>()
    val userData: LiveData<KtorClient.UserInformationData?> = _userData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // 添加状态跟踪
    private var _isInitialized = false
    private var _currentUserId: Long = -1L
    private val apiService = KtorClient.ApiServiceImpl

    fun loadUserDetails(userId: Long) {
        // 只有当用户ID真正改变时才重新加载
        if (this._currentUserId != userId) {
            this._currentUserId = userId
            this._isInitialized = false
            resetState()
            loadDataIfNeeded()
        } else {
            // 相同的用户ID，确保数据已加载
            loadDataIfNeeded()
        }
    }

    private fun resetState() {
        _userData.postValue(null)
        _errorMessage.postValue("")
    }

    // 内部方法：只在需要时加载数据
    private fun loadDataIfNeeded() {
        if (!_isInitialized && _currentUserId != -1L && !(_isLoading.value == true)) {
            _isInitialized = true
            loadData()
        }
    }

    // 提供手动刷新方法
    fun refresh() {
        if (_currentUserId != -1L) {
            loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val userCredentialsFlow = AuthManager.getCredentials(context)
            val userCredentials = userCredentialsFlow.first()
            val token = userCredentials?.token ?: ""

            // 检查是否已经在加载
            if (_isLoading.value == true) return@launch // 使用 return@launch 从协程返回

            _isLoading.postValue(true)

            try {
                val result = apiService.getUserInformation(
                    userId = _currentUserId,
                    token = token
                )

                when (val response = result.getOrNull()) {
                    is KtorClient.UserInformationResponse -> {
                        if (response.code == 1) {
                            // 修复：移除总是为 true 的条件检查，因为 response.data 是非空的
                            _userData.postValue(response.data)
                            _errorMessage.postValue("")
                        } else {
                            // 修复：移除不必要的 Elvis 操作符，因为 response.msg 是非空字符串
                            _errorMessage.postValue("加载失败: ${response.msg}")
                        }
                    }
                    else -> {
                        _errorMessage.postValue("加载失败: ${result.exceptionOrNull()?.message ?: "网络错误"}")
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("网络错误: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}    