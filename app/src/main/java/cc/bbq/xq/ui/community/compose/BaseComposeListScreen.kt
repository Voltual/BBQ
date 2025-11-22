//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.community.compose

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cc.bbq.xq.R
import cc.bbq.xq.KtorClient
import cc.bbq.xq.ui.community.ListScreen
import cc.bbq.xq.AuthManager // 添加 AuthManager 导入

// 导入下拉刷新相关组件
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.first // 添加 first 导入

// 在 BaseComposeListScreen.kt 中修复导航逻辑
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BaseComposeListScreen(
    title: String = "",
    posts: List<KtorClient.Post>, // 修改为 KtorClient.Post
    isLoading: Boolean,
    errorMessage: String,
    onItemClick: (KtorClient.Post) -> Unit, // 修改为 KtorClient.Post
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onSearchClick: () -> Unit = {},
    onCreateClick: () -> Unit = {},
    historyClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    totalPages: Int = 1,
    onJumpToPage: (Int) -> Unit = {},
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState, // 添加 SnackbarHostState 参数
    // 新增下拉刷新相关参数
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showJumpDialog by remember { mutableStateOf(false) }
    var inputPage by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // 创建 CoroutineScope
    
    // 添加下拉刷新状态
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("从哪页开始呢~_~") },
            text = {
                Column {
                    Text("总页数: $totalPages", color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = inputPage,
                        onValueChange = { inputPage = it },
                        label = { Text("输入页码 (1-$totalPages)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val page = inputPage.toIntOrNull() ?: 1
                        if (page in 1..totalPages) {
                            onJumpToPage(page)
                        }
                        showJumpDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant, // 设置对话框背景色
            titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant, // 设置标题文字颜色
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant // 设置文本内容颜色
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text(text = title)
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("社区") },
                                onClick = {
                                    expanded = false
                                    onNavigate("community")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("热点") },
                                onClick = {
                                    expanded = false
                                    onNavigate("hot_posts")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("关注的人") },
                                onClick = {
                                    expanded = false
                                    onNavigate("following_posts")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("我喜欢的") },
                                onClick = {
                                    expanded = false
                                    onNavigate("my_likes")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("我的帖子") },
                                onClick = {
                                    expanded = false
                                    // 在协程中获取 currentUserId
                                    scope.launch {
                                        // 使用 first() 获取 Flow 的当前值
                                        val currentUserId = AuthManager.getUserId(context).first()
                                        //  fixed: remove unnecessary non-null check. The type of `currentUserId` is Long, so it can't be null.
                                        //  Also, it's better to check if the user id is valid (e.g., greater than 0)
                                        if (currentUserId > 0) {
                                            onNavigate("my_posts/$currentUserId")
                                        } else {
                                             scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = context.getString(R.string.login_first),
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                            //android.widget.Toast.makeText(context, "请先登录", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // 确保使用正确的引用
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onMessageClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_message),
                            contentDescription = "消息中心",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showJumpDialog = true; inputPage = "" }) {
                        Icon(
                            painter = painterResource(id = R.drawable.kakao_page),
                            contentDescription = "跳页",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onCreateClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "发帖",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = historyClick) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "浏览历史",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pullRefresh(pullRefreshState) // 添加下拉刷新修饰符
        ) {
            if (errorMessage.isNotEmpty()) {
                ErrorView(
                    message = errorMessage,
                    onRetry = onRefresh,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (posts.isEmpty() && isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (posts.isEmpty()) {
                EmptyView(
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                PostListComposable(
                    posts = posts,
                    isLoading = isLoading,
                    onItemClick = onItemClick,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isLoading && posts.isNotEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
            
            // 添加下拉刷新指示器
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colorScheme.primary, // 使用主题主色
                backgroundColor = MaterialTheme.colorScheme.surface // 使用主题背景色
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = TextStyle(color = MaterialTheme.colorScheme.error),
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(text = "重试")
        }
    }
}

@Composable
private fun EmptyView(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "暂无内容",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRefresh,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(text = "刷新")
        }
    }
}