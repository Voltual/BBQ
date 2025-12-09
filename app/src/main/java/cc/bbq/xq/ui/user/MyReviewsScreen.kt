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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
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
import cc.bbq.xq.ui.theme.AppShapes
import cc.bbq.xq.ui.theme.BBQButton
import cc.bbq.xq.ui.theme.BBQOutlinedButton
import cc.bbq.xq.util.formatTimestamp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun MyReviewsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MyReviewsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val reviews by viewModel.reviews.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedStore by viewModel.selectedStore.collectAsState()
    val showDeleteReviewDialog by viewModel.showDeleteReviewDialog.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Column(modifier = modifier.fillMaxSize()) {

        Spacer(Modifier.height(8.dp))

        // 评价列表
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && reviews.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (reviews.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "暂无评价",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (error != null) "加载失败" else "暂无评价",
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
                    items(reviews) { review ->
                        MyReviewItem(
                            review = review,
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
                            onDeleteReview = { reviewId ->
                                viewModel.showDeleteReviewDialog(reviewId)
                            }
                        )
                    }
                }
            }

            // 加载更多指示器
            if (isLoading && reviews.isNotEmpty()) {
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

    // 删除评价确认对话框
    if (showDeleteReviewDialog != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteReviewDialog() },
            title = { Text("确认删除评价") },
            text = { Text("确定要删除这条评价吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteReviewDialog?.let { reviewId ->
                            viewModel.deleteReview(reviewId)
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
                TextButton(onClick = { viewModel.hideDeleteReviewDialog() }) { 
                    Text("取消") 
                } 
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyReviewItem(
    review: UnifiedComment,
    onUserClick: (String) -> Unit,
    onOpenApp: (String, Long) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDeleteReview: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 评价头部
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(review.sender.avatarUrl ?: "https://static.sineshop.xin/images/user_avatar/default_avatar.png")
                        .diskCachePolicy(CachePolicy.DISABLED) // 禁用磁盘缓存
                        .build(),
                    contentDescription = "用户头像",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onUserClick(review.sender.id) },
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.sender.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatTimestamp(review.sendTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 评分
            review.rating?.let { rating ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "评分",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$rating.0",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // 评价内容
            Text(
                text = review.content,
                style = MaterialTheme.typography.bodyMedium
            )

            // 被删除的应用提示
            if (review.appId == null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "评价的应用已被删除",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(8.dp))

            // 操作按钮
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 如果评价关联了应用，显示查看应用按钮
                review.appId?.let { appId ->
                    val versionId = review.versionId ?: 0L
                    BBQOutlinedButton(
                        onClick = { onOpenApp(appId, versionId) },
                        text = { Text("查看应用") }
                    )
                }

                // 删除评价按钮
                BBQOutlinedButton(
                    onClick = { onDeleteReview(review.id) },
                    text = { Text("删除评价") },
                    shape = AppShapes.small,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}