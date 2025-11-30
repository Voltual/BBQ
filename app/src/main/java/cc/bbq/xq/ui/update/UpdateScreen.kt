// File: /app/src/main/java/cc/bbq/xq/ui/update/UpdateScreen.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。

package cc.bbq.xq.ui.update

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.* // 添加必要的导入
import androidx.compose.material3.* // 添加必要的导入
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.bbq.xq.AppStore // 导入更新后的 AppStore
import cc.bbq.xq.data.unified.UnifiedAppItem // 导入 UnifiedAppItem
import cc.bbq.xq.ui.compose.BaseListScreen
import cc.bbq.xq.ui.theme.AppGridItem // 确保导入 AppGridItem
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID // 用于生成 uniqueId

@Composable
fun UpdateScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<UnifiedAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // 页面加载时获取应用列表
    LaunchedEffect(Unit) {
        loadInstalledApps(context) { apps, error ->
            installedApps = apps ?: emptyList()
            errorMessage = error
            isLoading = false
        }
    }

    // 使用 BaseListScreen 包装内容，这里简化处理，只显示加载状态和列表
    // TODO: 后续需要处理分页、错误重试等
    BaseListScreen(
        items = installedApps,
        isLoading = isLoading,
        error = errorMessage,
        currentPage = 1,
        totalPages = 1, // 暂时不分页
        onRetry = {
            // 重试加载逻辑
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                loadInstalledApps(context) { apps, error ->
                    installedApps = apps ?: emptyList()
                    errorMessage = error
                    isLoading = false
                }
            }
        },
        onLoadMore = {}, // 暂时不需要加载更多
        emptyMessage = "未找到已安装的应用",
        itemContent = { appItem ->
            // 使用 AppGridItem 展示每个应用，直接使用 UnifiedAppItem
            AppGridItem(
                app = appItem,
                onClick = {
                    // TODO: 实现点击应用项的逻辑，例如查看详情或开始更新检查
                }
            )
        },
        modifier = modifier
    )
}

/**
 * 加载设备上已安装的应用列表并转换为 UnifiedAppItem
 * 注意：此函数应在后台线程执行
 */
private suspend fun loadInstalledApps(
    context: Context,
    callback: (List<UnifiedAppItem>?, String?) -> Unit
) {
    try {
        val pm = context.packageManager
        // 获取基本应用信息列表 (注意: 这里可能需要 QUERY_ALL_PACKAGES 权限)
        val packages = withContext(coroutineScope.coroutineContext) {
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        }

        val appList = packages.mapNotNull { packageInfo ->
            try {
                val appInfo = packageInfo.applicationInfo
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
                // 注意: 这里我们为本地应用构造了一个临时的 UnifiedAppItem
                // uniqueId 和 navigationId 对于本地应用可能需要特殊处理
                // 这里使用 packageName + UUID 作为 uniqueId 保证唯一性
                val uniqueId = "${packageName}_local_${UUID.randomUUID()}"
                val navigationId = packageName // navigationId 可以用包名
                val navigationVersionId = packageInfo.longVersionCode // 使用版本码

                UnifiedAppItem(
                    uniqueId = uniqueId,
                    navigationId = navigationId,
                    navigationVersionId = navigationVersionId,
                    store = AppStore.LOCAL, // 使用新添加的 LOCAL 类型
                    name = appName,
                    iconUrl = iconUri,
                    versionName = versionName
                )
            } catch (e: Exception) {
                // 忽略无法加载信息的应用
                null
            }
        }.sortedBy { it.name } // 按名称排序

        callback(appList, null)
    } catch (e: Exception) {
        callback(null, "加载应用列表失败: ${e.message}")
    }
}