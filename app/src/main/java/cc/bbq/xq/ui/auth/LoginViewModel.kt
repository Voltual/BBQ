//LoginViewModel.kt
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
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {
    // 移除 isBotLoginMode 参数，改为在方法中判断

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
    fun onUsernameChange(newUsername: String) { _username.value = newUsername }
    fun onPasswordChange(newPassword: String) { _password.value = newPassword }
    fun onEmailChange(newEmail: String) { _email.value = newEmail }
    fun onCaptchaChange(newCaptcha: String) { _captcha.value = newCaptcha }

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
                val deviceId = AuthManager.getDeviceId(getApplication())
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
                _isLoading.value = false
            }
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
                val deviceId = AuthManager.getDeviceId(getApplication())
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
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loginAfterRegister() {
        try {
            val deviceId = AuthManager.getDeviceId(getApplication())
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
        }
    }

    fun loadVerificationCode() {
        _verificationCodeUrl.value = "http://apk.xiaoqu.online/api/get_image_verification_code?appid=1&type=2&t=${System.currentTimeMillis()}"
    }

    private fun saveCredentialsAndNotifySuccess(usertoken: String, userId: Long) {
        AuthManager.saveCredentials(
            getApplication(), _username.value, _password.value, usertoken, userId
        )
        _loginSuccess.value = true
    }
}