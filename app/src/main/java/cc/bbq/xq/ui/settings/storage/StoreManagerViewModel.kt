//ui/settings/storage/StoreManagerViewModel.kt
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