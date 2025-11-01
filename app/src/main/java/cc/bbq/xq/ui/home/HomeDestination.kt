//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.home

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cc.bbq.xq.AuthManager
import cc.bbq.xq.restartMainActivity
import cc.bbq.xq.ui.theme.BBQTheme
import cc.bbq.xq.ui.theme.ThemeManager
import cc.bbq.xq.ui.*

@Composable
fun HomeDestination(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState

    // 使用 LaunchedEffect 配合登录状态，只在登录状态变化时触发
    val isLoggedIn = AuthManager.getCredentials(context) != null
    
    LaunchedEffect(isLoggedIn) {
        viewModel.updateLoginState(isLoggedIn)
        if (isLoggedIn && uiState.dataLoadState == DataLoadState.NotLoaded) {
            viewModel.loadUserData(context)
        }
    }

    val onAvatarClick = remember {
        {
            if (!uiState.showLoginPrompt) {
                viewModel.toggleDarkMode()
                val modeName = if (ThemeManager.isAppDarkTheme) "深色" else "亮色"
                Toast.makeText(context, "已切换至${modeName}模式", Toast.LENGTH_SHORT).show()
            } else {
                navController.navigate(Login.route)
            }
        }
    }

    val onAvatarLongClick = remember {
        { 
            // 长按时强制刷新数据
            if (!uiState.showLoginPrompt) {
                viewModel.refreshUserData(context)
            }
            restartMainActivity(context) 
        }
    }

    val onLoginClick = remember {
        { navController.navigate(Login.route) }
    }

    val userId = AuthManager.getUserId(context)

    BBQTheme(appDarkTheme = ThemeManager.isAppDarkTheme) {
        HomeScreen(
            state = HomeState(
                showLoginPrompt = uiState.showLoginPrompt,
                isLoading = uiState.isLoading,
                avatarUrl = uiState.avatarUrl,
                nickname = uiState.nickname,
                level = uiState.level,
                coins = uiState.coins,
                exp = uiState.exp,
                userId = uiState.userId,
                followersCount = uiState.followersCount,
                fansCount = uiState.fansCount,
                postsCount = uiState.postsCount,
                likesCount = uiState.likesCount,
                seriesDays = uiState.seriesDays,
                signStatusMessage = uiState.signStatusMessage,
                displayDaysDiff = uiState.displayDaysDiff
            ),
            onPaymentCenterClick = { navController.navigate(PaymentCenterAdvanced.route) },
            onAvatarClick = onAvatarClick,
            onAvatarLongClick = onAvatarLongClick,
            onMessageCenterClick = { navController.navigate(MessageCenter.route) },
            onBrowseHistoryClick = { navController.navigate(BrowseHistory.route) },
            onMyLikesClick = { navController.navigate(MyLikes.route) },
            onFollowersClick = { navController.navigate(FollowList.route) },
            onFansClick = { navController.navigate(FanList.route) },
            onPostsClick = {
                if (userId != null) {
                    navController.navigate(MyPosts(userId).createRoute())
                } else {
                    Toast.makeText(context, "无法获取用户ID", Toast.LENGTH_SHORT).show()
                }
            },
            onMyResourcesClick = {
                if (userId != null) {
                    navController.navigate(ResourcePlaza(isMyResource = true, userId = userId).createRoute())
                } else {
                    Toast.makeText(context, "请先登录以查看我的资源", Toast.LENGTH_SHORT).show()
                    navController.navigate(Login.route)
                }
            },
            onBillingClick = { navController.navigate(Billing.route) },
            onLoginClick = onLoginClick,
            onSettingsClick = { navController.navigate(ThemeCustomize.route) },
            onSignClick = { viewModel.signIn(context) },
            onAboutClick = { navController.navigate(About.route) },
            onAccountProfileClick = { navController.navigate(AccountProfile.route) },
            onRecalculateDays = { viewModel.recalculateDaysDiff() }
        )
    }
}