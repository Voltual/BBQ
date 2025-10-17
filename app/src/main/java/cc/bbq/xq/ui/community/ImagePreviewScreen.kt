//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.community

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.load
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(
    imageUrl: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var showCopyToast by remember { mutableStateOf(false) }
    
    // 显示复制成功提示
    LaunchedEffect(showCopyToast) {
        if (showCopyToast) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "已复制图片链接", android.widget.Toast.LENGTH_SHORT).show()
            }
            showCopyToast = false
        }
    }

    Scaffold(
        containerColor = Color.Black,
        // 移除 topBar，因为 MainActivity 已经提供了返回按钮
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
                .clickable { onClose() } // 使用内置的 clickable
        ) {
            // 使用 AndroidView 嵌入原生 PhotoView
            AndroidView(
                factory = { context ->
                    // 创建一个 FrameLayout 作为容器
                    FrameLayout(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        // 添加 PhotoView
                        val photoView = PhotoView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            
                            // 设置缩放级别
                            maximumScale = 8f
                            mediumScale = 3f
                            setScaleLevels(1f, 3f, 8f)
                            
                            // 长按事件：复制图片链接
                            setOnLongClickListener {
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText("image_url", imageUrl)
                                )
                                showCopyToast = true
                                true
                            }
                        }
                        
                        addView(photoView)
                    }
                },
                update = { frameLayout ->
                    // 更新视图：加载图片
                    val photoView = frameLayout.getChildAt(0) as PhotoView
                    photoView.load(imageUrl) {
                        // 原始尺寸加载
                        size(coil.size.Size.ORIGINAL)
                        diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}