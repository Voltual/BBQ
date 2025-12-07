package cc.bbq.xq.ui.user

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
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
import cc.bbq.xq.AppStore
import cc.bbq.xq.data.unified.UnifiedComment
import cc.bbq.xq.ui.AppDetail
import cc.bbq.xq.ui.UserDetail
import cc.bbq.xq.ui.theme.*
import cc.bbq.xq.util.formatTimestamp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

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
    val showDeleteCommentDialog by viewModel.showDeleteCommentDialog.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 标题栏 - 移除商店选择器，只显示标题
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "我的评论",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "查看您在${selectedStore.displayName}发布的评论",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

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
                                val userDetailRoute = UserDetail(
                                    userId = userId.toLong(),
                                    store = selectedStore
                                ).createRoute()
                                navController.navigate(userDetailRoute)
                            },
                            onOpenApp = { appId, versionId ->
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
                                    // 错误处理
                                }
                            },
                            onDeleteComment = { commentId ->
                                viewModel.showDeleteCommentDialog(commentId)
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

    // 删除评论确认对话框
    if (showDeleteCommentDialog != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteCommentDialog() },
            title = { Text("确认删除评论") },
            text = { Text("确定要删除这条评论吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteCommentDialog?.let { commentId ->
                            viewModel.deleteComment(commentId)
                        }
                    }
                ) { 
                    Text(
                        "删除",
                        color = MaterialTheme.colorScheme.error
                    ) 
                }
            },
            dismissButton = { 
                TextButton(onClick = { viewModel.hideDeleteCommentDialog() }) { 
                    Text("取消") 
                } 
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyCommentItem(
    comment: UnifiedComment,
    onUserClick: (String) -> Unit,
    onOpenApp: (String, Long) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDeleteComment: (String) -> Unit // 新增：删除评论回调
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
                        text = formatTimestamp(comment.sendTime),
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

                // 删除评论按钮
                Button(
                    onClick = { onDeleteComment(comment.id) },
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("删除评论")
                }
            }
        }
    }
}