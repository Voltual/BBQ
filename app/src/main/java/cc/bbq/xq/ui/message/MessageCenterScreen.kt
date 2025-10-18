//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.message

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cc.bbq.xq.KtorClient
import cc.bbq.xq.ui.compose.MessageItem
import cc.bbq.xq.ui.compose.PageJumpDialog
import cc.bbq.xq.ui.compose.PaginationControls

// 在 MessageCenterScreen.kt 中修复 UI 显示问题

@Composable
fun MessageCenterScreen(
    viewModel: MessageViewModel,
    onMessageClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var showPageDialog by remember { mutableStateOf(false) }
    val dialogShape = remember { RoundedCornerShape(4.dp) }

    // 修复：只在真正需要时初始化，不强制重置
    LaunchedEffect(Unit) {
        viewModel.initializeIfNeeded()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 消息列表
        Box(modifier = Modifier.weight(1f)) {
            when {
                // 修复：显示加载指示条
                state.isLoading && state.messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("加载中...")
                    }
                }
                state.messages.isEmpty() && state.isInitialized -> {
                    Text(
                        text = "暂无消息",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.messages.isEmpty() -> {
                    Text(
                        text = "加载中...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.messages) { message ->
                            MessageItem(
                                message = message,
                                onClick = {
                                    if (message.postid != null) {
                                        onMessageClick(message.postid)
                                    }
                                }
                            )
                        }
                        
                        // 修复：分页加载时显示底部加载指示
                        if (state.isLoading && state.messages.isNotEmpty()) {
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
            }

            // 显示错误信息
            state.error?.let { error ->
                if (state.messages.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.reset() }) {
                            Text("重试")
                        }
                    }
                }
            }
        }

        // 分页控制栏 - 只在有数据且不是加载中时显示
        if (state.messages.isNotEmpty() && !state.isLoading) {
            PaginationControls(
                currentPage = state.currentPage,
                totalPages = state.totalPages,
                onPrevClick = { viewModel.prevPage() },
                onNextClick = { viewModel.nextPage() },
                onPageClick = { showPageDialog = true },
                isPrevEnabled = state.currentPage > 1,
                isNextEnabled = state.currentPage < state.totalPages
            )
        }
    }

    // 分页跳转对话框
    if (showPageDialog) {
        PageJumpDialog(
            currentPage = state.currentPage,
            totalPages = state.totalPages,
            onDismiss = { showPageDialog = false },
            onConfirm = { page ->
                viewModel.goToPage(page)
                showPageDialog = false
            },
            shape = dialogShape
        )
    }
}