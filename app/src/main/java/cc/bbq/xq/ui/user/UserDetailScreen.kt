//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
    
package cc.bbq.xq.ui.user

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import cc.bbq.xq.util.formatTimestamp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import cc.bbq.xq.KtorClient
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
//import cc.bbq.xq.RetrofitClient
import cc.bbq.xq.ui.*
import cc.bbq.xq.ui.community.compose.CommentItem
import cc.bbq.xq.ui.compose.LinkifyText
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import cc.bbq.xq.R
import cc.bbq.xq.AuthManager
import cc.bbq.xq.ui.theme.AppShapes
import cc.bbq.xq.ui.theme.BBQButton
import cc.bbq.xq.ui.theme.BBQCard
import cc.bbq.xq.ui.theme.BBQOutlinedButton
import kotlinx.coroutines.flow.first
import cc.bbq.xq.data.unified.UnifiedUserDetail  // 导入 UnifiedUserDetail
import cc.bbq.xq.AppStore

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UserDetailScreen(
    userData: UnifiedUserDetail?,  // 使用 UnifiedUserDetail
    isLoading: Boolean,
    errorMessage: String?,
    onPostsClick: () -> Unit,
    onResourcesClick: (Long, AppStore) -> Unit, // 修改：增加 AppStore 参数
    onImagePreview: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState
) {
    // 下拉刷新状态
    var refreshing by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = {
        refreshing = true
        //viewModel.refresh()
        refreshing = false
    })

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        ScreenContent(
            modifier = Modifier.fillMaxSize(),
            userData = userData,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onPostsClick = onPostsClick,
            onResourcesClick = onResourcesClick,
            onImagePreview = onImagePreview,
            snackbarHostState = snackbarHostState
        )

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    userData: UnifiedUserDetail?,  // 使用 UnifiedUserDetail
    isLoading: Boolean,
    errorMessage: String?,
    onPostsClick: () -> Unit,
    onResourcesClick: (Long, AppStore) -> Unit, // 修改：增加 AppStore 参数: (Long) -> Unit,
    onImagePreview: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when {
            isLoading -> LoadingState(Modifier.align(Alignment.Center))
            !errorMessage.isNullOrEmpty() -> ErrorState(message = errorMessage, modifier = Modifier.align(Alignment.Center))
            userData == null -> EmptyState(modifier = Modifier.align(Alignment.Center))
            else -> {
                // 根据 store 选择不同的 UI
                when (userData.store) {
                    AppStore.XIAOQU_SPACE -> XiaoQuProfileContent(  // 使用 UnifiedUserDetail
                        userData = userData,
                        onPostsClick = onPostsClick,
                        onResourcesClick = onResourcesClick,
                        onImagePreview = onImagePreview,
                        snackbarHostState = snackbarHostState
                    )
                    AppStore.SIENE_SHOP -> SieneShopProfileContent(
                        userData = userData,
                        onResourcesClick = onResourcesClick,
                        onImagePreview = onImagePreview,
                        snackbarHostState = snackbarHostState
                    )
                    else -> Text("不支持的应用商店")
                }
            }
        }
    }
}

@Composable
private fun XiaoQuProfileContent( // 使用 UnifiedUserDetail
    userData: UnifiedUserDetail,
    onPostsClick: () -> Unit,
    onResourcesClick: (Long, AppStore) -> Unit, // 修改：增加 AppStore 参数: (Long) -> Unit,
    onImagePreview: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderCard(
            userData = userData,
            onAvatarClick = {
                if (!userData.avatarUrl.isNullOrEmpty()) {
                    onImagePreview(userData.avatarUrl)
                }
            }
        )

        ActionButtonsRow(
            userData = userData,
            onResourcesClick: (Long, AppStore) -> Unit, // 修改：增加 AppStore 参数: (Long) -> Unit,
            snackbarHostState = snackbarHostState
        )

        StatsCard(
            userData = userData,
            onPostsClick = onPostsClick
        )

        DetailsCard(userData = userData)
    }
}

@Composable
private fun SieneShopProfileContent(
    userData: UnifiedUserDetail,
    onResourcesClick: (Long, AppStore) -> Unit, // 修改：增加 AppStore 参数: (Long) -> Unit,
    onImagePreview: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部区域：渐变背景 + 头像 + 用户信息
        BBQCard {
            Box(modifier = Modifier.fillMaxWidth()) {
                ProfileBackground() // 使用和小趣空间相同的渐变背景
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 头像
                    AsyncImage(
                        model = userData.avatarUrl,
                        contentDescription = "用户头像",
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.ic_menu_profile),
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (!userData.avatarUrl.isNullOrEmpty()) {
                                    onImagePreview(userData.avatarUrl)
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 用户名
                    Text(
                        text = userData.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 用户ID
                    Text(
                        text = "ID: ${userData.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 详细信息卡片
        BBQCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "详细信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 描述
                userData.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 其他信息
                InfoItem(label = "等级:", value = userData.userOfficial ?: "无")
                InfoItem(label = "徽章:", value = userData.userBadge ?: "无")
                InfoItem(label = "状态:", value = userData.userStatus?.toString() ?: "无")
                InfoItem(label = "状态原因:", value = userData.userStatusReason ?: "无")
                InfoItem(label = "加入时间:", value = userData.joinTime?.let { formatTimestamp(it) } ?: "无")
                InfoItem(label = "上次在线:", value = userData.lastOnlineTime?.let { formatTimestamp(it) } ?: "无")
                InfoItem(label = "上传数量:", value = userData.uploadCount?.toString() ?: "0")
                InfoItem(label = "评论数量:", value = userData.replyCount?.toString() ?: "0")
            }
        }

        // "XXX 的资源" 按钮
        BBQOutlinedButton(
            onClick = { onResourcesClick(userData.id, userData.store) }, // 传递 store
            modifier = Modifier.fillMaxWidth(),
            text = { Text("${userData.displayName}的资源") }
        )
    }
}

@Composable
private fun HeaderCard(
    userData: UnifiedUserDetail, // 使用 UnifiedUserDetail
    onAvatarClick: () -> Unit,
) {
    BBQCard {
        Box(modifier = Modifier.fillMaxWidth()) {
            ProfileBackground()
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        avatarUrl = userData.avatarUrl ?: "",
                        onAvatarClick = onAvatarClick
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    UserBasicInfo(userData = userData)
                }
            }
        }
    }
}

@Composable
private fun ProfileBackground() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun UserAvatar(
    avatarUrl: String,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = avatarUrl,
        contentDescription = "用户头像",
        contentScale = ContentScale.Crop,
        placeholder = painterResource(R.drawable.ic_menu_profile),
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .clickable(onClick = onAvatarClick)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
    )
}

@Composable
private fun UserBasicInfo(userData: UnifiedUserDetail) { // 使用 UnifiedUserDetail
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = userData.displayName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            userData.hierarchy?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = AppShapes.small
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ID: ${userData.username}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionButtonsRow(
    userData: UnifiedUserDetail, // 使用 UnifiedUserDetail
    onResourcesClick: (Long) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    //val isFollowing = remember { mutableStateOf(userData.follow_status == "2") }
    //val apiService = KtorClient.ApiServiceImpl

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (userData.store == AppStore.XIAOQU_SPACE) {
            BBQButton(
                onClick = {
                   //TODO
                },
                modifier = Modifier.weight(1f),
                text = { Text("关注") }
            )
        }

        BBQOutlinedButton(
            onClick = { onResourcesClick(userData.id) },
            modifier = Modifier.weight(1f),
            text = { Text("${userData.displayName}的资源") }
        )
    }
}

@Composable
private fun StatsCard(
    userData: UnifiedUserDetail, // 使用 UnifiedUserDetail
    onPostsClick: () -> Unit
) {
    BBQCard {
        UserStats(
            userData = userData,
            onPostsClick = onPostsClick,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun UserStats(
    userData: UnifiedUserDetail, // 使用 UnifiedUserDetail
    onPostsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        userData.followersCount?.let {
            StatItem(
                count = it,
                label = "关注",
                modifier = Modifier.weight(1f)
            )
            VerticalDivider()
        }
        userData.fansCount?.let {
            StatItem(
                count = it,
                label = "粉丝",
                modifier = Modifier.weight(1f)
            )
            VerticalDivider()
        }
        userData.postCount?.let {
            StatItem(
                count = it,
                label = "帖子",
                onClick = onPostsClick,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider()
        }
        userData.likeCount?.let {
            StatItem(
                count = it,
                label = "获赞",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DetailsCard(userData: UnifiedUserDetail) { // 使用 UnifiedUserDetail
    BBQCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "详细信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            userData.money?.let {
                InfoItem(label = "硬币:", value = it.toString())
            }
            userData.commentCount?.let {
                InfoItem(label = "评论数:", value = it.toString())
            }
            userData.seriesDays?.let {
                InfoItem(label = "总签到:", value = "$it 天")
            }
            userData.lastActivityTime?.let {
                InfoItem(label = "上次在线:", value = it)
            }
        }
    }
}

@Composable
private fun VerticalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    )
}

@Composable
private fun StatItem(
    count: String,
    label: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = abbreviateNumber(count),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun abbreviateNumber(count: String): String {
    return try {
        val number = count.toLong()
        when {
            number >= 1_000_000 -> "%.1fM".format(number / 1_000_000.0)
            number >= 10_000 -> "%.1fW".format(number / 10_000.0)
            number >= 1_000 -> "%.1fK".format(number / 1_000.0)
            else -> count
        }
    } catch (e: NumberFormatException) {
        count
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "未找到用户数据",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}