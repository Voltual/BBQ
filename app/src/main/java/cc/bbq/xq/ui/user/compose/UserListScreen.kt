//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cc.bbq.xq.R
import cc.bbq.xq.RetrofitClient
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun UserListScreen(
    users: List<RetrofitClient.models.UserItem>,
    isLoading: Boolean,
    errorMessage: String?,
    isEmpty: Boolean,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onUserClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // 修复：使用更简单的状态管理
    val safeUsers by remember(users) { mutableStateOf(users) }
    val safeIsLoading by remember(isLoading) { mutableStateOf(isLoading) }
    val safeErrorMessage by remember(errorMessage) { mutableStateOf(errorMessage) }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 错误状态显示
        safeErrorMessage?.takeIf { it.isNotEmpty() }?.let { message ->
            ErrorState(message = message, onRetry = onRefresh)
            return
        }

        // 空状态显示
        if (safeUsers.isEmpty() && !safeIsLoading && safeErrorMessage.isNullOrEmpty()) {
            EmptyState()
            return
        }

        // 使用更安全的 LazyColumn 实现
        SafeLazyColumn(
            users = safeUsers,
            isLoading = safeIsLoading,
            onLoadMore = onLoadMore,
            onUserClick = onUserClick,
            onRefresh = onRefresh
        )
    }
}

@Composable
private fun SafeLazyColumn(
    users: List<RetrofitClient.models.UserItem>,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    onUserClick: (Long) -> Unit,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()
    
    // 修复：使用更保守的加载更多检测
    var lastLoadMoreIndex by remember { mutableIntStateOf(-1) }
    
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo
            
            if (visibleItems.isNotEmpty() && totalItems > 0) {
                val lastVisibleIndex = visibleItems.last().index
                // 只有当滚动到接近底部且不是正在加载时才触发
                if (lastVisibleIndex >= totalItems - 2 && 
                    lastVisibleIndex != lastLoadMoreIndex && 
                    !isLoading) {
                    lastLoadMoreIndex = lastVisibleIndex
                    onLoadMore()
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        itemsIndexed(
            items = users,
            key = { index, user -> user.id ?: index } // 确保有唯一的 key
        ) { index, user ->
            StableUserListItem(user = user, onClick = { onUserClick(user.id) })
            if (index < users.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun StableUserListItem(
    user: RetrofitClient.models.UserItem,
    onClick: () -> Unit
) {
    // 修复：完全稳定的状态管理
    val stableUser = remember(user) { user }
    val avatarUrl = remember(stableUser.usertx) { stableUser.usertx }
    val nickname = remember(stableUser.nickname) { stableUser.nickname }
    val hierarchy = remember(stableUser.hierarchy) { stableUser.hierarchy }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(R.drawable.ic_menu_profile),
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = hierarchy,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_users_found),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}