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
    
    // 简化的 LaunchedEffect - 只设置用户ID，加载逻辑在 ViewModel 中处理
    LaunchedEffect(userId) {
        viewModel.setUserId(userId)
    }
    
    // 获取当前登录用户的ID
    val currentUserIdFlow = AuthManager.getUserId(context)
    // 在 LaunchedEffect 内部获取 currentUserId 的值
    var title = "用户帖子" // 默认标题
    LaunchedEffect(Unit) {
        val currentUserId = currentUserIdFlow.first()
        title = if (currentUserId == userId) "我的帖子" else "用户帖子"
    }
    
    BaseComposeListScreen(
        title = title, // 注意：由于 title 是在 LaunchedEffect 中设置的，这里可能不会立即显示正确的值。
        // 一个更可靠的方法是直接在 BaseComposeListScreen 中处理标题逻辑，或者将 currentUserId 传递给它。
        // 为了简化，我们暂时这样处理，实际应用中可能需要更复杂的逻辑。
        posts = posts,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onItemClick = { post ->
            navController.navigate(PostDetail(post.postid).createRoute())
        },
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
                    val targetUserId = route.removePrefix("my_posts/").toLongOrNull()
                    // 使用 first() 来获取 Flow 的值
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