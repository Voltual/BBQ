//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
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
import cc.bbq.xq.ui.theme.BBQSnackbarHost // 导入 BBQSnackbarHost
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.load
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import cc.bbq.xq.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(
    imageUrl: String,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState, // 添加类型注解并标记为未使用
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val internalSnackbarHostState = remember { SnackbarHostState() } // fixed: rename to internalSnackbarHostState
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { BBQSnackbarHost(internalSnackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
                .clickable { onClose() }
        ) {
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        val photoView = PhotoView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            maximumScale = 8f
                            mediumScale = 3f
                            setScaleLevels(1f, 3f, 8f)

                            setOnLongClickListener {
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText("image_url", imageUrl)
                                )
                                scope.launch {
                                    internalSnackbarHostState.showSnackbar(
                                        message = context.getString(R.string.image_link_copied)
                                    )
                                }
                                true
                            }
                        }
                        addView(photoView)
                    }
                },
                update = { frameLayout ->
                    val photoView = frameLayout.getChildAt(0) as PhotoView
                    photoView.load(imageUrl) {
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