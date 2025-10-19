package cc.bbq.xq.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import kotlinx.coroutines.launch

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
        val context = getApplication<Application>()
        val credentials = AuthManager.getCredentials(context)
        val token = credentials?.third ?: ""

        if (_isLoading.value == true) return

        _isLoading.postValue(true)

        viewModelScope.launch {
            try {
                val result = apiService.getUserInformation(
                    userId = _currentUserId,
                    token = token
                )

                when (val response = result.getOrNull()) {
                    is KtorClient.UserInformationResponse -> {
                        if (response.code == 1) {
                            response.data?.let {
                                _userData.postValue(it)
                                _errorMessage.postValue("")
                            } ?: run {
                                _errorMessage.postValue("用户数据为空")
                            }
                        } else {
                            _errorMessage.postValue("加载失败: ${response.msg ?: "未知错误"}")
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