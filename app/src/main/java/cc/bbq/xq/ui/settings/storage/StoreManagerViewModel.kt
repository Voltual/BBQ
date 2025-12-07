//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.settings.storage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.BBQApplication
import cc.bbq.xq.data.StorageSettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class StoreManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val storageSettingsDataStore = StorageSettingsDataStore(application)

    private val _isSuperCacheEnabled = MutableStateFlow(false)
    val isSuperCacheEnabled: StateFlow<Boolean> = _isSuperCacheEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            storageSettingsDataStore.isSuperCacheEnabledFlow.collect { isEnabled ->
                _isSuperCacheEnabled.value = isEnabled
            }
        }
    }

    fun onSuperCacheEnabledChanged(isEnabled: Boolean) {
        viewModelScope.launch {
            storageSettingsDataStore.updateSuperCacheEnabled(isEnabled)
        }
    }
}