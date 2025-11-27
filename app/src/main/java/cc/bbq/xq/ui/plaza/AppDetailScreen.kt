//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.plaza

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import cc.bbq.xq.ui.theme.BBQSnackbarHost // 导入 BBQSnackbarHost
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import cc.bbq.xq.KtorClient
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cc.bbq.xq.ui.*
import cc.bbq.xq.ui.community.compose.CommentDialog
import cc.bbq.xq.ui.community.compose.CommentItem
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import cc.bbq.xq.AppStore
import cc.bbq.xq.SineShopClient
import cc.bbq.xq.util.cleanUrl

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppDetailScreen(
    viewModel: AppDetailComposeViewModel,
    appId: Long,
    versionId: Long,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appDetail by viewModel.appDetail.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val showCommentDialog by viewModel.showCommentDialog.collectAsState()
    val showReplyDialog by viewModel.showReplyDialog.collectAsState()
    val currentReplyComment by viewModel.currentReplyComment.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val appStore by viewModel.appStore.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 简化的初始化 - 只调用 ViewModel 的初始化方法
    LaunchedEffect(appId, versionId) {
        viewModel.initializeData(appId, versionId, appStore)
    }

    // 下拉刷新状态
    var refreshing by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = {
        refreshing = true
        viewModel.refresh()
        refreshing = false
    })

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }

    fun shareApp() {
        val postUrl = when (val detail = appDetail) {
            is KtorClient.AppDetail -> detail.posturl
            is SineShopClient.SineShopAppDetail -> "" // 弦应用商店没有分享链接
            else -> null
        }

        if (!postUrl.isNullOrBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("应用链接", postUrl)
            clipboard.setPrimaryClip(clip)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("已复制应用链接: $postUrl")
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("分享链接无效")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (appDetail != null) {
            AppDetailContent(
                navController = navController,
                appDetail = appDetail,
                comments = comments,
                appStore = appStore,
                onCommentReply = { comment ->
                    viewModel.openReplyDialog(comment)
                },
                onDownload = { downloadUrl ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("无法打开下载链接")
                        }
                    }
                },
                onCommentDelete = { commentId ->
                    viewModel.deleteAppComment(commentId)
                },
                onUpdateClick = {
                    val detail = appDetail
                    if (detail is KtorClient.AppDetail) {
                        // 使用 KtorClient 的 JsonConverter
                        val appDetailJson = KtorClient.JsonConverter.toJson(detail)
                        navController.navigate(UpdateAppRelease(appDetailJson).createRoute())
                    }
                },
                onRefundClick = {
                    val detail = appDetail
                    if (detail is KtorClient.AppDetail) {
                        val destination = CreateRefundPost(
                            appId = detail.id,
                            versionId = detail.apps_version_id,
                            appName = detail.appname,
                            payMoney = detail.pay_money
                        )
                        navController.navigate(destination.createRoute())
                    }
                },
                onDeleteClick = {
                    showDeleteConfirmDialog = true
                },
                onShareClick = {
                    shareApp()
                },
                onImagePreview = { imageUrl ->
                    navController.navigate(
                        ImagePreview(
                            imageUrl = imageUrl
                        ).createRoute()
                    )
                }
            )
        }

        // 浮动评论按钮
        FloatingActionButton(
            onClick = { viewModel.openCommentDialog() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Comment, "评论")
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.surface
        )
    }

    // 评论对话框
    if (showCommentDialog) {
        CommentDialog(
            hint = "输入评论内容...",
            onDismiss = { viewModel.closeCommentDialog() },
            context = LocalContext.current,
            onSubmit = { content, imageUrl ->
                viewModel.submitAppComment(content, imageUrl)
            }
        )
    }

    if (showReplyDialog && currentReplyComment != null) {
        CommentDialog(
            hint = "回复 @${
                when (val comment = currentReplyComment) {
                    is KtorClient.Comment -> comment.nickname
                    is SineShopClient.SineShopComment -> comment.sender.displayName
                    else -> ""
                }
            }",
            onDismiss = { viewModel.closeReplyDialog() },
            context = LocalContext.current,
            onSubmit = { content, imageUrl ->
                viewModel.submitAppComment(content, imageUrl)
            }
        )
    }

    // 删除确认对话框
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除") },
            text = { Text("您确定要删除此应用吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteApp {
                            // 删除成功后不需要手动调用 onAppDeleted，因为 NavGraph 会处理
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant, // 设置对话框背景色
            titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant, // 设置标题文字颜色
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant // 设置文本内容颜色
        )
    }

    // Snackbar 宿主
    Box(modifier = Modifier.fillMaxSize()) {
        BBQSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun AppDetailContent(
    navController: NavController,
    appDetail: Any?, // 使用 Any? 类型
    comments: List<Any>, // 使用 Any 类型
    appStore: AppStore,
    onCommentReply: (Any) -> Unit, // 使用 Any 类型
    onDownload: (String) -> Unit,
    onCommentDelete: (Long) -> Unit,
    onUpdateClick: () -> Unit,
    onRefundClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onImagePreview: (String) -> Unit
) {
    val context = navController.context
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    // 将下拉菜单状态移到内容组件内部
    var showMoreMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // 使用 surfaceVariant 背景色
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        AsyncImage(
                            model = when (val detail = appDetail) {
                                is KtorClient.AppDetail -> detail.app_icon
                                is SineShopClient.SineShopAppDetail -> detail.app_icon
                                else -> ""
                            },
                            contentDescription = "应用图标",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val imageUrl = when (val detail = appDetail) {
                                        is KtorClient.AppDetail -> detail.app_icon
                                        is SineShopClient.SineShopAppDetail -> detail.app_icon
                                        else -> ""
                                    }
                                    if (imageUrl.isNotEmpty()) {
                                        onImagePreview(imageUrl)
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (val detail = appDetail) {
                                    is KtorClient.AppDetail -> detail.appname
                                    is SineShopClient.SineShopAppDetail -> detail.app_name
                                    else -> "应用名称"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "版本: ${
                                    when (val detail = appDetail) {
                                        is KtorClient.AppDetail -> detail.version
                                        is SineShopClient.SineShopAppDetail -> detail.version_name
                                        else -> ""
                                    }
                                }",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "大小: ${
                                    when (val detail = appDetail) {
                                        is KtorClient.AppDetail -> detail.app_size
                                        is SineShopClient.SineShopAppDetail -> detail.download_size
                                        else -> ""
                                    }
                                }MB",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 更多菜单按钮与下拉菜单正确集成
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true }
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                            }

                            // 下拉菜单直接与按钮关联
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                val detail = appDetail
                                if (detail is KtorClient.AppDetail) {
                                    DropdownMenuItem(
                                        text = { Text("更新") },
                                        onClick = {
                                            showMoreMenu = false
                                            onUpdateClick()
                                        }
                                    )
                                    if (detail.is_pay == 1) {
                                        DropdownMenuItem(
                                            text = { Text("退币") },
                                            onClick = {
                                                showMoreMenu = false
                                                onRefundClick()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("删除") },
                                        onClick = {
                                            showMoreMenu = false
                                            onDeleteClick()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("分享") },
                                    onClick = {
                                        showMoreMenu = false
                                        onShareClick()
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val downloadUrl = when (val detail = appDetail) {
                                is KtorClient.AppDetail ->  if (detail.is_pay == 0 || detail.is_user_pay) detail.download else null
                                is SineShopClient.SineShopAppDetail -> {
                                    // 弦应用商店没有直接下载链接，这里可以显示一个提示或者跳转到应用详情页
                                    ""
                                }
                                else -> null
                            }

                            if (!downloadUrl.isNullOrBlank()) {
                                onDownload(downloadUrl)
                            } else {
                                when (val detail = appDetail) {
                                    is KtorClient.AppDetail -> {
                                        if (detail.is_pay == 1 && !detail.is_user_pay) {
                                            val destination = PaymentForApp(
                                                appId = detail.id,
                                                appName = detail.appname,
                                                versionId = detail.apps_version_id,
                                                price = detail.pay_money,
                                                iconUrl = detail.app_icon,
                                                previewContent = detail.app_introduce?.take(30) ?: ""
                                            )
                                            navController.navigate(destination.createRoute())
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("下载链接无效")
                                            }
                                        }
                                    }
                                    else ->  coroutineScope.launch {
                                        snackbarHostState.showSnackbar("下载链接无效")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = appStore == AppStore.XIAOQU_SPACE || appDetail is SineShopClient.SineShopAppDetail
                    ) {
    Icon(Icons.Filled.Download, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text(
        text = when (val detail = appDetail) {
            is KtorClient.AppDetail -> if (detail.is_pay == 0 || detail.is_user_pay) "下载应用" else "购买应用 (${detail.pay_money}硬币)"
            is SineShopClient.SineShopAppDetail -> "查看详情"
            else -> "下载应用"
        }
    )
}
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // 使用 surfaceVariant 背景色
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "应用介绍",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = when (val detail = appDetail) {
                            is KtorClient.AppDetail -> detail.app_introduce?.replace("<br>", "\n") ?: "暂无介绍"
                            is SineShopClient.SineShopAppDetail -> detail.app_describe ?: "暂无介绍"
                            else -> "暂无介绍"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // 使用 surfaceVariant 背景色
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "应用信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    when (val detail = appDetail) {
                        is KtorClient.AppDetail -> {
                            Text(
                                text = "更新时间: ${detail.update_time}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "创建时间: ${detail.create_time}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "子分类: ${detail.sub_category_name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "应用说明: ${detail.app_explain}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is SineShopClient.SineShopAppDetail -> {
                            Text(
                                text = "上传时间: ${detail.upload_time}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "更新时间: ${detail.update_time}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "应用来源: ${detail.app_source}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "应用开发者: ${detail.app_developer}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // 使用 surfaceVariant 背景色
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            val userId = when (val detail = appDetail) {
                                is KtorClient.AppDetail -> detail.userid
                                is SineShopClient.SineShopAppDetail -> detail.user.id.toLong()
                                else -> -1L
                            }
                            // 修复：点击作者头像跳转到用户详情页
                            if (userId != -1L) {
                                navController.navigate(UserDetail(userId).createRoute())
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = when (val detail = appDetail) {
                            is KtorClient.AppDetail -> detail.usertx
                            is SineShopClient.SineShopAppDetail -> detail.user_avatar
                            else -> ""
                        },
                        contentDescription = "用户头像",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                val imageUrl = when (val detail = appDetail) {
                                    is KtorClient.AppDetail -> detail.usertx
                                    is SineShopClient.SineShopAppDetail -> detail.user_avatar ?: ""
                                    else -> ""
                                }
                                if (imageUrl.isNotEmpty()) {
                                    onImagePreview(imageUrl)
                                }
                            },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when (val detail = appDetail) {
                                is KtorClient.AppDetail -> detail.nickname
                                is SineShopClient.SineShopAppDetail -> detail.user.displayName
                                else -> "用户名"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "查看用户详情",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (appDetail is KtorClient.AppDetail && !appDetail.app_introduction_image_array.isNullOrEmpty()) {
            item {
                Column {
                    Text(
                        text = "应用截图",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(appDetail.app_introduction_image_array) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "应用截图",
                                modifier = Modifier
                                    .height(200.dp)
                                    .width(300.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onImagePreview(imageUrl) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        } else if (appDetail is SineShopClient.SineShopAppDetail && !appDetail.app_previews.isNullOrEmpty()) {
            item {
                Column {
                    Text(
                        text = "应用截图",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(appDetail.app_previews) { imageUrl ->
                            AsyncImage(
                                model = imageUrl.cleanUrl(),
                                contentDescription = "应用截图",
                                modifier = Modifier
                                    .height(200.dp)
                                    .width(300.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onImagePreview(imageUrl) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "用户评论 (${comments.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (comments.isNotEmpty()) {
            items(comments) { comment ->
                CommentItem(
                    comment = when (comment) {
                        is KtorClient.Comment -> comment
                        is SineShopClient.SineShopComment -> KtorClient.Comment(
                            id = comment.id.toLong(),
                            content = comment.content,
                            userid = comment.sender.id.toLong(),
                            time = comment.send_time.toString(),
                            username = comment.sender.username,
                            nickname = comment.sender.displayName,
                            usertx = comment.sender.user_avatar ?: "",
                            hierarchy = "0",
                            parentid = comment.father_reply_id.toLong(),
                            parentnickname = comment.father_reply?.sender?.displayName ?: "",
                            parentcontent = comment.father_reply?.content ?: "",
                            image_path = null,
                            sub_comments_count = comment.child_count
                        )
                        else -> KtorClient.Comment(0,"","","","","","",0,0,"","","",0)
                    },
                    navController = navController,
                    onReply = { onCommentReply(comment) },
                    onDelete = {
                        val commentId = when (comment) {
                            is KtorClient.Comment -> comment.id
                            is SineShopClient.SineShopComment -> comment.id.toLong()
                            else -> 0L
                        }
                        onCommentDelete(commentId)
                    },
                    clipboardManager = clipboardManager,
                    snackbarHostState = snackbarHostState, // 传递 SnackbarHostState
                    context = context
                )
            }
        } else {
            item {
                Text(
                    text = "暂无评论",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}