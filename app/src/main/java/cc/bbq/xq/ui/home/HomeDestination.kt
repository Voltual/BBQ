package cc.bbq.xq.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize // 新增：导入 fillMaxSize 扩展函数
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier // 新增导入
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cc.bbq.xq.AppStore
import cc.bbq.xq.AuthManager
import cc.bbq.xq.restartMainActivity
import cc.bbq.xq.ui.theme.BBQTheme
import cc.bbq.xq.ui.theme.ThemeManager
import cc.bbq.xq.ui.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.res.stringResource
import cc.bbq.xq.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import cc.bbq.xq.SineShopClient
import cc.bbq.xq.ui.Update // 导入 Update 导航目标
import cc.bbq.xq.ui.MyComments // 导入 MyComments 导航目标

@Composable
fun HomeDestination(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val userCredentialsFlow = AuthManager.getCredentials(context)
        val userCredentials = userCredentialsFlow.first()
        val isLoggedIn = userCredentials != null

        viewModel.updateLoginState(isLoggedIn)
        if (isLoggedIn && uiState.dataLoadState == DataLoadState.NotLoaded) {
            viewModel.loadUserData(context)
        }

        viewModel.checkAndUpdateSineShopLoginState(context)
    }

    val onAvatarClick = remember {
        {
            if (!uiState.showLoginPrompt) {
                viewModel.toggleDarkMode()
                val modeName = if (ThemeManager.isAppDarkTheme) "深色" else "亮色"
                viewModel.showSnackbar(context.getString(R.string.theme_changed,modeName))
            } else {
                navController.navigate(Login.route)
            }
        }
    }

    val onAvatarLongClick = remember {
        {
            if (!uiState.showLoginPrompt) {
                viewModel.refreshUserData(context)
                viewModel.checkAndUpdateSineShopLoginState(context)
            }
            restartMainActivity(context)
        }
    }

    val onLoginClick = remember {
        { navController.navigate(Login.route) }
    }

    val onSineShopLoginClick = remember {
        { navController.navigate(Login.route) }
    }

    val userIdFlow = AuthManager.getUserId(context)

    BBQTheme(appDarkTheme = ThemeManager.isAppDarkTheme) {
        HomeScreen(
            state = HomeState(
                showLoginPrompt = uiState.showLoginPrompt,
                isLoading = uiState.isLoading, // 现在 HomeState 有 isLoading 了
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
            sineShopUserInfo = uiState.sineShopUserInfo,
            sineShopLoginPrompt = uiState.sineShopLoginPrompt,
            onSineShopLoginClick = onSineShopLoginClick,
            onPaymentCenterClick = { navController.navigate(PaymentCenterAdvanced.route) },
            onAvatarClick = onAvatarClick,
            onAvatarLongClick = onAvatarLongClick,
            onMessageCenterClick = { navController.navigate(MessageCenter.route) },
            onBrowseHistoryClick = { navController.navigate(BrowseHistory.route) },
            onMyLikesClick = { navController.navigate(MyLikes.route) },
            onFollowersClick = { navController.navigate(FollowList.route) },
            onFansClick = { navController.navigate(FanList.route) },
            onPostsClick = {
                coroutineScope.launch {
                    val userId = userIdFlow.first()
                    if (userId > 0) {
                        navController.navigate(MyPosts(userId).createRoute())
                    } else {
                        viewModel.showSnackbar(context.getString(R.string.unable_to_get_userid))
                    }
                }
            },
            onMyResourcesClick = {
                coroutineScope.launch{
                    val userId = userIdFlow.first()
                    if (userId > 0) {
                        navController.navigate(ResourcePlaza(isMyResource = true, userId = userId).createRoute())
                    } else {
                        viewModel.showSnackbar(context.getString(R.string.login_first_my_resources))
                        navController.navigate(Login.route)
                    }
                }
            },
            onBillingClick = { navController.navigate(Billing.route) },
            onLoginClick = onLoginClick,
            onSettingsClick = { navController.navigate(ThemeCustomize.route) },
            onSignClick = { viewModel.signIn(context) },
            onAboutClick = { navController.navigate(About.route) },
            onAccountProfileClick = { navController.navigate(AccountProfile.createRoute(AppStore.SIENE_SHOP)) },
            onRecalculateDays = { viewModel.recalculateDaysDiff() },
            onNavigateToUpdate = { navController.navigate(Update.route) }, // 传递导航回调
            onNavigateToMyReviews = { navController.navigate(MyReviews.route) }, // 新增：传递导航回调
            onNavigateToMyComments = {navController.navigate(MyComments.route)},
            onNavigateToCreateAppRelease = {navController.navigate(CreateAppRelease.route)},
            modifier = Modifier.fillMaxSize(), 
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            navController = navController
        )
    }
}