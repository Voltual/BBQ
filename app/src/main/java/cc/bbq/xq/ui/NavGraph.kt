//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui

import android.app.Activity
import android.app.Application
import androidx.compose.animation.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.fillMaxSize
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.layout.padding
import cc.bbq.xq.ui.download.DownloadScreen // 导入 DownloadScreen
import cc.bbq.xq.ui.update.UpdateScreen // 新增导入
import cc.bbq.xq.ui.settings.storage.StoreManagerScreen
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cc.bbq.xq.AppStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import cc.bbq.xq.ui.auth.LoginScreen
import cc.bbq.xq.ui.auth.LoginViewModel
import cc.bbq.xq.ui.billing.BillingScreen
import cc.bbq.xq.ui.billing.BillingViewModel
import cc.bbq.xq.ui.community.*
import cc.bbq.xq.ui.community.compose.BaseComposeListScreen
import cc.bbq.xq.ui.community.compose.PostDetailScreen
import cc.bbq.xq.ui.home.AboutScreen
import cc.bbq.xq.ui.home.HomeDestination
import cc.bbq.xq.ui.log.LogScreen
import cc.bbq.xq.ui.log.LogViewModel
import cc.bbq.xq.ui.message.MessageCenterScreen
import cc.bbq.xq.ui.message.MessageViewModel
import cc.bbq.xq.ui.payment.PaymentCenterScreen
import cc.bbq.xq.ui.payment.PaymentType
import cc.bbq.xq.ui.payment.PaymentViewModel
import cc.bbq.xq.ui.player.PlayerScreen
import cc.bbq.xq.ui.player.PlayerViewModel
import cc.bbq.xq.ui.plaza.*
import cc.bbq.xq.ui.rank.RankingListScreen
import cc.bbq.xq.ui.search.SearchScreen
import cc.bbq.xq.ui.search.SearchViewModel
import cc.bbq.xq.ui.theme.ThemeCustomizeScreen
import cc.bbq.xq.ui.user.*
import cc.bbq.xq.ui.user.compose.UserListScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import cc.bbq.xq.ui.animation.materialSharedAxisXIn
import cc.bbq.xq.ui.animation.materialSharedAxisXOut
import androidx.compose.ui.unit.dp
import cc.bbq.xq.ui.animation.rememberSlideDistance
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.bbq.xq.ui.settings.update.UpdateSettingsScreen //导入更新屏幕
import androidx.compose.material3.SnackbarHostState // 确保 SnackbarHostState 被正确导入

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState, // 添加 SnackbarHostState 参数
    modifier: Modifier = Modifier//,
//    restartAppCallback: (() -> Unit)? = null
) {
   // val context = LocalContext.current
//    val density = LocalDensity.current
    val slideDistance = rememberSlideDistance()

    // 将 ViewModel 的创建移动到 AppNavHost 函数中
    val searchViewModel: SearchViewModel = org.koin.androidx.compose.koinViewModel()
    val postCreateViewModel: PostCreateViewModel = org.koin.androidx.compose.koinViewModel()
    val plazaViewModel: PlazaViewModel = org.koin.androidx.compose.koinViewModel { parametersOf(false) }
    val appReleaseViewModel: AppReleaseViewModel = org.koin.androidx.compose.koinViewModel()
    val messageViewModel: MessageViewModel = org.koin.androidx.compose.koinViewModel()
    val billingViewModel: BillingViewModel = org.koin.androidx.compose.koinViewModel()
    val paymentViewModel: PaymentViewModel = org.koin.androidx.compose.koinViewModel()
    val playerViewModel: PlayerViewModel = org.koin.androidx.compose.koinViewModel()
    val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
    val myLikesViewModel: MyLikesViewModel = org.koin.androidx.compose.koinViewModel()
    val hotPostsViewModel: HotPostsViewModel = org.koin.androidx.compose.koinViewModel()
    val followingPostsViewModel: FollowingPostsViewModel = org.koin.androidx.compose.koinViewModel()
//    val userDetailViewModel: UserDetailViewModel = org.koin.androidx.compose.koinViewModel()
    val myPostsViewModel: MyPostsViewModel = org.koin.androidx.compose.koinViewModel()
    val appDetailViewModel: AppDetailComposeViewModel = org.koin.androidx.compose.koinViewModel()

    NavHost(
        navController = navController,
        startDestination = Home.route,
        modifier = modifier,
        enterTransition = { materialSharedAxisXIn(forward = true, slideDistance = slideDistance) },
        exitTransition = { materialSharedAxisXOut(forward = true, slideDistance = slideDistance) },
        popEnterTransition = { materialSharedAxisXIn(forward = false, slideDistance = slideDistance) },
        popExitTransition = { materialSharedAxisXOut(forward = false, slideDistance = slideDistance) }
    ) {
        // --- 核心屏幕 ---
        composable(route = Home.route) {
    HomeDestination(
        navController = navController,
        snackbarHostState = snackbarHostState // 传递 SnackbarHostState
    )
}

        composable(route = Login.route) {
            val loginViewModel: LoginViewModel = org.koin.androidx.compose.koinViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
//                isBotLoginMode = false, 
                modifier = Modifier.fillMaxSize() // 添加 modifier
            )
        }

        // 在 NavGraph.kt 中更新 AboutScreen 的调用
        composable(route = About.route) {
    AboutScreen(
        modifier = Modifier.fillMaxSize(),
        snackbarHostState = snackbarHostState
    )
}

        composable(route = Search.route) {
            SearchScreen(
                viewModel = searchViewModel,
                onPostClick = { postId -> navController.navigate(PostDetail(postId).createRoute()) },
                onLogClick = { navController.navigate(LogViewer.route) },
                modifier = Modifier.fillMaxSize() // 添加 modifier
            )
        }
        // 新增：应用更新屏幕的导航项
        composable(route = Update.route) {
            UpdateScreen(
                snackbarHostState = snackbarHostState, // 传递来自 AppNavHost 的 snackbarHostState
                modifier = Modifier.fillMaxSize()
                // viewModel 会通过 viewModel() 工厂自动注入
            )
        }

        composable(route = ThemeCustomize.route) {
            ThemeCustomizeScreen(
                modifier = Modifier.fillMaxSize() // 添加 modifier
            )
        }
        
        composable(route = StoreManager.route) {
             // 在 NavGraph.kt 中更新存储管理屏幕的调用
             StoreManagerScreen()
        }

        // --- 社区与帖子 ---
        composable(route = PostDetail(0).route, arguments = PostDetail.arguments) { backStackEntry ->
            val postId = backStackEntry.arguments?.getLong(AppDestination.ARG_POST_ID) ?: 0L
            PostDetailScreen(
                postId = postId,
                navController = navController,
//                onBack = { navController.popBackStack() },
                onPostDeleted = { navController.popBackStack() },
                snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
                modifier = Modifier.fillMaxSize() // 添加 modifier
            )
        }

composable(route = CreatePost.route) {
    PostCreateScreen(
        viewModel = postCreateViewModel,
        navController = navController, // 传递 navController
        snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
        onBackClick = { navController.popBackStack() },
        // 移除 onSubmitClick 参数
        mode = "create",
        refundAppName = "",
        refundAppId = 0L,
        refundVersionId = 0L,
        refundPayMoney = 0,
        modifier = Modifier.fillMaxSize()
    )
}

composable(route = CreateRefundPost(0, 0, "", 0).route, arguments = CreateRefundPost.arguments) { backStackEntry ->
    val args = backStackEntry.arguments!!
    PostCreateScreen(
        viewModel = postCreateViewModel,
        navController = navController, // 传递 navController
        onBackClick = { navController.popBackStack() },
        // 移除 onSubmitClick 参数
        snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
        mode = "refund",
        refundAppName = URLDecoder.decode(args.getString(AppDestination.ARG_APP_NAME, ""), StandardCharsets.UTF_8.toString()),
        refundAppId = args.getLong(AppDestination.ARG_APP_ID),
        refundVersionId = args.getLong(AppDestination.ARG_VERSION_ID),
        refundPayMoney = args.getInt(AppDestination.ARG_PAY_MONEY),
        modifier = Modifier.fillMaxSize()
    )
}

        // 在 NavGraph.kt 中更新 BrowseHistoryScreen 的调用
        composable(route = BrowseHistory.route) {
    BrowseHistoryScreen(
        onPostClick = { postId -> navController.navigate(PostDetail(postId).createRoute()) },
        modifier = Modifier.fillMaxSize(),
        snackbarHostState = snackbarHostState
    )
}

        composable(route = ImagePreview("").route, arguments = ImagePreview.arguments) { backStackEntry ->
            val imageUrl = backStackEntry.arguments?.getString(AppDestination.ARG_IMAGE_URL)?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""
            ImagePreviewScreen(
                imageUrl = imageUrl,
                snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
                onClose = { navController.popBackStack() }
            )
        }
        
        composable(route = Download.route) {
    DownloadScreen(modifier = Modifier.fillMaxSize())
}

composable(route = MyComments.route) {
    MyCommentsScreen(
        navController = navController,
        modifier = Modifier.fillMaxSize()
    )
}

composable(route = MyReviews.route) {
    MyReviewsScreen(
        navController = navController,
        modifier = Modifier.fillMaxSize()
    )
}

composable(route = UserDetail(0).route, arguments = UserDetail.arguments) { backStackEntry ->
    val userId = backStackEntry.arguments?.getLong(AppDestination.ARG_USER_ID) ?: -1L
    val storeName = backStackEntry.arguments?.getString("store") ?: AppStore.XIAOQU_SPACE.name
    val store = try {
        AppStore.valueOf(storeName)
    } catch (e: IllegalArgumentException) {
        AppStore.XIAOQU_SPACE // 默认值
    }
    
    // 使用 koinViewModel() 而不是 viewModel()
    val viewModel: UserDetailViewModel = koinViewModel()
    
    // 简化的LaunchedEffect - 只设置用户ID
    LaunchedEffect(userId, store) {
        if (userId != -1L) {
            viewModel.loadUserDetails(userId, store)
        }
    }
    
    val userData by viewModel.userData.collectAsStateWithLifecycle()
val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    UserDetailScreen(
                userData = userData,
                isLoading = isLoading,
                snackbarHostState = snackbarHostState,
                errorMessage = errorMessage,
                onPostsClick = { navController.navigate(MyPosts(userId).createRoute()) },
                onResourcesClick = { uid, store -> // 接收 store 参数
    navController.navigate(ResourcePlaza(isMyResource = false, userId = uid, mode = "public", storeName = store.name).createRoute())
},
                onImagePreview = { imageUrl ->
                    navController.navigate(ImagePreview(imageUrl).createRoute())
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(route = MyPosts(0).route, arguments = MyPosts.arguments) { backStackEntry ->
    val userId = backStackEntry.arguments?.getLong(AppDestination.ARG_USER_ID) ?: -1L
    MyPostsScreen(
        viewModel = myPostsViewModel,
        userId = userId,
        navController = navController,
        snackbarHostState = snackbarHostState // 添加这行
    )
}

        // 在 NavGraph.kt 中更新 UserListScreen 的调用

        // 关注列表
composable(route = FollowList.route) {
    val viewModel: UserListViewModel = koinViewModel()
    
    // 设置列表类型并手动初始化
    LaunchedEffect(Unit) {
        viewModel.setListType(UserListType.FOLLOWERS)
        viewModel.loadInitialData()
    }
    
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UserListScreen(
        users = state.users,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
//        isEmpty = state.users.isEmpty() && !state.isLoading && state.errorMessage.isNullOrEmpty(),
        onLoadMore = { viewModel.loadNextPage() },
//        onRefresh = { viewModel.refresh() },
        onUserClick = { userId -> navController.navigate(UserDetail(userId).createRoute()) },
        modifier = Modifier.fillMaxSize()
    )
}

// 粉丝列表  
composable(route = FanList.route) {
    val viewModel: UserListViewModel = koinViewModel()
    
    // 设置列表类型并手动初始化
    LaunchedEffect(Unit) {
        viewModel.setListType(UserListType.FANS)
        viewModel.loadInitialData()
    }
    
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UserListScreen(
        users = state.users,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
  //      isEmpty = state.users.isEmpty() && !state.isLoading && state.errorMessage.isNullOrEmpty(),
        onLoadMore = { viewModel.loadNextPage() },
    //    onRefresh = { viewModel.refresh() },
        onUserClick = { userId -> navController.navigate(UserDetail(userId).createRoute()) },
        modifier = Modifier.fillMaxSize()
    )
}

composable(route = AccountProfile.route, arguments = AccountProfileArgs.arguments) { backStackEntry ->
    val storeName = backStackEntry.arguments?.getString("store") ?: AppStore.XIAOQU_SPACE.name
    val store = try {
        AppStore.valueOf(storeName)
    } catch (e: IllegalArgumentException) {
        AppStore.XIAOQU_SPACE
    }
    
    AccountProfileScreen(
        modifier = Modifier.fillMaxSize(),
        snackbarHostState = snackbarHostState,
        store = store // 传递 store 参数
    )
}

        // --- 资源广场 ---
composable(route = ResourcePlaza(false).route, arguments = ResourcePlaza.arguments) { backStackEntry ->
            val isMyResource = backStackEntry.arguments?.getBoolean(AppDestination.ARG_IS_MY_RESOURCE) ?: false
            val userId = backStackEntry.arguments?.getLong(AppDestination.ARG_USER_ID) ?: -1L
            val mode = backStackEntry.arguments?.getString("mode") ?: "public"
            val storeName = backStackEntry.arguments?.getString("store") ?: AppStore.XIAOQU_SPACE.name // 获取 storeName

            ResourcePlazaScreen(
                isMyResourceMode = isMyResource,
                mode = mode,
                storeName = storeName, // 传递 storeName
                navigateToAppDetail = { appId, versionId, store ->
                    navController.navigate(AppDetail(appId, versionId, store).createRoute())
                },
                userId = if (userId != -1L) userId.toString() else null,
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 应用详情页 ---
        composable(route = AppDetail("", 0, "").route, arguments = AppDetail.arguments) { backStackEntry ->
            val appId = backStackEntry.arguments?.getString(AppDestination.ARG_APP_ID) ?: ""
            val versionId = backStackEntry.arguments?.getLong(AppDestination.ARG_VERSION_ID) ?: 0L
            val storeName = backStackEntry.arguments?.getString("storeName") ?: "XIAOQU_SPACE"

            AppDetailScreen(
                appId = appId,
                versionId = versionId,
                storeName = storeName,
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }


        // 在 NavGraph.kt 中更新 AppReleaseScreen 的调用
        composable(route = CreateAppRelease.route) {
            AppReleaseScreen(
                viewModel = appReleaseViewModel,
                navController = navController, // 传递 navController
                snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
                modifier = Modifier.fillMaxSize()
            )
        }

        // 在 NavGraph.kt 中，更新 UpdateAppRelease 相关的代码
composable(route = UpdateAppRelease("").route, arguments = UpdateAppRelease.arguments) { backStackEntry ->
    val appDetailJson = backStackEntry.arguments?.getString(AppDestination.ARG_APP_DETAIL_JSON)?.let {
        URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
    }
    if (!appDetailJson.isNullOrBlank()) {
        // 改为使用 KtorClient 的 JsonConverter
        val appDetailToUpdate = KtorClient.JsonConverter.fromJson(appDetailJson)
        if (appDetailToUpdate != null) {
            appReleaseViewModel.populateFromAppDetail(appDetailToUpdate)
        }
    }
    AppReleaseScreen(
        viewModel = appReleaseViewModel,
        navController = navController,
        snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
        modifier = Modifier.fillMaxSize()
    )
}

        composable(route = LogViewer.route) {
            val logViewModel: LogViewModel = org.koin.androidx.compose.koinViewModel()
            LogScreen(
                viewModel = logViewModel,
                snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
//                onBackClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize() // 添加 modifier
            )
        }

        // --- 消息、账单、支付 ---
        composable(route = MessageCenter.route) {
            MessageCenterScreen(
                viewModel = messageViewModel,
                onMessageClick = { postId -> navController.navigate(PostDetail(postId).createRoute()) },
                modifier = Modifier.fillMaxSize()//,
//                navController = navController // 传递 navController
            )
        }

        composable(route = Billing.route) {
            LaunchedEffect(Unit) {
                billingViewModel.loadBilling()
            }
            BillingScreen(
                viewModel = billingViewModel//,
//                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = PaymentCenterAdvanced.route) {
            paymentViewModel.setPaymentInfo(type = PaymentType.POST_REWARD, locked = false)
            PaymentCenterScreen(
                viewModel = paymentViewModel,
                modifier = Modifier.fillMaxSize()//,
//                navController = navController // 传递 navController
            )
        }

        composable(route = PaymentForApp(0, "", 0, 0, "", "").route, arguments = PaymentForApp.arguments) { backStackEntry ->
            val args = backStackEntry.arguments ?: return@composable
            paymentViewModel.setPaymentInfo(
                type = PaymentType.APP_PURCHASE,
                appId = args.getLong(AppDestination.ARG_APP_ID),
                appName = URLDecoder.decode(args.getString(AppDestination.ARG_APP_NAME, ""), StandardCharsets.UTF_8.toString()),
                versionId = args.getLong(AppDestination.ARG_VERSION_ID),
                price = args.getInt(AppDestination.ARG_PRICE),
                iconUrl = URLDecoder.decode(args.getString(AppDestination.ARG_ICON_URL, ""), StandardCharsets.UTF_8.toString()),
                previewContent = URLDecoder.decode(args.getString(AppDestination.ARG_PREVIEW_CONTENT, ""), StandardCharsets.UTF_8.toString()),
                locked = true
            )
            PaymentCenterScreen(
                viewModel = paymentViewModel,
                modifier = Modifier.fillMaxSize()//,
//                navController = navController // 传递 navController
            )
        }

        composable(route = PaymentForPost(0, "", "", "", "", "").route, arguments = PaymentForPost.arguments) { backStackEntry ->
            val args = backStackEntry.arguments ?: return@composable
            paymentViewModel.setPaymentInfo(
                type = PaymentType.POST_REWARD,
                postId = args.getLong(AppDestination.ARG_POST_ID),
                postTitle = URLDecoder.decode(args.getString(AppDestination.ARG_POST_TITLE, ""), StandardCharsets.UTF_8.toString()),
                previewContent = URLDecoder.decode(args.getString(AppDestination.ARG_PREVIEW_CONTENT, ""), StandardCharsets.UTF_8.toString()),
                authorName = URLDecoder.decode(args.getString(AppDestination.ARG_AUTHOR_NAME, ""), StandardCharsets.UTF_8.toString()),
                authorAvatar = URLDecoder.decode(args.getString(AppDestination.ARG_AUTHOR_AVATAR, ""), StandardCharsets.UTF_8.toString()),
                postTime = URLDecoder.decode(args.getString(AppDestination.ARG_POST_TIME, ""), StandardCharsets.UTF_8.toString()),
                locked = false
            )
            PaymentCenterScreen(
                viewModel = paymentViewModel,
                modifier = Modifier.fillMaxSize()//,
//                navController = navController // 传递 navController
            )
        }
       

composable(
    route = UpdateSettings.route
) {
    UpdateSettingsScreen(
        snackbarHostState = snackbarHostState
    )
}

        // --- 列表屏幕 ---
composable(Community.route) { CommunityScreen(navController, communityViewModel, snackbarHostState) }
composable(MyLikes.route) { MyLikesScreen(navController, myLikesViewModel, snackbarHostState) }
composable(HotPosts.route) { HotPostsScreen(navController, hotPostsViewModel, snackbarHostState) }
composable(FollowingPosts.route) { FollowingPostsScreen(navController, followingPostsViewModel, snackbarHostState) }

        // --- 其他 ---
        composable(route = RankingList.route) {
            RankingListScreen(navController = navController)
        }

        composable(route = Player("").route, arguments = Player.arguments) { backStackEntry ->
            val bvid = backStackEntry.arguments?.getString(AppDestination.ARG_BVID) ?: ""
            if (bvid.isNotEmpty()) {
                LaunchedEffect(bvid) {
                    playerViewModel.loadVideoData(bvid)
                }
            }
            PlayerScreen(viewModel = playerViewModel, onBack = { navController.popBackStack() })
        }
    }
}


// 在 NavGraph.kt 中修复社区屏幕的导航逻辑
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(navController: NavController, viewModel: CommunityViewModel,snackbarHostState: SnackbarHostState) {
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            viewModel.loadInitialData()
        }
    }

    BaseComposeListScreen(
        title = "社区",
        posts = posts,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onItemClick = { post -> navController.navigate(PostDetail(post.postid).createRoute()) },
        onLoadMore = { viewModel.loadNextPage() },
        onRefresh = { viewModel.refresh() },
        onSearchClick = { navController.navigate(Search.route) },
        onCreateClick = { navController.navigate(CreatePost.route) },
        snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
        historyClick = { navController.navigate(BrowseHistory.route) },
        totalPages = totalPages,
        onJumpToPage = { page -> viewModel.jumpToPage(page) },
        onMessageClick = { navController.navigate(MessageCenter.route) },
        onNavigate = { route ->
            when {
                route == "community" -> navController.navigate(Community.route)
                route == "hot_posts" -> navController.navigate(HotPosts.route)
                route == "following_posts" -> navController.navigate(FollowingPosts.route)
                route == "my_likes" -> navController.navigate(MyLikes.route)
                route.startsWith("my_posts/") -> {
                    // 处理我的帖子导航
                    val userId = route.removePrefix("my_posts/").toLongOrNull()
                    if (userId != null) {
                        navController.navigate(MyPosts(userId).createRoute())
                    }
                }
                else -> {
                    // 其他路由
                    navController.navigate(route)
                }
            }
        },
        onBackClick = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLikesScreen(navController: NavController, viewModel: MyLikesViewModel,snackbarHostState: SnackbarHostState) {
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            viewModel.loadInitialData()
        }
    }

    BaseComposeListScreen(
        title = "我喜欢的",
        posts = posts,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onItemClick = { post -> navController.navigate(PostDetail(post.postid).createRoute()) },
        onLoadMore = { viewModel.loadNextPage() },
        onRefresh = { viewModel.refresh() },
        onSearchClick = { navController.navigate(Search.route) },
        onCreateClick = { navController.navigate(CreatePost.route) },
        snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
        historyClick = { navController.navigate(BrowseHistory.route) },
        totalPages = totalPages,
        onJumpToPage = { page -> viewModel.jumpToPage(page) },
        onMessageClick = { navController.navigate(MessageCenter.route) },
        onNavigate = { route ->
            when {
                route == "community" -> navController.navigate(Community.route)
                route == "hot_posts" -> navController.navigate(HotPosts.route)
                route == "following_posts" -> navController.navigate(FollowingPosts.route)
                route == "my_likes" -> navController.navigate(MyLikes.route)
                route.startsWith("my_posts/") -> {
                    // 处理我的帖子导航
                    val userId = route.removePrefix("my_posts/").toLongOrNull()
                    if (userId != null) {
                        navController.navigate(MyPosts(userId).createRoute())
                    }
                }
                else -> {
                    // 其他路由
                    navController.navigate(route)
                }
            }
        },
        onBackClick = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotPostsScreen(navController: NavController, viewModel: HotPostsViewModel,snackbarHostState: SnackbarHostState) {
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            viewModel.loadInitialData()
        }
    }

    BaseComposeListScreen(
        title = "热点",
        posts = posts,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onItemClick = { post -> navController.navigate(PostDetail(post.postid).createRoute()) },
        onLoadMore = { viewModel.loadNextPage() },
        onRefresh = { viewModel.refresh() },
        onSearchClick = { navController.navigate(Search.route) },
        onCreateClick = { navController.navigate(CreatePost.route) },
        snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
        historyClick = { navController.navigate(BrowseHistory.route) },
        totalPages = totalPages,
        onJumpToPage = { page -> viewModel.jumpToPage(page) },
        onMessageClick = { navController.navigate(MessageCenter.route) },
        onNavigate = { route ->
            when {
                route == "community" -> navController.navigate(Community.route)
                route == "hot_posts" -> navController.navigate(HotPosts.route)
                route == "following_posts" -> navController.navigate(FollowingPosts.route)
                route == "my_likes" -> navController.navigate(MyLikes.route)
                route.startsWith("my_posts/") -> {
                    // 处理我的帖子导航
                    val userId = route.removePrefix("my_posts/").toLongOrNull()
                    if (userId != null) {
                        navController.navigate(MyPosts(userId).createRoute())
                    }
                }
                else -> {
                    // 其他路由
                    navController.navigate(route)
                }
            }
        },
        onBackClick = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingPostsScreen(navController: NavController, viewModel: FollowingPostsViewModel,snackbarHostState: SnackbarHostState) {
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            viewModel.loadInitialData()
        }
    }

    BaseComposeListScreen(
        title = "关注的人",
        posts = posts,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onItemClick = { post -> navController.navigate(PostDetail(post.postid).createRoute()) },
        onLoadMore = { viewModel.loadNextPage() },
        onRefresh = { viewModel.refresh() },
        onSearchClick = { navController.navigate(Search.route) },
        snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
        onCreateClick = { navController.navigate(CreatePost.route) },
        historyClick = { navController.navigate(BrowseHistory.route) },
        totalPages = totalPages,
        onJumpToPage = { page -> viewModel.jumpToPage(page) },
        onMessageClick = { navController.navigate(MessageCenter.route) },
        onNavigate = { route ->
            when {
                route == "community" -> navController.navigate(Community.route)
                route == "hot_posts" -> navController.navigate(HotPosts.route)
                route == "following_posts" -> navController.navigate(FollowingPosts.route)
                route == "my_likes" -> navController.navigate(MyLikes.route)
                route.startsWith("my_posts/") -> {
                    // 处理我的帖子导航
                    val userId = route.removePrefix("my_posts/").toLongOrNull()
                    if (userId != null) {
                        navController.navigate(MyPosts(userId).createRoute())
                    }
                }
                else -> {
                    // 其他路由
                    navController.navigate(route)
                }
            }
        },
        onBackClick = { navController.popBackStack() }
    )
}