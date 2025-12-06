// 文件路径: cc/bbq.xq.ui/plaza/AppDetailScreen.kt
package cc.bbq.xq.ui.plaza

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// 修复：从正确的包导入 pullRefresh 相关组件
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cc.bbq.xq.data.unified.UnifiedAppDetail
import cc.bbq.xq.data.unified.UnifiedComment
import cc.bbq.xq.ui.ImagePreview
import cc.bbq.xq.ui.UserDetail
import cc.bbq.xq.ui.community.compose.CommentDialog
import cc.bbq.xq.ui.community.compose.CommentItem
import cc.bbq.xq.ui.theme.BBQSnackbarHost
import cc.bbq.xq.ui.theme.DownloadSourceDrawer
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import cc.bbq.xq.ui.Download // 确保导入 Download
import cc.bbq.xq.AppStore
import cc.bbq.xq.util.formatTimestamp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@OptIn(ExperimentalMaterialApi::class) // 保留 ExperimentalMaterialApi 注解
@Composable
fun AppDetailScreen(
    appId: String,
    versionId: Long,
    storeName: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AppDetailComposeViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val appDetail by viewModel.appDetail.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val showCommentDialog by viewModel.showCommentDialog.collectAsState()
    val showReplyDialog by viewModel.showReplyDialog.collectAsState()
    val currentReplyComment by viewModel.currentReplyComment.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val showDownloadDrawer by viewModel.showDownloadDrawer.collectAsState()
    val downloadSources by viewModel.downloadSources.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 应用删除确认对话框
    var showDeleteAppDialog by remember { mutableStateOf(false) }

    // 评论删除确认对话框
    var showDeleteCommentDialog by remember { mutableStateOf(false) }  // 修正：使用 Boolean
    var commentToDeleteId by remember { mutableStateOf<String?>(null) } // 修正：使用 String?

    LaunchedEffect(appId, versionId, storeName) {
        viewModel.initializeData(appId, versionId, storeName)
    }

    LaunchedEffect(Unit) {
        viewModel.openUrlEvent.collectLatest { url ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("无法打开链接: $url")
            }
        }
    }
    
    // 监听 ViewModel 中的 snackbarEvent
    LaunchedEffect(viewModel.snackbarEvent) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    // 监听 ViewModel 中的 navigateToDownloadEvent
    LaunchedEffect(viewModel.navigateToDownloadEvent) {
        viewModel.navigateToDownloadEvent.collectLatest { navigate ->
            if (navigate) {
                navController.navigate(Download.route)  // 导航到下载管理页面
            }
        }
    }

    var refreshing by remember { mutableStateOf(false) }
    // 修复：使用正确的 rememberPullRefreshState
    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = {
        refreshing = true
        viewModel.refresh()
        refreshing = false
    })

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            coroutineScope.launch { snackbarHostState.showSnackbar(errorMessage) }
        }
    }

    // 修复：使用 Modifier.pullRefresh 包装内容
    Box(modifier = modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (appDetail != null) {
            // 弦应用商店才显示版本列表
            val pageCount = if (appDetail!!.store == AppStore.SIENE_SHOP) 2 else 1
            val pagerState = rememberPagerState(pageCount = { pageCount })
            
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> {
                        // 应用详情页面
                        AppDetailContent(
                            navController = navController,
                            appDetail = appDetail!!,
                            comments = comments,
                            onCommentReply = { viewModel.openReplyDialog(it) },
                            onDownloadClick = { viewModel.handleDownloadClick() },
                            onCommentLongClick = { commentId ->
                                commentToDeleteId = commentId
                                showDeleteCommentDialog = true
                            },
                            onDeleteAppClick = { showDeleteAppDialog = true },
                            onImagePreview = { url -> navController.navigate(ImagePreview(url).createRoute()) }
                        )
                    }
                    1 -> {
                        // 版本列表页面
                        if (appDetail!!.store == AppStore.SIENE_SHOP) {
                            VersionListScreen(
                                appId = appDetail!!.id.toInt(),
                                onVersionSelected = { version ->
                                    // 切换到详情页面并更新版本
                                    // 这里可以调用 viewModel 的方法来切换到详情页面并更新版本
                                    // 但由于我们已经移除了版本列表逻辑，这里暂时只显示 Snackbar
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("选择了版本: ${version.versionName}")
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // 如果不是弦应用商店，显示错误信息或者空内容
                            Text("版本列表仅在弦应用商店提供")
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.openCommentDialog() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.AutoMirrored.Filled.Comment, "评论")
        }

        // 修复：使用语义颜色
        PullRefreshIndicator(
            refreshing, 
            pullRefreshState, 
            Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface, // 使用语义颜色
            contentColor = MaterialTheme.colorScheme.primary // 使用语义颜色
        )
        BBQSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    DownloadSourceDrawer(
        show = showDownloadDrawer,
        onDismissRequest = { viewModel.closeDownloadDrawer() },
        sources = downloadSources,
        onSourceSelected = { source ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                context.startActivity(intent)
            } catch (e: Exception) {
                coroutineScope.launch { snackbarHostState.showSnackbar("无法打开链接") }
            }
        }
    )

    if (showCommentDialog) {
        CommentDialog(
            hint = "输入评论...",
            onDismiss = { viewModel.closeCommentDialog() },
            context = context,
            onSubmit = { content, _ -> viewModel.submitComment(content) }
        )
    }

    if (showReplyDialog && currentReplyComment != null) {
        CommentDialog(
            hint = "回复 @${currentReplyComment!!.sender.displayName}",
            onDismiss = { viewModel.closeReplyDialog() },
            context = context,
            onSubmit = { content, _ -> viewModel.submitComment(content) }
        )
    }

    // 删除应用确认对话框
    if (showDeleteAppDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAppDialog = false },
            title = { Text("确认删除应用") },
            text = { Text("确定要删除此应用吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAppDialog = false
                    viewModel.deleteApp { navController.popBackStack() }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAppDialog = false }) { Text("取消") } }
        )
    }

    // 删除评论确认对话框
    if (showDeleteCommentDialog && commentToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteCommentDialog = false },
            title = { Text("确认删除评论") },
            text = { Text("确定要删除这条评论吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteCommentDialog = false
                    commentToDeleteId?.let { viewModel.deleteComment(it) }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteCommentDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
fun AppDetailContent(
    navController: NavController,
    appDetail: UnifiedAppDetail,
    comments: List<UnifiedComment>,
    onCommentReply: (UnifiedComment) -> Unit,
    onDownloadClick: () -> Unit,
    onCommentLongClick: (String) -> Unit, // 修改参数名，更清晰
    onDeleteAppClick: () -> Unit, // 修改参数名，区分删除应用和删除评论
    onImagePreview: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 应用头部信息 ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = appDetail.iconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                                .clickable { onImagePreview(appDetail.iconUrl) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(appDetail.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("版本: ${appDetail.versionName}", style = MaterialTheme.typography.bodyMedium)
                            Text("大小: ${appDetail.size}", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = onDeleteAppClick) {
                            Icon(Icons.Default.MoreVert, "更多")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text("下载应用")
                    }
                }
            }
        }

        // --- 更新日志（弦应用商店） ---
        if (appDetail.store == AppStore.SIENE_SHOP && !appDetail.updateLog.isNullOrEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("更新日志", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(appDetail.updateLog!!)
                    }
                }
            }
        }

        // --- 适配说明（小趣空间） ---
        if (appDetail.store == AppStore.XIAOQU_SPACE) {
            val appExplain = when (val raw = appDetail.raw) {
                is cc.bbq.xq.KtorClient.AppDetail -> raw.app_explain
                else -> null
            }
            
            if (!appExplain.isNullOrEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("适配说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(appExplain)
                        }
                    }
                }
            }
        }

        // --- 应用信息卡片 ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("应用信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    // 根据不同商店显示不同的信息字段
                    when (appDetail.store) {
                        AppStore.XIAOQU_SPACE -> {
                            // 小趣空间信息
                            val raw = appDetail.raw as? cc.bbq.xq.KtorClient.AppDetail
                            
                            InfoRow(
                                label = "应用类型",
                                value = appDetail.type
                            )
                            InfoRow(
                                label = "下载次数",
                                value = "${appDetail.downloadCount} 次"
                            )
                            if (appDetail.size != null) {
                                InfoRow(
                                    label = "安装包大小",
                                    value = appDetail.size
                                )
                            }
                            InfoRow(
                                label = "上传时间",
                                value = raw?.create_time ?: "未知"
                            )
                            InfoRow(
                                label = "更新时间",
                                value = raw?.update_time ?: "未知"
                            )
                        }
                        AppStore.SIENE_SHOP -> {
                            // 弦应用商店信息
                            val raw = appDetail.raw as? cc.bbq.xq.SineShopClient.SineShopAppDetail
                            val deviceInfo = getDeviceInfo(raw?.app_sdk_min ?: 0)
                            
                            InfoRow(
                                label = "应用类型",
                                value = appDetail.type
                            )
                            InfoRow(
                                label = "版本类型",
                                value = raw?.app_version_type ?: "未知"
                            )
                            
                            // 支持系统信息（包含最低SDK、目标SDK和设备兼容性）
                            val supportSystem = buildString {
                                append("Android ${raw?.app_sdk_min ?: "未知"}")
                                if (raw?.app_sdk_target != null && raw.app_sdk_target != raw.app_sdk_min) {
                                    append(" (目标SDK: ${raw.app_sdk_target})")
                                }
                                append(" • ")
                                append(deviceInfo)
                            }
                            InfoRow(
                                label = "支持系统",
                                value = supportSystem
                            )
                            
                            if (appDetail.size != null) {
                                InfoRow(
                                    label = "安装包大小",
                                    value = appDetail.size
                                )
                            }
                            InfoRow(
                                label = "下载次数",
                                value = "${appDetail.downloadCount} 次"
                            )
                            InfoRow(
                                label = "应用开发者",
                                value = raw?.app_developer ?: "未知"
                            )
                            InfoRow(
                                label = "应用来源",
                                value = raw?.app_source ?: "未知"
                            )
                            InfoRow(
                                label = "上传时间",
                                value = if (raw?.upload_time != null) formatTimestamp(raw.upload_time) else "未知"
                            )
                            InfoRow(
                                label = "资料时间",
                                value = if (raw?.update_time != null) formatTimestamp(raw.update_time) else "未知"
                            )
                            
                            // 显示应用标签
                            if (!raw?.tags.isNullOrEmpty()) {
                                InfoRow(
                                    label = "应用标签",
                                    value = raw?.tags?.joinToString(", ") { it.name } ?: ""
                                )
                            }
                            
                            // 显示审核状态（如果有审核失败的情况）
                            if (raw?.audit_status == 0 && !raw.audit_reason.isNullOrEmpty()) {
                                InfoRow(
                                    label = "审核状态",
                                    value = raw.audit_reason
                                )
                            }
                        }
                        else -> {
                            // 其他商店的通用信息
                            InfoRow(
                                label = "应用类型",
                                value = appDetail.type
                            )
                            if (appDetail.size != null) {
                                InfoRow(
                                    label = "安装包大小",
                                    value = appDetail.size
                                )
                            }
                            InfoRow(
                                label = "下载次数",
                                value = "${appDetail.downloadCount} 次"
                            )
                            InfoRow(
                                label = "上传时间",
                                value = appDetail.uploadTime.toString()
                            )
                        }
                    }
                }
            }
        }

        // --- 应用介绍 ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("应用介绍", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(appDetail.description ?: "暂无介绍")
                }
            }
        }

        // --- 应用截图 ---
        if (!appDetail.previews.isNullOrEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("应用截图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(appDetail.previews) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onImagePreview(url) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 作者信息 ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("作者信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    when (appDetail.store) {
                        AppStore.SIENE_SHOP -> {
                            // 弦应用商店：同时显示上传者和审核员
                            val raw = appDetail.raw as? cc.bbq.xq.SineShopClient.SineShopAppDetail
                            
                            // 上传者信息
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val userId = raw?.user?.id
                                        if (userId != null) {
                                            navController.navigate(UserDetail(userId.toLong(), appDetail.store).createRoute())
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = raw?.user?.userAvatar,
                                    contentDescription = "上传者头像",
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(raw?.user?.displayName ?: "未知上传者", style = MaterialTheme.typography.titleMedium)
                                    Text("上传者", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            
                            // 审核员信息
                            if (raw?.audit_user != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            val userId = raw.audit_user?.id
                                            if (userId != null) {
                                                navController.navigate(UserDetail(userId.toLong(), appDetail.store).createRoute())
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = raw.audit_user?.userAvatar,
                                        contentDescription = "审核员头像",
                                        modifier = Modifier.size(40.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(raw.audit_user?.displayName ?: "未知审核员", style = MaterialTheme.typography.titleMedium)
                                        Text("审核员", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        else -> {
                            // 其他商店（如小趣空间）只显示上传者
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        val userId = appDetail.user.id.toLongOrNull()
                                        if (userId != null) {
                                            navController.navigate(UserDetail(userId, appDetail.store).createRoute())
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = appDetail.user.avatarUrl,
                                    contentDescription = "上传者头像",
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(appDetail.user.displayName, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }

        // --- 评论列表 ---
        item {
            Text("评论 (${appDetail.reviewCount})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (comments.isEmpty()) {
            item {
                Text("暂无评论", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
            }
        } else {
            items(comments) { comment ->
                UnifiedCommentItem(
                    comment = comment,
                    onReply = { onCommentReply(comment) },
                    onLongClick = { onCommentLongClick(comment.id) },
                    onUserClick = {
                        val userId = comment.sender.id.toLongOrNull()
                        if (userId != null) {
                            navController.navigate(UserDetail(userId, appDetail.store).createRoute())
                        }
                    }
                )
            }
        }
    }
}

// 新增：信息行组件
@Composable
fun InfoRow(label: String, value: String?) {
    if (!value.isNullOrEmpty() && value != "未知" && value != "") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Divider(modifier = Modifier.padding(vertical = 2.dp))
    }
}

// 新增：获取设备兼容性信息
@Composable
fun getDeviceInfo(minSdk: Int?): String {
    val context = LocalContext.current
    val deviceSdk = android.os.Build.VERSION.SDK_INT
    
    return buildString {
        append("设备: Android $deviceSdk")
        if (minSdk != null && deviceSdk >= minSdk) {
            append(" • 兼容")
        } else {
            append(" • 不兼容")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnifiedCommentItem(
    comment: UnifiedComment,
    onReply: () -> Unit,
    onLongClick: () -> Unit,
    onUserClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            // 添加长按支持
            .combinedClickable(
                onClick = {}, // 点击事件目前没有特殊操作，留空
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = comment.sender.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onUserClick),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Text(comment.sender.displayName, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "回复",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onReply)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)

            if (comment.fatherReply != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "回复 @${comment.fatherReply.sender.displayName}: ${comment.fatherReply.content}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}