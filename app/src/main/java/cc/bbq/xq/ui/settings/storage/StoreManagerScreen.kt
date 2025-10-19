//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.settings.storage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.bbq.xq.ui.theme.BBQCard
import cc.bbq.xq.ui.theme.BBQOutlinedButton
import coil.Coil
import kotlinx.coroutines.launch
import coil.annotation.ExperimentalCoilApi // 导入 ExperimentalCoilApi
import coil.imageLoader // 导入 imageLoader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class) // 添加 ExperimentalCoilApi
@Composable
fun StoreManagerScreen(
    viewModel: StoreManagerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 注释掉超级缓存状态
    // val isSuperCacheEnabled by viewModel.isSuperCacheEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BBQCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "图片缓存",
                    style = MaterialTheme.typography.titleMedium
                )
                BBQOutlinedButton(
                    onClick = {
                        scope.launch {
                            Coil.imageLoader(context).diskCache?.clear()
                            Coil.imageLoader(context).memoryCache?.clear()
                        }
                    },
                    text = { Text("清除图片缓存") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 注释掉超级缓存开关部分
        /*
        BBQCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "超级缓存",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = isSuperCacheEnabled,
                        onCheckedChange = { viewModel.onSuperCacheEnabledChanged(it) }
                    )
                }
            }
        }
        */
    }
}