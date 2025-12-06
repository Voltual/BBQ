// 文件路径: cc/bbq/xq/ui/plaza/VersionListScreen.kt
package cc.bbq.xq.ui.plaza

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.bbq.xq.data.unified.UnifiedAppItem
import cc.bbq.xq.ui.compose.BaseListScreen
import cc.bbq.xq.ui.theme.AppGridItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun VersionListScreen(
    appId: Int,
    onVersionSelected: (UnifiedAppItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VersionListViewModel = koinViewModel()
) {
    // 初始化ViewModel
    LaunchedEffect(appId) {
        viewModel.initialize(appId)
    }

    val versions by viewModel.versions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()

    BaseListScreen(
        items = versions,
        isLoading = isLoading,
        error = errorMessage,
        currentPage = currentPage,
        totalPages = totalPages,
        onRetry = { viewModel.refresh() },
        onLoadMore = { viewModel.loadMore() },
        emptyMessage = "暂无版本信息",
        itemContent = { version ->
            VersionItem(
                version = version,
                onClick = { onVersionSelected(version) }
            )
        },
        autoLoadMode = true,
        modifier = modifier
    )
}

@Composable
fun VersionItem(
    version: UnifiedAppItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 显示应用图标
            AppGridItem(
                app = version,
                onClick = onClick
            )
            
            // 显示版本信息
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "版本: ${version.versionName}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}