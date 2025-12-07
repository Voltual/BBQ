// 添加缺失的导入
package cc.bbq.xq.ui.user

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape // 添加这个导入
import androidx.compose.material.icons.Icons // 添加这个导入
import androidx.compose.material.icons.filled.Comment // 添加这个导入
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color // 添加这个导入
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController // 添加这个导入
import cc.bbq.xq.AppStore
import cc.bbq.xq.data.unified.UnifiedComment // 添加这个导入
import cc.bbq.xq.ui.AppDetail // 添加这个导入
import cc.bbq.xq.ui.UserDetail // 添加这个导入
import cc.bbq.xq.ui.theme.*
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyCommentsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MyCommentsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedStore by viewModel.selectedStore.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部标题和商店切换
        AppStoreSelectorCard(
            selectedStore = selectedStore,
            onStoreChange = { newStore ->
                // 如果商店类型改变，切换到新商店
                if (newStore != selectedStore) {
                    viewModel.switchStore(newStore)
                }
            },
            title = "我的评论",
            description = "查看您在所有商店发布的评论"
        )

        Spacer(Modifier.height(16.dp))

        // 评论列表
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && comments.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (comments.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "暂无评论",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (error != null) "加载失败" else "暂无评论",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (error != null) {
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(comments) { comment ->
                        MyCommentItem(
                            comment = comment,
                            onUserClick = { userId ->
                                // 跳转到用户详情页
                                val userDetailRoute = UserDetail(
                                    userId = userId.toLong(),
                                    store = selectedStore
                                ).createRoute()
                                navController.navigate(userDetailRoute)
                            },
                            onOpenApp = { appId, versionId ->
                                // 跳转到应用详情页
                                val appDetailRoute = AppDetail(
                                    appId = appId,
                                    versionId = versionId,
                                    storeName = selectedStore.name
                                ).createRoute()
                                navController.navigate(appDetailRoute)
                            },
                            onOpenUrl = { url ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // 这里不能直接调用 @Composable 函数，需要使用 SnackbarHostState
                                    // 暂时忽略错误处理
                                }
                            }
                        )
                    }
                }
            }

            // 加载更多指示器
            if (isLoading && comments.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)) // 临时使用固定颜色
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("加载更多...")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyCommentItem(
    comment: UnifiedComment,
    onUserClick: (String) -> Unit,
    onOpenApp: (String, Long) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 评论头部
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = comment.sender.avatarUrl,
                    contentDescription = "用户头像",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onUserClick(comment.sender.id) },
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.sender.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatDate(comment.sendTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 评论内容
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )

            // 被删除的评论提示
            if (comment.appId == null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "评论的应用已被删除",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // 回复的评论
            if (comment.fatherReply != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "回复 @${comment.fatherReply.sender.displayName}：",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = comment.fatherReply.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 操作按钮
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onUserClick(comment.sender.id) },
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("查看用户")
                }

                // 如果评论关联了应用，显示查看应用按钮
                comment.appId?.let { appId ->
                    Button(
                        onClick = { 
                            val versionId = comment.versionId ?: 0L
                            onOpenApp(appId, versionId) 
                        },
                        shape = AppShapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("查看应用")
                    }
                }
            }
        }
    }
}

// 格式化时间
fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(date)
}
                  ```
                  </content>                  ## user sent a file: Mappers.kt
                  <content>
                  ```
                  // /app/src/main/java/cc/bbq/xq/data/unified/Mappers.kt
package cc.bbq.xq.data.unified

import cc.bbq.xq.AppStore
import cc.bbq.xq.KtorClient
import cc.bbq.xq.SineShopClient
import cc.bbq.xq.KtorClient.AppItem
import cc.bbq.xq.KtorClient.Comment
import cc.bbq.xq.KtorClient.AppComment
import cc.bbq.xq.KtorClient.AppDetail
import cc.bbq.xq.KtorClient.UserInformationData
import cc.bbq.xq.SineShopClient.SineShopApp
import cc.bbq.xq.SineShopClient.SineShopUserInfoLite
import cc.bbq.xq.SineShopClient.SineShopComment
import cc.bbq.xq.SineShopClient.SineShopAppDetail
import cc.bbq.xq.SineShopClient.AppTag
import cc.bbq.xq.SineShopClient.SineShopDownloadSource
import cc.bbq.xq.SineShopClient.SineShopUserInfo

// --- KtorClient (小趣空间) Mappers ---

fun KtorClient.AppItem.toUnifiedAppItem(): UnifiedAppItem {
    return UnifiedAppItem(
        uniqueId = "${AppStore.XIAOQU_SPACE}-${this.id}-${this.apps_version_id}",
        navigationId = this.id.toString(),
        navigationVersionId = this.apps_version_id,
        store = AppStore.XIAOQU_SPACE,
        name = this.appname,
        iconUrl = this.app_icon,
        versionName = ""
    )
}

fun createUnifiedUserFromKtor(id: Long, name: String, avatar: String): UnifiedUser {
    return UnifiedUser(
        id = id.toString(),
        displayName = name,
        avatarUrl = avatar
    )
}

fun KtorClient.Comment.toUnifiedComment(): UnifiedComment {
    return UnifiedComment(
        id = this.id.toString(),
        content = this.content,
        sendTime = this.time.toLongOrNull() ?: 0L,
        sender = createUnifiedUserFromKtor(this.userid, this.nickname, this.usertx),
        childCount = this.sub_comments_count,
        // KtorClient.Comment 没有直接包含子评论列表，设为空
        childComments = emptyList(), 
        fatherReply = null,
        raw = this
    )
}

fun KtorClient.AppComment.toUnifiedComment(): UnifiedComment {
    val father = if (this.parentid != null && this.parentid != 0L) {
        UnifiedComment(
            id = this.parentid.toString(),
            content = this.parentcontent ?: "",
            sendTime = 0L,
            sender = createUnifiedUserFromKtor(0L, this.parentnickname ?: "未知用户", ""),
            childCount = 0,
            fatherReply = null,
            raw = this
        )
    } else null

    return UnifiedComment(
        id = this.id.toString(),
        content = this.content,
        sendTime = this.time.toLongOrNull() ?: 0L,
        sender = createUnifiedUserFromKtor(this.userid, this.nickname, this.usertx),
        childCount = 0,
        fatherReply = father,
        raw = this
    )
}

fun KtorClient.AppDetail.toUnifiedAppDetail(): UnifiedAppDetail {
    return UnifiedAppDetail(
        id = this.id.toString(),
        store = AppStore.XIAOQU_SPACE,
        packageName = "", 
        name = this.appname,
        versionCode = 0L,
        versionName = this.version,
        iconUrl = this.app_icon,
        type = this.category_name,
        previews = this.app_introduction_image_array,
        description = this.app_introduce,
        updateLog = this.app_explain,
        developer = null,
        size = this.app_size,
        uploadTime = this.create_time.toLongOrNull() ?: 0L,
        user = createUnifiedUserFromKtor(this.userid, this.nickname, this.usertx),
        tags = listOf(this.category_name, this.sub_category_name),
        downloadCount = this.download_count,
        isFavorite = false,
        favoriteCount = 0,
        reviewCount = this.comment_count,
        downloadUrl = if (this.is_pay == 0 || this.is_user_pay) this.download else null,
        raw = this
    )
}

// --- SineShopClient (弦应用商店) Mappers ---

fun SineShopClient.SineShopApp.toUnifiedAppItem(): UnifiedAppItem {
    return UnifiedAppItem(
        uniqueId = "${AppStore.SIENE_SHOP}-${this.id}",
        navigationId = this.id.toString(),
        navigationVersionId = 0L,
        store = AppStore.SIENE_SHOP,
        name = this.app_name,
        iconUrl = this.app_icon,
        versionName = this.version_name
    )
}

fun SineShopClient.SineShopUserInfoLite.toUnifiedUser(): UnifiedUser {
    return UnifiedUser(
        id = this.id.toString(),
        displayName = this.displayName,
        avatarUrl = this.userAvatar
    )
}

fun SineShopClient.SineShopComment.toUnifiedComment(): UnifiedComment {
    return UnifiedComment(
        id = this.id.toString(),
        content = this.content,
        sendTime = this.send_time,
        sender = this.sender.toUnifiedUser(),
        childCount = this.child_count,
        fatherReply = this.father_reply?.toUnifiedComment(),
        raw = this,
        // 弦应用商店的评论不直接包含应用ID，需要在 Repository 层设置
        appId = null,
        versionId = null
    )
}

fun SineShopClient.SineShopAppDetail.toUnifiedAppDetail(): UnifiedAppDetail {
    return UnifiedAppDetail(
        id = this.id.toString(),
        store = AppStore.SIENE_SHOP,
        packageName = this.package_name,
        name = this.app_name,
        versionCode = this.version_code.toLong(),
        versionName = this.version_name,
        iconUrl = this.app_icon,
        type = this.app_type,
        previews = this.app_previews,
        description = this.app_describe,
        updateLog = this.app_update_log,
        developer = this.app_developer,
        size = this.download_size,
        uploadTime = this.upload_time,
        user = this.user.toUnifiedUser(),
        tags = this.tags?.map { it.name },
        downloadCount = this.download_count,
        isFavorite = this.is_favourite == 1,
        favoriteCount = this.favourite_count,
        reviewCount = this.review_count,
        downloadUrl = null,
        raw = this
    )
}

fun SineShopClient.AppTag.toUnifiedCategory(): UnifiedCategory {
    return UnifiedCategory(
        id = this.id.toString(),
        name = this.name,
        icon = this.icon
    )
}

fun SineShopClient.SineShopDownloadSource.toUnifiedDownloadSource(): UnifiedDownloadSource {
    return UnifiedDownloadSource(
        name = this.name,
        url = this.url,
        isOfficial = this.isExtra == 1 // 假设 1 代表官方/主要线路
    )
}

fun SineShopClient.SineShopUserInfo.toUnifiedUser(): UnifiedUser {
    return UnifiedUser(
        id = this.id.toString(),
        displayName = this.displayName,
        avatarUrl = this.userAvatar
    )
}

// 修正 KtorClient (小趣空间) 映射中的类型错误
fun KtorClient.UserInformationData.toUnifiedUserDetail(): UnifiedUserDetail {
    return UnifiedUserDetail(
        id = this.id,
        username = this.username,
        displayName = this.nickname,
        avatarUrl = this.usertx,
        description = null, // 小趣空间无此字段
        hierarchy = this.hierarchy,
        followersCount = this.fanscount,
        fansCount = this.followerscount,
        postCount = this.postcount,
        likeCount = this.likecount,
        money = this.money, // Int 类型，正确
        commentCount = this.commentcount?.toIntOrNull(), // 从 String? 转换为 Int?
        seriesDays = this.series_days,
        lastActivityTime = this.last_activity_time,
        store = AppStore.XIAOQU_SPACE,
        raw = this
    )
}

// 修正 SineShopClient (弦应用商店) 映射
fun SineShopClient.SineShopUserInfo.toUnifiedUserDetail(): UnifiedUserDetail {
    return UnifiedUserDetail(
        id = this.id.toLong(),
        username = this.username,
        displayName = this.displayName,
        avatarUrl = this.userAvatar,
        description = this.userDescribe,
        userOfficial = this.userOfficial,
        userBadge = this.userBadge,
        userStatus = this.userStatus,
        userStatusReason = this.userStatusReason,
        banTime = this.banTime?.toLong(),
        joinTime = this.joinTime,
        userPermission = this.userPermission,
        bindQq = this.bindQq,
        bindEmail = this.bindEmail,
        bindBilibili = this.bindBilibili,
        verifyEmail = this.verifyEmail,
        lastLoginDevice = this.lastLoginDevice,
        lastOnlineTime = this.lastOnlineTime,
        uploadCount = this.uploadCount,
        replyCount = this.replyCount,
        store = AppStore.SIENE_SHOP,
        raw = this
    )
}