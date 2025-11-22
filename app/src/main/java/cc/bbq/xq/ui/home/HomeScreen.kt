//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import android.content.Context
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import cc.bbq.xq.AuthManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.bbq.xq.R
import cc.bbq.xq.ui.theme.AppShapes
import cc.bbq.xq.ui.theme.BBQBackgroundCard
import coil.compose.AsyncImage
import cc.bbq.xq.MainActivity
import cc.bbq.xq.ui.*

@Composable
fun HomeScreen(
    state: HomeState,
    onAvatarClick: () -> Unit,
    onAvatarLongClick: () -> Unit,
    onMessageCenterClick: () -> Unit,
    onBrowseHistoryClick: () -> Unit,
    onMyLikesClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFansClick: () -> Unit,
    onPostsClick: () -> Unit,
    onMyResourcesClick: () -> Unit,
    onBillingClick: () -> Unit,
    onLoginClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPaymentCenterClick: () -> Unit,
    onSignClick: () -> Unit,
    onRecalculateDays: () -> Unit,
    onAboutClick: () -> Unit, // 添加onAboutClick参数
    onAccountProfileClick: () -> Unit, // 新增“账号资料”点击事件
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState//新增ViewModel和snackbar参数
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.setSnackbarHostState(snackbarHostState)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (state.showLoginPrompt) {
                LoginPromptSection(
                    onLoginClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PersonalCenterSection(
                    state = state,
                    onAvatarClick = onAvatarClick,
                    onAvatarLongClick = onAvatarLongClick,
                    onFollowersClick = onFollowersClick,
                    onFansClick = onFansClick,
                    onPostsClick = onPostsClick,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                FunctionCardSection(
                    onPaymentCenterClick = onPaymentCenterClick,
                    onMessageCenterClick = onMessageCenterClick,
                    onBrowseHistoryClick = onBrowseHistoryClick,
                    onMyLikesClick = onMyLikesClick,
                    onSettingsClick = onSettingsClick,
                    onMyResourcesClick = onMyResourcesClick,
                    onBillingClick = onBillingClick,
                    onAboutClick = onAboutClick, // 传递新参数
                    onAccountProfileClick = onAccountProfileClick, // 传递“账号资料”点击事件
                    state = state,
                    onSignClick = onSignClick,
                    onRecalculateDays = onRecalculateDays,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun LoginPromptSection(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "您尚未登录，请登录后使用完整功能",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        TextButton(
            onClick = onLoginClick,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text("立即登录", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun PersonalCenterSection(
    state: HomeState,
    onAvatarClick: () -> Unit,
    onAvatarLongClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFansClick: () -> Unit,
    onPostsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BBQBackgroundCard(
        shape = AppShapes.medium,
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 背景部分
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                // 渐变叠加层
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }

            // 头像
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 10.dp)
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onAvatarClick() },
                            onLongPress = { onAvatarLongClick() }
                        )
                    }
            ) {
                AsyncImage(
                    model = state.avatarUrl,
                    contentDescription = "用户头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.ic_menu_profile)
                )
            }

            // 用户信息
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 112.dp, end = 16.dp, bottom = 16.dp)
            ) {
                // 昵称和等级
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = state.nickname,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = state.level,
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

                // 硬币和ID信息
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "硬币:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = state.coins,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "ID:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = state.userId,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // 统计信息
                StatsRow(
                    followersCount = state.followersCount,
                    fansCount = state.fansCount,
                    postsCount = state.postsCount,
                    likesCount = state.likesCount,
                    onFollowersClick = onFollowersClick,
                    onFansClick = onFansClick,
                    onPostsClick = onPostsClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatsRow(
    followersCount: String,
    fansCount: String,
    postsCount: String,
    likesCount: String,
    onFollowersClick: () -> Unit,
    onFansClick: () -> Unit,
    onPostsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            count = followersCount,
            label = "我的关注",
            onClick = onFollowersClick,
            modifier = Modifier.weight(1f)
        )

        StatItem(
            count = fansCount,
            label = "我的粉丝",
            onClick = onFansClick,
            modifier = Modifier.weight(1f)
        )

        StatItem(
            count = postsCount,
            label = "我的帖子",
            onClick = onPostsClick,
            modifier = Modifier.weight(1f)
        )

        StatItem(
            count = likesCount,
            label = "我的获赞",
            modifier = Modifier.weight(1f)
        )
    }
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionCardSection(
    onMessageCenterClick: () -> Unit,
    onBrowseHistoryClick: () -> Unit,
    onMyLikesClick: () -> Unit,
    onMyResourcesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBillingClick: () -> Unit,
    onPaymentCenterClick: () -> Unit,
    onAboutClick: () -> Unit, // 添加onAboutClick参数
    onAccountProfileClick: () -> Unit, // 新增“账号资料”点击事件
    state: HomeState,
    onSignClick: () -> Unit,
    onRecalculateDays: () -> Unit,
    modifier: Modifier = Modifier
) {
    BBQBackgroundCard(
        shape = AppShapes.medium,
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 签到区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onSignClick() },
                            onLongPress = { onRecalculateDays() }
                        )
                    }
            ) {
                Text(
                    text = state.signStatusMessage ?: "已签到${state.seriesDays}天",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "账号已使用${state.displayDaysDiff}天 经验：${state.exp}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 功能区列表
            FunctionItem(
                icon = R.drawable.ic_menu_message,
                label = "消息中心",
                onClick = onMessageCenterClick
            )

            Divider()

            FunctionItem(
                icon = R.drawable.ic_visibility,
                label = "浏览记录",
                onClick = onBrowseHistoryClick
            )

            Divider()

            FunctionItem(
                icon = R.drawable.heart_favorites_open,
                label = "我喜欢的",
                onClick = onMyLikesClick
            )

            Divider()

            FunctionItem(
                icon = R.drawable.ic_menu_settings,
                label = "主题设置",
                onClick = onSettingsClick
            )

            Divider()

            FunctionItem(
                icon = R.drawable.google_cloud_search,
                label = "我的资源",
                onClick = onMyResourcesClick
            )

            Divider()

            FunctionItem(
                icon = R.drawable.mobills,
                label = "我的账单",
                onClick = onBillingClick
            )

            Divider()

            FunctionItem(
                icon = R.drawable.banking_4a,
                label = "支付中心",
                onClick = onPaymentCenterClick
            )

            Divider()

            FunctionItem(
                icon = R.drawable.ic_info_outline,
                label = "关于本程序",
                onClick = onAboutClick // 使用新参数
            )

            Divider()

            // 新增“账号资料”选项
            FunctionItem(
                icon = Icons.Filled.Person, // 使用 Material 图标
                label = "账号资料",
                onClick = onAccountProfileClick // 使用新参数
            )
        }
    }
}

@Composable
private fun FunctionItem(
    icon: Any, // 修改为 Any 类型
    label: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val itemHeight = 48.dp
    val iconSize = 45.dp
    val iconMarginEnd = 16.dp
    val arrowSize = 24.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight)
            .padding(horizontal = 16.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 主图标
        when (icon) {
            is Int -> {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                        .padding(end = iconMarginEnd),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            is ImageVector -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                        .padding(end = iconMarginEnd),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            else -> {
                // 处理其他类型的 icon，或者抛出异常
                throw IllegalArgumentException("Unsupported icon type: ${icon::class.java}")
            }
        }

        // 标签文本
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // 右侧箭头
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(arrowSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Divider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    startIndent: Dp = 56.dp
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .padding(start = startIndent)
            .background(MaterialTheme.colorScheme.outline)
    )
}

data class HomeState(
    val showLoginPrompt: Boolean = false,
    val isLoading: Boolean = false,
    val avatarUrl: String? = null,
    val nickname: String = "BBQ用户",
    val level: String = "LV0",
    val coins: String = "未知",
    val userId: String = "未知",
    val followersCount: String = "0",
    val fansCount: String = "0",
    val postsCount: String = "0",
    val likesCount: String = "0",
    // 添加签到相关状态
    val seriesDays: Int = 0,
    val signStatusMessage: String? = null,
    val exp: Int = 0,
    val displayDaysDiff: Int = 0
)
