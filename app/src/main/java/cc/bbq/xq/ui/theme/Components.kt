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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext // 导入 LocalContext
import androidx.compose.runtime.collectAsState // 导入 collectAsState
import androidx.compose.foundation.layout.Box // 导入 Box
import androidx.compose.foundation.layout.fillMaxSize // 导入 fillMaxSize
import androidx.compose.foundation.Image // 导入 Image
import androidx.compose.ui.layout.ContentScale // 导入 ContentScale
import coil.compose.rememberAsyncImagePainter // 导入 rememberAsyncImagePainter

// 基础按钮组件
@Composable
fun BBQButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    enabled: Boolean = true, // 添加 enabled 参数
    shape: Shape = AppShapes.medium,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled, // 传递 enabled 状态
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
    enabled: Boolean = true, // 添加 enabled 参数
    shape: Shape = AppShapes.small,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled, // 传递 enabled 状态
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