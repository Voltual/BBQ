//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.util

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import cc.bbq.xq.BuildConfig
import cc.bbq.xq.KtorClient
import cc.bbq.xq.data.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import io.ktor.client.call.body
import android.app.Activity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import cc.bbq.xq.ui.getActivity
import androidx.compose.ui.res.stringResource
import cc.bbq.xq.R
import android.content.ContextWrapper

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
                                //Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                                showSnackbar(context, context.getString(R.string.already_latest_version))
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            //Toast.makeText(context, "获取更新信息失败", Toast.LENGTH_SHORT).show()
                            showSnackbar(context, context.getString(R.string.failed_to_get_update_info))
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        //Toast.makeText(context, "检查更新失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        showSnackbar(context, context.getString(R.string.check_update_failed) + ": ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    //Toast.makeText(context, "检查更新出错: ${e.message}", Toast.LENGTH_SHORT).show()
                    showSnackbar(context, context.getString(R.string.check_update_error) + ": ${e.message}")
                    onUpdate(null)
                }
            }
        }
    }

    private fun showSnackbar(context: Context, message: String) {
        val activity = context.getActivity() ?: return
        val snackbarHostState = activity.snackbarHostState ?: return
        CoroutineScope(Dispatchers.Main).launch {
            snackbarHostState.showSnackbar(message)
        }
    }
}

// 扩展函数，用于在 Context 中查找 SnackbarHostState
private val Activity.snackbarHostState: SnackbarHostState?
    get() = (this as? MainActivity)?.let {
        val composeView = window.decorView.findViewById<androidx.compose.ui.platform.ComposeView>(android.R.id.content)
        composeView?.let {
            var hostState: SnackbarHostState? = null
            it.setContent {
                hostState = remember { SnackbarHostState() }
            }
            hostState
        }
    }

fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}