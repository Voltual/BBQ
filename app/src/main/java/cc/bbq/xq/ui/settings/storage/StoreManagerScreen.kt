//ui/settings/storage/StoreManagerScreen.kt
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
    val isSuperCacheEnabled by viewModel.isSuperCacheEnabled.collectAsState()

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
    }
}
*/