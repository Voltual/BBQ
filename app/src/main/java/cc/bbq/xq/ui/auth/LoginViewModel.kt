//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.auth

import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.*
import cc.bbq.xq.AppStore
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import cc.bbq.xq.SineShopClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow // 添加这个导入

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    // --- 商店选择状态 ---
    private val _selectedStore = MutableStateFlow(AppStore.XIAOQU_SPACE)
    val selectedStore: StateFlow<AppStore> = _selectedStore.asStateFlow()

    fun onStoreSelected(store: AppStore) {
        _selectedStore.value = store
    }

    // --- 通用状态 ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    // --- 登录状态 ---
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    // --- 注册状态 ---
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _captcha = MutableStateFlow("")
    val captcha: StateFlow<String> = _captcha.asStateFlow()

    private val _verificationCodeUrl = MutableStateFlow<String?>(null)
    val verificationCodeUrl: StateFlow<String?> = _verificationCodeUrl.asStateFlow()

    // --- 事件处理 ---
    fun onUsernameChange(newUsername: String) {
        _username.value = newUsername
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onCaptchaChange(newCaptcha: String) {
        _captcha.value = newCaptcha
    }

    // --- 业务逻辑 ---
    fun login() {
        if (_username.value.isBlank() || _password.value.isBlank()) {
            _errorMessage.value = "用户名和密码不能为空"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                when (_selectedStore.value) {
                    AppStore.XIAOQU_SPACE -> loginXiaoqu()
                    AppStore.SIENE_SHOP -> loginSineShop()
                    // 添加 else 分支处理 LOCAL 或其他不需要登录的商店类型
                    else -> {
                        _errorMessage.value = "所选商店无需登录"
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
                _isLoading.value = false // 确保在异常时也关闭加载状态
            }
            // 注意：不再在 finally 块中设置 _isLoading.value = false，
            // 因为在 else 分支和 catch 块中已经处理了。
            // 只有在成功的登录流程中才需要在最后设置为 false。
        }
    }

    private suspend fun loginXiaoqu() {
        try {
            // 显式转换为 Application 类型
            val context: Application = getApplication()
            // 显式指定 Flow 的类型为 String
            val deviceIdFlow: Flow<String> = AuthManager.getDeviceId(context)
            val deviceId = deviceIdFlow.first()
            val loginResult = KtorClient.ApiServiceImpl.login(
                username = _username.value,
                password = _password.value,
                device = deviceId
            )
            if (loginResult.isSuccess) {
                val loginResponse = loginResult.getOrNull()
                if (loginResponse?.code == 1) {
                    loginResponse.data?.let {
                        saveCredentialsAndNotifySuccess(
                            usertoken = it.usertoken,
                            userId = it.id
                        )
                    } ?: run {
                        _errorMessage.value = "登录失败: 无法获取用户信息"
                    }
                } else {
                    _errorMessage.value = loginResponse?.msg ?: "登录失败"
                }
            } else {
                _errorMessage.value = loginResult.exceptionOrNull()?.message ?: "登录失败"
            }
        } catch (e: Exception) {
            _errorMessage.value = "网络错误: ${e.message}"
        } finally {
            // 只有在尝试登录后才设置加载状态为 false
            _isLoading.value = false
        }
    }

    private suspend fun loginSineShop() {
        try {
            val loginResult = SineShopClient.login(
                username = _username.value,
                password = _password.value
            )
            if (loginResult.isSuccess) {
                val token = loginResult.getOrNull() ?: ""
                if (token.isNotEmpty()) {
                    // 保存弦应用商店的token
                    val context: Application = getApplication()
                    AuthManager.saveSineMarketToken(context, token)
                    _loginSuccess.value = true // 登录成功
                } else {
                    _errorMessage.value = "弦应用商店登录失败: 无法获取token"
                }
            } else {
                _errorMessage.value = "弦应用商店登录失败: ${loginResult.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _errorMessage.value = "弦应用商店登录失败: ${e.message}"
        } finally {
            // 只有在尝试登录后才设置加载状态为 false
            _isLoading.value = false
        }
    }

    fun register() {
        if (_username.value.isBlank() || _password.value.isBlank() || _email.value.isBlank() || _captcha.value.isBlank()) {
            _errorMessage.value = "请填写所有注册信息"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // 显式转换为 Application 类型
                val context: Application = getApplication()
                // 显式指定 Flow 的类型为 String
                val deviceIdFlow: Flow<String> = AuthManager.getDeviceId(context)
                val deviceId = deviceIdFlow.first()
                val registerResult = KtorClient.ApiServiceImpl.register(
                    username = _username.value,
                    password = _password.value,
                    email = _email.value,
                    device = deviceId,
                    captcha = _captcha.value
                )
                if (registerResult.isSuccess) {
                    val registerResponse = registerResult.getOrNull()
                    if (registerResponse?.code == 1) {
                        // 注册成功，自动登录
                        loginAfterRegister()
                    } else {
                        _errorMessage.value = registerResponse?.msg ?: "注册失败"
                        // 注册失败后，通常需要刷新验证码
                        loadVerificationCode()
                    }
                } else {
                    _errorMessage.value = registerResult.exceptionOrNull()?.message ?: "注册失败"
                    // 注册失败后，通常需要刷新验证码
                    loadVerificationCode()
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
                // 注册失败后，通常需要刷新验证码
                loadVerificationCode()
            } finally {
                // 注意：loginAfterRegister 会再次设置 _isLoading，所以这里只在出错时设置
                // 如果 loginAfterRegister 成功，它会自己处理 _isLoading
                if (_errorMessage.value != null) {
                   _isLoading.value = false
                }
            }
        }
    }

    private suspend fun loginAfterRegister() {
        try {
            // 显式转换为 Application 类型
            val context: Application = getApplication()
            // 显式指定 Flow 的类型为 String
            val deviceIdFlow: Flow<String> = AuthManager.getDeviceId(context)
            val deviceId = deviceIdFlow.first()
            val loginResult = KtorClient.ApiServiceImpl.login(
                username = _username.value,
                password = _password.value,
                device = deviceId
            )
            if (loginResult.isSuccess) {
                val loginResponse = loginResult.getOrNull()
                if (loginResponse?.code == 1) {
                    loginResponse.data?.let {
                        saveCredentialsAndNotifySuccess(
                            usertoken = it.usertoken,
                            userId = it.id
                        )
                    } ?: run {
                        _errorMessage.value = "登录失败: 无法获取用户信息"
                    }
                } else {
                    _errorMessage.value = "注册成功，但自动登录失败: ${loginResponse?.msg}"
                }
            } else {
                _errorMessage.value = "注册成功，但自动登录时网络错误: ${loginResult.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _errorMessage.value = "注册成功，但自动登录时网络错误: ${e.message}"
        } finally {
             _isLoading.value = false // 无论自动登录成功与否，都结束加载状态
        }
    }

    fun loadVerificationCode() {
        _verificationCodeUrl.value = "http://apk.xiaoqu.online/api/get_image_verification_code?appid=1&type=2&t=${System.currentTimeMillis()}"
    }

    private suspend fun saveCredentialsAndNotifySuccess(usertoken: String, userId: Long) {
        // 显式转换为 Application 类型
        val context: Application = getApplication()
        AuthManager.saveCredentials(
            context,
            _username.value,
            _password.value,
            usertoken,
            userId
        )
        _loginSuccess.value = true
    }
}