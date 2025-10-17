//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.processedPostsDataStore: DataStore<Preferences> by preferencesDataStore(name = "qubot_processed_posts")

class ProcessedPostsDataStore(private val context: Context) {

    private val dataStore: DataStore<Preferences>
        get() = context.processedPostsDataStore

    private companion object {
        val PROCESSED_POST_IDS = stringSetPreferencesKey("processed_post_ids")
    }

    val processedPostIdsFlow: Flow<Set<String>> = dataStore.data
        .map { preferences ->
            preferences[PROCESSED_POST_IDS] ?: emptySet()
        }

    suspend fun addPostId(postId: Long) {
        dataStore.edit { preferences ->
            val currentIds = preferences[PROCESSED_POST_IDS] ?: emptySet()
            preferences[PROCESSED_POST_IDS] = currentIds + postId.toString()
        }
    }

    suspend fun containsPostId(postId: Long): Boolean {
        return processedPostIdsFlow.first().contains(postId.toString())
    }

    suspend fun clearHistory() {
        dataStore.edit { preferences ->
            preferences.remove(PROCESSED_POST_IDS)
        }
    }
}