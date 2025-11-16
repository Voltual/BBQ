//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.user

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UserDetailScreen(
    userData: KtorClient.UserInformationData?,
    isLoading: Boolean,
    errorMessage: String?,
    onPostsClick: () -> Unit,
    onResourcesClick: (Long) -> Unit,
    onImagePreview: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState // 添加 SnackbarHostState 参数
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
            snackbarHostState = snackbarHostState // 传递 SnackbarHostState
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
    userData: KtorClient.UserInformationData?,
    isLoading: Boolean,
    errorMessage: String?,
    onPostsClick: () -> Unit,
    onResourcesClick: (Long) -> Unit,
    onImagePreview: (String) -> Unit,
    snackbarHostState: SnackbarHostState // 添加 SnackbarHostState 参数
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
            else -> UserProfileContent(
                userData = userData,
                onPostsClick = onPostsClick,
                onResourcesClick = onResourcesClick,
                onImagePreview = onImagePreview,
                snackbarHostState = snackbarHostState // 传递 SnackbarHostState
            )
        }
    }
}

@Composable
private fun UserProfileContent(
    userData: KtorClient.UserInformationData,
    onPostsClick: () -> Unit,
    onResourcesClick: (Long) -> Unit,
    onImagePreview: (String) -> Unit,
    snackbarHostState: SnackbarHostState // 添加 SnackbarHostState 参数
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
                if (userData.usertx.isNotEmpty()) {
                    onImagePreview(userData.usertx)
                }
            }
        )

        ActionButtonsRow(
            userData = userData,
            onResourcesClick = { onResourcesClick(userData.id) },
            snackbarHostState = snackbarHostState // 传递 SnackbarHostState
        )

        StatsCard(
            userData = userData,
            onPostsClick = onPostsClick
        )

        DetailsCard(userData = userData)
    }
}

// 其他组件保持不变...
@Composable
private fun HeaderCard(
    userData: KtorClient.UserInformationData,
    onAvatarClick: () -> Unit,
) {
    BBQCard {
        Box(modifier = Modifier.fillMaxWidth()) {
            ProfileBackground()
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        avatarUrl = userData.usertx,
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
private fun UserBasicInfo(userData: KtorClient.UserInformationData) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = userData.nickname,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = userData.hierarchy,
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
    userData: KtorClient.UserInformationData,
    onResourcesClick: () -> Unit,
    snackbarHostState: SnackbarHostState // 添加 SnackbarHostState 参数
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isFollowing = remember { mutableStateOf(userData.follow_status == "2") }
    val apiService = KtorClient.ApiServiceImpl

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BBQButton(
            onClick = {
                val token = AuthManager.getCredentials(context)?.third
                if (token.isNullOrBlank()) {
                    //Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.login_first),
                            duration = SnackbarDuration.Short
                        )
                    }
                    return@BBQButton
                }
                coroutineScope.launch {
                    try {
                        val result = apiService.followUser(token = token, followedId = userData.id)
                        when (val response = result.getOrNull()) {
                            is KtorClient.BaseResponse -> {
                                if (response.code == 1) {
                                    isFollowing.value = !isFollowing.value
                                    //Toast.makeText(
                                    //    context,
                                    //    if (isFollowing.value) "关注成功" else "取消关注成功",
                                    //    Toast.LENGTH_SHORT
                                    //).show()
                                    val message = if (isFollowing.value) "关注成功" else "取消关注成功"
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } else {
                                    // 修复：移除不必要的 Elvis 操作符，因为 response.msg 是非空字符串
                                    //Toast.makeText(context, response.msg, Toast.LENGTH_SHORT).show()
                                     coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = response.msg,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                            else -> {
                                 coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = result.exceptionOrNull()?.message ?: "操作失败",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                //Toast.makeText(
                                //    context,
                                //    result.exceptionOrNull()?.message ?: "操作失败",
                                //    Toast.LENGTH_SHORT
                                //).show()
                            }
                        }
                    } catch (e: Exception) {
                        //Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                          coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message =  context.getString(R.string.network_error, e.message),
                                    duration = SnackbarDuration.Short
                                )
                            }
                    }
                }
            },
            modifier = Modifier.weight(1f),
            text = { Text(if (isFollowing.value) "已关注" else "关注") }
        )

        BBQOutlinedButton(
            onClick = onResourcesClick,
            modifier = Modifier.weight(1f),
            text = { Text("${userData.nickname}的资源") }
        )
    }
}

@Composable
private fun StatsCard(
    userData: KtorClient.UserInformationData,
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
    userData: KtorClient.UserInformationData,
    onPostsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatItem(
            count = userData.followerscount,
            label = "关注",
            modifier = Modifier.weight(1f)
        )
        VerticalDivider()
        StatItem(
            count = userData.fanscount,
            label = "粉丝",
            modifier = Modifier.weight(1f)
        )
        VerticalDivider()
        StatItem(
            count = userData.postcount,
            label = "帖子",
            onClick = onPostsClick,
            modifier = Modifier.weight(1f)
        )
        VerticalDivider()
        StatItem(
            count = userData.likecount,
            label = "获赞",
            modifier = Modifier.weight(1f)
        )
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
private fun DetailsCard(userData: KtorClient.UserInformationData) {
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
            InfoItem(label = "硬币:", value = userData.money.toString())
            userData.commentcount?.let {
                InfoItem(label = "评论数:", value = it)
            }
            userData.series_days?.let {
                InfoItem(label = "总签到:", value = "$it 天")
            }
            userData.last_activity_time?.let {
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

@Composable
fun UserDetailScreen(
    userData: KtorClient.UserInformationData?,
    isLoading: Boolean,
    errorMessage: String?,
    onPostsClick: () -> Unit,
    onResourcesClick: (Long) -> Unit,
    onImagePreview: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState // 添加 SnackbarHostState 参数
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
            snackbarHostState = snackbarHostState // 传递 SnackbarHostState
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