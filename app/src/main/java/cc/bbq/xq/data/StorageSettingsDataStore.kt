//data/StorageSettingsDataStore.kt
package cc.bbq.xq.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.storageSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "storage_settings")

class StorageSettingsDataStore(context: Context) {

    private val dataStore: DataStore<Preferences> = context.storageSettingsDataStore

    companion object {
        private val IS_SUPER_CACHE_ENABLED = booleanPreferencesKey("is_super_cache_enabled")
    }

    val isSuperCacheEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_SUPER_CACHE_ENABLED] ?: false
        }

    suspend fun updateSuperCacheEnabled(isEnabled: Boolean) {
        dataStore.edit { it[IS_SUPER_CACHE_ENABLED] = isEnabled }
    }
}