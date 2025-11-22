//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.user

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import cc.bbq.xq.AuthManager
import cc.bbq.xq.ui.BrowseHistory
import cc.bbq.xq.ui.community.compose.BaseComposeListScreen
import cc.bbq.xq.ui.CreatePost
import cc.bbq.xq.ui.MessageCenter
import cc.bbq.xq.ui.PostDetail
import cc.bbq.xq.ui.Search
import cc.bbq.xq.ui.Community
import cc.bbq.xq.ui.HotPosts
import cc.bbq.xq.ui.FollowingPosts
import cc.bbq.xq.ui.MyLikes
import androidx.compose.material3.SnackbarHostState
import cc.bbq.xq.ui.MyPosts
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    viewModel: MyPostsViewModel,
    userId: Long, // 目标用户的ID
    snackbarHostState: SnackbarHostState,
    navController: NavController
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()

    val context = navController.context
    
    // 使用单个 LaunchedEffect，避免重复触发
    LaunchedEffect(userId) {
        viewModel.setUserId(userId)
    }
    
    // 简化的标题逻辑 - 不再需要复杂的标题设置
    val title = "用户帖子"
    
    BaseComposeListScreen(
        title = title,
        posts = posts,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onItemClick = { post ->
            navController.navigate(PostDetail(post.postid).createRoute())
        },
        onLoadMore = { viewModel.loadNextPage() },
        onRefresh = { viewModel.refresh() },
        onSearchClick = { navController.navigate(Search.route) },
        snackbarHostState = snackbarHostState,
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
                    val targetUserId = route.removePrefix("my_posts/").toLongOrNull()
                    if (targetUserId != null && targetUserId != userId) {
                        navController.navigate(MyPosts(targetUserId).createRoute())
                    }
                }
                else -> navController.navigate(route)
            }
        },
        onBackClick = { navController.popBackStack() }
    )
}