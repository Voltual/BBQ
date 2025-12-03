// 文件路径: cc/bbq/xq/ui/download/DownloadScreen.kt
package cc.bbq.xq.ui.download

import android.content.Context
import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.bbq.xq.service.download.DownloadStatus
import cc.bbq.xq.service.download.DownloadTask
import cc.bbq.xq.ui.theme.AppShapes
import cc.bbq.xq.ui.theme.BBQButton
import cc.bbq.xq.ui.theme.BBQCard
import cc.bbq.xq.ui.theme.BBQIconButton
// 关键：导入 FileActionUtil
import cc.bbq.xq.util.FileActionUtil

@Composable
fun DownloadScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = viewModel()
) {
    val status by viewModel.downloadStatus.collectAsState()
    // 从 ViewModel 获取所有下载任务
    val downloadTasks by viewModel.downloadTasks.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 如果没有下载任务，则显示当前下载状态
            if (downloadTasks.isEmpty()) {
                AnimatedContent(targetState = status, label = "DownloadStatus") { currentStatus ->
                    when (currentStatus) {
                        is DownloadStatus.Idle -> EmptyDownloadState()
                        is DownloadStatus.Pending -> PendingDownloadState()
                        is DownloadStatus.Downloading -> DownloadingState(
                            status = currentStatus,
                            onCancel = { viewModel.cancelDownload() }
                        )
                        is DownloadStatus.Paused -> PausedState(currentStatus)
                        is DownloadStatus.Success -> SuccessState(currentStatus)
                        is DownloadStatus.Error -> ErrorState(currentStatus)
                    }
                }
            } else {
                // 如果有下载任务，则显示下载任务列表
                LazyColumn {
                    items(downloadTasks) { task ->
                        DownloadTaskItem(task = task, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadTaskItem(
    task: DownloadTask,
    viewModel: DownloadViewModel
) {
    val context = LocalContext.current
    val status = when (task.status) {
        DownloadStatus.Idle::class.java.simpleName -> DownloadStatus.Idle
        DownloadStatus.Pending::class.java.simpleName -> DownloadStatus.Pending
        DownloadStatus.Downloading::class.java.simpleName -> DownloadStatus.Downloading(
            progress = task.progress,
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes,
            speed = "" // 速度信息在数据库中没有存储，需要从服务中获取
        )
        DownloadStatus.Paused::class.java.simpleName -> DownloadStatus.Paused(
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes
        )
        DownloadStatus.Success::class.java.simpleName -> {
            val file = java.io.File(task.savePath, task.fileName)
            DownloadStatus.Success(file = file)
        }
        DownloadStatus.Error::class.java.simpleName -> DownloadStatus.Error(message = "未知错误")
        else -> DownloadStatus.Idle
    }

    when (status) {
        is DownloadStatus.Idle -> EmptyDownloadState()
        is DownloadStatus.Pending -> PendingDownloadState()
        is DownloadStatus.Downloading -> DownloadingState(
            status = status,
            onCancel = { viewModel.cancelDownload() }
        )
        is DownloadStatus.Paused -> PausedState(status)
        is DownloadStatus.Success -> SuccessState(status)
        is DownloadStatus.Error -> ErrorState(status)
    }
}

@Composable
fun EmptyDownloadState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无下载任务",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PendingDownloadState() {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("准备中...")
        }
    }
}

@Composable
fun DownloadingState(
    status: DownloadStatus.Downloading,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "正在下载文件...",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatFileSize(context, status.downloadedBytes)} / ${formatFileSize(context, status.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BBQIconButton(
                    onClick = onCancel,
                    icon = Icons.Default.Close,
                    contentDescription = "取消下载",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(AppShapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(status.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status.speed,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PausedState(status: DownloadStatus.Paused) {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("下载已暂停", style = MaterialTheme.typography.titleMedium)
                Text(
                    "已下载: ${formatFileSize(LocalContext.current, status.downloadedBytes)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SuccessState(status: DownloadStatus.Success) {
    val context = LocalContext.current
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "下载完成",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = status.file.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BBQButton(
                    onClick = {
                        // 使用 FileActionUtil 打开文件
                        FileActionUtil.openFile(context, status.file)
                    },
                    text = { Text("查看文件") }
                )
            }
        }
    }
}

@Composable
fun ErrorState(status: DownloadStatus.Error) {
    BBQCard(
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "下载失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

// 辅助函数：格式化文件大小
fun formatFileSize(context: Context, size: Long): String {
    return Formatter.formatFileSize(context, size)
}