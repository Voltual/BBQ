//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.util

import android.content.Context
import android.widget.Toast
import cc.bbq.xq.BuildConfig
import cc.bbq.xq.KtorClient
import cc.bbq.xq.data.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import io.ktor.client.call.body

object UpdateChecker {
    fun checkForUpdates(context: Context, onUpdate: (UpdateInfo?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = KtorClient.ApiServiceImpl.getLatestRelease()
                if (result.isSuccess) {
                    val update = result.getOrNull()
                    if (update != null) {
                        val currentVersion = BuildConfig.VERSION_NAME
                        val newVersion = update.tag_name.replace(Regex("[^\\d.]"), "")
                        if (newVersion > currentVersion) {
                            withContext(Dispatchers.Main) {
                                onUpdate(update)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "获取更新信息失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "检查更新失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "检查更新出错: ${e.message}", Toast.LENGTH_SHORT).show()
                    onUpdate(null)
                }
            }
        }
    }
}