// Components.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose. runtime.collectAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.layout.width // 添加正确的导入路径
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import androidx.navigation.NavController
import androidx.compose.foundation.clickable // 导入 clickable
import androidx.compose.material3.CircularProgressIndicator // 导入 CircularProgressIndicator
import androidx.compose.material.icons.Icons // 导入 Icons
import androidx.compose.foundation.layout.padding // 导入 padding
import androidx.compose.foundation.shape.CircleShape // 导入 CircleShape
import androidx.compose.material.icons.filled.BrokenImage // 确保导入了 BrokenImage

// 基础按钮组件
@Composable
fun BBQButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    enabled: Boolean = true,
    shape: Shape = AppShapes.medium,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = contentPadding
    ) {
        text()
    }
}

// 轮廓按钮组件
@Composable
fun BBQOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    enabled: Boolean = true,
    shape: Shape = AppShapes.small,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = contentPadding
    ) {
        text()
    }
}

// 卡片组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BBQCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    shape: Shape = AppShapes.medium,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick ?: {},
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = border
    ) {
        content()
    }
}

// 新增：支持全局背景的卡片
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BBQBackgroundCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    shape: Shape = AppShapes.medium,
//    backgroundAlpha: Float = 0.1f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val globalBackgroundUriState = ThemeColorStore.getGlobalBackgroundUriFlow(context).collectAsState(initial = null)
    val globalBackgroundUri by globalBackgroundUriState

    Card(
        modifier = modifier,
        onClick = onClick ?: {},
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = border
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 全局背景图片
            if (globalBackgroundUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = globalBackgroundUri),
                    contentDescription = "Global Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .matchParentSize()
                )
            }

            // 内容区域（不透明，确保文字可读）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                content()
            }
        }
    }
}

// 图标按钮组件
@Composable
fun BBQIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

//移动帖子详情页“带文本的开关”到theme下的公共位置以便复用
@Composable
fun SwitchWithText(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ImagePreviewItem(
    imageModel: Any,
    onRemoveClick: (() -> Unit)? = null,
    onImageClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentDescription: String = "图片预览"
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .size(100.dp)
            .clip(MaterialTheme.shapes.medium)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable { onImageClick?.invoke() }
        ) {
            val state = painter.state
            when (state) {
                is AsyncImagePainter.State.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(), 
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }
                is AsyncImagePainter.State.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(), 
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BrokenImage, 
                            contentDescription = "加载失败",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    SubcomposeAsyncImageContent()
                }
            }
        }

        // 只有在提供了移除回调时才显示移除按钮
        onRemoveClick?.let { removeCallback ->
            IconButton(
                onClick = removeCallback,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f), 
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "移除图片",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// 用于显示图片上传区域的通用组件
@Composable
fun ImageUploadSection(
    images: List<Any>,
    onAddClick: () -> Unit,
    onRemoveClick: (Any) -> Unit,
    onImageClick: (Any) -> Unit,
    maxImages: Int,
    modifier: Modifier = Modifier,
    title: String = "图片上传"
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "$title (${images.size}/$maxImages)", 
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(images) { image ->
                ImagePreviewItem(
                    imageModel = image,
                    onRemoveClick = { onRemoveClick(image) },
                    onImageClick = { onImageClick(image) },
                    contentDescription = "已上传图片"
                )
            }
            
            // 添加图片按钮（如果未达到最大数量）
            if (images.size < maxImages) {
                item {
                    OutlinedButton(
                        onClick = onAddClick,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Icon(Icons.Default.Add, "添加图片")
                    }
                }
            }
        }
    }
}