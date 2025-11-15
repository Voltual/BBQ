//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.deviceNameDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_name")

class DeviceNameDataStore(context: Context) {
    private val DEVICE_NAME_KEY = stringPreferencesKey("device_name")
    private val dataStore = context.deviceNameDataStore

    val deviceNameFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[DEVICE_NAME_KEY] ?: "https://gitee.com/Voltula/bbq/releases/"
        }

    suspend fun saveDeviceName(deviceName: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_NAME_KEY] = deviceName
        }
    }
}