//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package cc.bbq.xq.ui.community

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.navigation.NavController
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun BrowseHistoryScreen(
    viewModel: BrowseHistoryViewModel = viewModel(),
    onPostClick: (Long) -> Unit,
//    navController: NavController,
    modifier: Modifier = Modifier
) {
    val history by viewModel.historyList.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedCount = viewModel.selectedItems.collectAsState().value.size
    val context = LocalContext.current

    // 监听复制事件
    LaunchedEffect(Unit) {
        // 核心修正 #3: 解构 Pair，获取正确的 count
        viewModel.copyEvent.collect { (textToCopy, count) ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("BBQ History Links", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "已复制 $count 条链接", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("还没有任何浏览记录", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(history, key = { it.postId }) { item ->
                    val isSelected = viewModel.selectedItems.collectAsState().value.contains(item.postId)
                    HistoryListItem(
                        history = item,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onToggleSelection = { viewModel.toggleSelection(item.postId) },
                        onStartSelection = { viewModel.startSelectionMode(item.postId) },
                        onPostClick = onPostClick
                    )
                }
            }
        }

        // 选择模式下的浮动操作按钮
        if (isSelectionMode) {
            SelectionActionFABs(
                onDelete = { viewModel.deleteSelected() },
                onCopy = { viewModel.copyShareLinks() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }

    // 选择模式下的顶部应用栏状态管理
    // 注意：实际的 TopAppBar 现在由 MainActivity 统一管理
    // 这里我们通过状态来影响 MainActivity 的标题显示
    LaunchedEffect(isSelectionMode, selectedCount) {
        // 这里可以添加逻辑来更新 MainActivity 的标题状态
        // 例如通过共享的 ViewModel 或回调
    }
}

@Composable
private fun SelectionActionFABs(
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onCopy,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(Icons.Default.ContentCopy, "复制")
        }
        FloatingActionButton(
            onClick = onDelete,
            containerColor = MaterialTheme.colorScheme.errorContainer
        ) {
            Icon(Icons.Default.Delete, "删除")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryListItem(
    history: BrowseHistory,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onStartSelection: () -> Unit,
    onPostClick: (Long) -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    // 使用 PostItem 几乎可以完美复用，但为了点击逻辑分离，我们自定义一个
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    } else {
                        onPostClick(history.postId)
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        onStartSelection()
                    }
                }
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(history.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(history.previewContent, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Text(
                history.formattedTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 选择模式状态管理
// 由于我们移除了内部的 TopAppBar，选择模式的状态需要由外部管理
// 这里我们提供一个状态类来帮助管理选择模式
class BrowseHistorySelectionState {
    var isSelectionMode by mutableStateOf(false)
    var selectedCount by mutableStateOf(0)
    var selectionTitle by mutableStateOf("浏览历史")
}

// 选择模式下的操作函数
@Composable
fun rememberBrowseHistorySelectionState(): BrowseHistorySelectionState {
    return remember { BrowseHistorySelectionState() }
}