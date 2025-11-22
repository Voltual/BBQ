//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.home

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AuthManager
//import cc.bbq.xq.RetrofitClient // 移除 RetrofitClient
import cc.bbq.xq.KtorClient // 导入 KtorClient
import cc.bbq.xq.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.res.stringResource
import cc.bbq.xq.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow // 添加导入

// 添加数据加载状态
sealed class DataLoadState {
    object NotLoaded : DataLoadState()
    object Loading : DataLoadState()
    object Loaded : DataLoadState()
    object Error : DataLoadState()
}

// 添加签到相关状态
data class HomeUiState(
    val showLoginPrompt: Boolean = true,
    val isLoading: Boolean = false,
    val avatarUrl: String? = null,
    val nickname: String = "You",
    val level: String = "LV0",
    val coins: String = "null",
    val userId: String = "null",
    val followersCount: String = "?",
    val fansCount: String = "?",
    val postsCount: String = "?",
    val likesCount: String = "?",
    // 签到相关状态
    val seriesDays: Int = 0,
    val signStatusMessage: String? = null,
    val createTime: String = "",
    val exp: Int = 0, // 添加经验值属性
    val lastSignTime: String = "",
    val displayDaysDiff: Int = 0,
    // 添加数据加载状态
    val dataLoadState: DataLoadState = DataLoadState.NotLoaded
)

class HomeViewModel : ViewModel() {
    var uiState = mutableStateOf(HomeUiState())
        private set

    // 新增：Snackbar HostState
    var snackbarHostState = mutableStateOf<SnackbarHostState?>(null)
        private set

    fun setSnackbarHostState(hostState: SnackbarHostState) {
        snackbarHostState.value = hostState
    }

    fun loadUserData(context: Context, forceRefresh: Boolean = false) {
        // 将实际的加载逻辑移到一个协程中
        viewModelScope.launch {
            val userCredentialsFlow = AuthManager.getCredentials(context)
            val userCredentials = userCredentialsFlow.first()
            if (userCredentials == null) return@launch // 如果没有凭证，则不加载数据

            // 如果数据已经加载且不是强制刷新，则跳过
            if (!forceRefresh && uiState.value.dataLoadState == DataLoadState.Loaded) {
                return@launch
            }

            
            try {
                uiState.value = uiState.value.copy(
                    isLoading = true,
                    dataLoadState = DataLoadState.Loading
                )

                // 使用 KtorClient 发起网络请求
                val response = withContext(Dispatchers.IO) {
                    //KtorClient.instance.getUserInfo(token = credentials.third)
                    KtorClient.ApiServiceImpl.getUserInfo(token = userCredentials.token)
                }

                response.onSuccess { result ->
                    result.data.let { userData ->
                        // 计算时间差（创建时间到上次签到时间）
                        val daysDiff = calculateDaysDiff(
                            userData.create_time,
                            userData.signlasttime
                        )

                        uiState.value = uiState.value.copy(
                            showLoginPrompt = false,
                            avatarUrl = userData.usertx,
                            nickname = userData.nickname,
                            level = userData.hierarchy,
                            coins = userData.money.toString(),
                            userId = userData.username,
                            followersCount = userData.followerscount.toString(),
                            fansCount = userData.fanscount.toString(),
                            postsCount = userData.postcount.toString(),
                            likesCount = userData.likecount.toString(),
                            seriesDays = userData.series_days,
                            createTime = userData.create_time,
                            lastSignTime = userData.signlasttime,
                            displayDaysDiff = daysDiff,
                            isLoading = false,
                            exp = userData.exp, // 更新经验值
                            dataLoadState = DataLoadState.Loaded
                        )
                    }
                }.onFailure { _ ->
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        dataLoadState = DataLoadState.Error
                    )
                }
            } catch (e: Exception) {
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    dataLoadState = DataLoadState.Error
                )
            }
        }
    }

    // 强制刷新用户数据
    fun refreshUserData(context: Context) {
        loadUserData(context, forceRefresh = true)
    }

    // 重置加载状态（用于用户登出等情况）
    fun resetLoadState() {
        uiState.value = uiState.value.copy(
            dataLoadState = DataLoadState.NotLoaded,
            showLoginPrompt = true
        )
    }

    // 签到功能
    fun signIn(context: Context) {
        // 将实际的签到逻辑移到一个协程中
        viewModelScope.launch {
            val userCredentialsFlow = AuthManager.getCredentials(context)
            val userCredentials = userCredentialsFlow.first()
            val token = userCredentials?.token ?: ""

            
            try {
                uiState.value = uiState.value.copy(isLoading = true)

                // 使用 KtorClient 发起网络请求
                val response = withContext(Dispatchers.IO) {
                    //RetrofitClient.instance.userSignIn(token = token)
                    KtorClient.ApiServiceImpl.userSignIn(token = token)
                }

                response.onSuccess { result ->
                    // 处理服务器返回的401错误
                    if (result.code == 401) {
                        uiState.value = uiState.value.copy(
                            signStatusMessage = "登录已过期，请长按头像刷新",
                            showLoginPrompt = true,
                            isLoading = false
                        )
                        // 重置加载状态，因为登录已过期
                        resetLoadState()
                    } else {
                        // 正常处理成功响应
                        uiState.value = uiState.value.copy(
                            signStatusMessage = result.msg,
                            isLoading = false
                        )

                        // 2秒后清除状态消息
                        launch {
                            delay(2000)
                            uiState.value = uiState.value.copy(signStatusMessage = null)
                        }

                        // 重新加载用户数据（强制刷新）
                        refreshUserData(context)
                    }
                }.onFailure { _ ->
                    uiState.value = uiState.value.copy(
                        signStatusMessage = "签到失败: 网络请求错误",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                uiState.value = uiState.value.copy(
                    signStatusMessage = "网络错误: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // 计算两个日期之间的天数差
    fun calculateDaysDiff(startDate: String, endDate: String): Int {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val start = format.parse(startDate) ?: Date()
            val end = format.parse(endDate) ?: Date()
            val diff = end.time - start.time
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    // 重新计算时间差（创建时间到当前时间）
    fun recalculateDaysDiff() {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
        val daysDiff = calculateDaysDiff(uiState.value.createTime, currentTime)
        uiState.value = uiState.value.copy(displayDaysDiff = daysDiff)
    }

    fun toggleDarkMode() {
        ThemeManager.toggleTheme()
    }

    fun updateLoginState(isLoggedIn: Boolean) {
        uiState.value = uiState.value.copy(showLoginPrompt = !isLoggedIn)
        // 如果用户登出，重置加载状态
        if (!isLoggedIn) {
            resetLoadState()
        }
    }

    // 新增：显示 Snackbar 的方法
    fun showSnackbar(context:Context ,message: String) {
        viewModelScope.launch {
            snackbarHostState.value?.showSnackbar(message)
        }
    }
}