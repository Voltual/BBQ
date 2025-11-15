//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.settings.update

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import cc.bbq.xq.data.UpdateSettingsDataStore
import cc.bbq.xq.KtorClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import cc.bbq.xq.BuildConfig
import cc.bbq.xq.data.UpdateInfo
import kotlinx.serialization.json.Json
import io.ktor.client.call.body
import cc.bbq.xq.ui.compose.UpdateDialog
import kotlinx.serialization.decodeFromString

class UpdateSettingsViewModel : ViewModel() {

    val autoCheckUpdates: Flow<Boolean> = UpdateSettingsDataStore.autoCheckUpdates

    suspend fun setAutoCheckUpdates(context: Context, value: Boolean) {
        UpdateSettingsDataStore.setAutoCheckUpdates(context, value)
    }
        fun checkForUpdates(context: Context) {
        viewModelScope.launch {
            try {
                val result = KtorClient.ApiServiceImpl.getLatestRelease()
                if (result.isSuccess) {
                    val update = result.getOrNull()
                    if (update != null) {
                        // 版本号比较
                        val currentVersion = BuildConfig.VERSION_NAME
                        val newVersion = update.tag_name.replace(Regex("[^\\d.]"), "") // 提取数字部分
                        if (newVersion > currentVersion) {
                            // 在主线程更新UI
                            withContext(Dispatchers.Main) {
                                showUpdateDialog(context, update)
                            }
                        } else {
                            withContext(Dispatchers.Main){
                                Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main){
                            Toast.makeText(context, "获取更新信息失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main){
                        Toast.makeText(context, "检查更新失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main){
                    Toast.makeText(context, "检查更新出错: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
        @Composable
    private fun showUpdateDialog(context: Context, updateInfo: UpdateInfo) {
        UpdateDialog(updateInfo = updateInfo) {
            //showDialog = false
        }
    }
}