// File: /app/src/main/java/cc/bbq/xq/ui/update/UpdateViewModel.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.update

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.R
import cc.bbq.xq.data.unified.UnifiedAppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * UpdateScreen 的 ViewModel，用于管理应用列表的状态和加载逻辑。
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * 定义 UI 状态
     */
    data class UiState(
        val isLoading: Boolean = false,
        val apps: List<UnifiedAppItem> = emptyList(),
        val error: String? = null
    )

    // 私有可变状态流
    private val _uiState = MutableStateFlow(UiState())
    // 对外暴露的只读状态流
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // ViewModel 创建后立即加载数据
        loadInstalledApps()
    }

    /**
     * 公开的刷新方法，供 UI 调用
     */
    fun refresh() {
        loadInstalledApps()
    }

    /**
     * 加载设备上已安装的用户应用列表并转换为 UnifiedAppItem
     */
    private fun loadInstalledApps() {
        // 更新状态为加载中
        _uiState.value = UiState(isLoading = true, apps = emptyList(), error = null)

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val pm = context.packageManager

                // 在 IO 线程执行耗时操作
                val appList = withContext(Dispatchers.IO) {
                    // 获取所有已安装的应用包信息
                    val allPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

                    // 过滤出用户安装的应用（非系统应用）
                    // FLAG_SYSTEM 表示系统应用
                    // FLAG_UPDATED_SYSTEM_APP 表示被更新过的系统应用，通常也视为用户可管理的
                    val userPackages = allPackages.filter { packageInfo ->
                        val flags = packageInfo.applicationInfo?.flags ?: 0
                        (flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                                (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    }

                    // 将用户应用信息转换为 UnifiedAppItem
                    userPackages.mapNotNull { packageInfo ->
                        try {
                            val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                            val appName = appInfo.loadLabel(pm).toString()
                            val packageName = packageInfo.packageName
                            val versionName = packageInfo.versionName ?: "N/A"
                            // 使用应用图标资源 ID 构造 URI
                            val iconResId = appInfo.icon
                            val iconUri = if (iconResId != 0) {
                                "android.resource://${context.packageName}/$iconResId"
                            } else {
                                "" // 或者使用默认图标
                            }

                            // 创建 UnifiedAppItem
                            val uniqueId = "${packageName}_local_${UUID.randomUUID()}"
                            val navigationId = packageName
                            val navigationVersionId = packageInfo.longVersionCode

                            UnifiedAppItem(
                                uniqueId = uniqueId,
                                navigationId = navigationId,
                                navigationVersionId = navigationVersionId,
                                store = AppStore.LOCAL,
                                name = appName,
                                iconUrl = iconUri,
                                versionName = versionName
                            )
                        } catch (e: Exception) {
                            // 忽略无法加载信息的应用
                            null
                        }
                    }.sortedBy { it.name } // 按名称排序
                }

                // 加载成功，更新状态
                _uiState.value = UiState(isLoading = false, apps = appList, error = null)

            } catch (e: Exception) {
                // 加载失败，更新状态
                val errorMessage = getApplication<Application>().getString(R.string.update_load_apps_failed, e.message)
                _uiState.value = UiState(isLoading = false, apps = emptyList(), error = errorMessage)
            }
        }
    }
}