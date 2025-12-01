// /app/src/main/java/cc/bbq/xq/ui/user/UserDetailViewModel.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import cc.bbq.xq.SineShopClient
import cc.bbq.xq.BBQApplication
import cc.bbq.xq.data.unified.UnifiedUserDetail  // 新增：统一用户详情模型
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class UserDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _userData = MutableLiveData<UnifiedUserDetail?>()
    val userData: LiveData<UnifiedUserDetail?> = _userData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // 添加状态跟踪
    private var _isInitialized = false
    private var _currentUserId: Long = -1L
    private var _currentStore: AppStore = AppStore.XIAOQU_SPACE
    private val apiService = KtorClient.ApiServiceImpl

    fun loadUserDetails(userId: Long, store: AppStore = AppStore.XIAOQU_SPACE) {
        // 只有当用户ID或商店真正改变时才重新加载
        if (this._currentUserId != userId || this._currentStore != store) {
            this._currentUserId = userId
            this._currentStore = store
            this._isInitialized = false
            resetState()
            loadDataIfNeeded()
        } else {
            // 相同的用户ID和商店，确保数据已加载
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
            if (_isLoading.value == true) return@launch

            _isLoading.postValue(true)

            try {
                val result = when (_currentStore) {
                    AppStore.XIAOQU_SPACE -> {
                        // 小趣空间 API
                        apiService.getUserInformation(
                            userId = _currentUserId,
                            token = token
                        )
                    }
                    AppStore.SIENE_SHOP -> {
                        // 弦应用商店 API（需要 token，根据实际情况调整）
                        SineShopClient.getUserInfoById(_currentUserId)
                    }
                    else -> {
                        throw IllegalArgumentException("不支持的应用商店: $_currentStore")
                    }
                }

                when (val response = result.getOrNull()) {
                    is KtorClient.UserInformationResponse -> {
                        if (response.code == 1) {
                            _userData.postValue(response.data.toUnifiedUserDetail())  // 映射到统一模型
                            _errorMessage.postValue("")
                        } else {
                            _errorMessage.postValue("加载失败: ${response.msg}")
                        }
                    }
                    is SineShopClient.BaseResponse<*> -> {  // SineShop 使用 BaseResponse
                        if (response.code == 0) {
                            @Suppress("UNCHECKED_CAST")
                            val data = response.data as? SineShopClient.SineShopUserInfo
                            if (data != null) {
                                _userData.postValue(data.toUnifiedUserDetail())  // 映射到统一模型
                                _errorMessage.postValue("")
                            } else {
                                _errorMessage.postValue("用户数据为空")
                            }
                        } else {
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