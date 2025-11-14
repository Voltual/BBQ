//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
// UpdateDialog.kt
package cc.bbq.xq.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.bbq.xq.data.UpdateInfo
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.style.TextAlign

// UpdateDialog.kt
@Composable
fun UpdateDialog(updateInfo: UpdateInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "发现新版本：${updateInfo.tag_name}",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = updateInfo.body,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请选择合适的APK版本下载",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                updateInfo.assets.filter { it.name.endsWith(".apk") }.forEach { asset ->
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(asset.browser_download_url)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            LocalContext.current.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "下载 ${asset.name}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后更新")
            }
        }
    )
}
