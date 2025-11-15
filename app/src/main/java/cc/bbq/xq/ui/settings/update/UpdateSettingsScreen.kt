//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.settings.update

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.bbq.xq.ui.components.SwitchWithText //修复 import
import kotlinx.coroutines.launch
import cc.bbq.xq.ui.components.SwitchWithText

@Composable
fun UpdateSettingsScreen(
    viewModel: UpdateSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsState(initial = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SwitchWithText(
            text = "自动检查更新",
            checked = autoCheckUpdates,
            onCheckedChange = {
                scope.launch {
                    viewModel.setAutoCheckUpdates(context, it)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // 手动检查更新的逻辑
                viewModel.checkForUpdates(context)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("手动检查更新")
        }
    }
}