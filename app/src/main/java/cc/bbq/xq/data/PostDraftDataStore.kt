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
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.draftDataStore: DataStore<Preferences> by preferencesDataStore(name = "post_draft")

class PostDraftDataStore(private val context: Context) {
    companion object {
        private val DRAFT_TITLE = stringPreferencesKey("draft_title")
        private val DRAFT_CONTENT = stringPreferencesKey("draft_content")
        private val DRAFT_SUBSECTION_ID = intPreferencesKey("draft_subsection_id")
        private val HAS_DRAFT = booleanPreferencesKey("has_draft")
        private val DRAFT_IMAGE_URIS = stringPreferencesKey("draft_image_uris")
        // 新增图片URL存储键
        private val DRAFT_IMAGE_URLS = stringPreferencesKey("draft_image_urls")

        // 新增：自动恢复草稿选项
        private val AUTO_RESTORE_DRAFT = booleanPreferencesKey("auto_restore_draft")
        // 新增：不存储草稿选项
        private val DO_NOT_SAVE_DRAFT = booleanPreferencesKey("do_not_save_draft")
    }

    suspend fun saveDraft(
        title: String,
        content: String,
        imageUris: List<Uri>,
        imageUrls: String, // 新增图片URL参数
        subsectionId: Int
    ) {
        val uriStrings = imageUris.joinToString(",") { it.toString() }
        context.draftDataStore.edit { preferences ->
            preferences[DRAFT_TITLE] = title
            preferences[DRAFT_CONTENT] = content
            preferences[DRAFT_IMAGE_URIS] = uriStrings
            preferences[DRAFT_IMAGE_URLS] = imageUrls // 保存图片URL
            preferences[DRAFT_SUBSECTION_ID] = subsectionId
            preferences[HAS_DRAFT] = true
        }
    }

    suspend fun clearDraft() {
        context.draftDataStore.edit { preferences ->
            preferences.remove(DRAFT_TITLE)
            preferences.remove(DRAFT_CONTENT)
            preferences.remove(DRAFT_IMAGE_URIS)
            preferences.remove(DRAFT_IMAGE_URLS) // 清除图片URL
            preferences.remove(DRAFT_SUBSECTION_ID)
            preferences[HAS_DRAFT] = false
        }
    }

    // 新增：保存自动恢复草稿选项
    suspend fun setAutoRestoreDraft(autoRestore: Boolean) {
        context.draftDataStore.edit { preferences ->
            preferences[AUTO_RESTORE_DRAFT] = autoRestore
        }
    }

    // 新增：保存不存储草稿选项
    suspend fun setDoNotSaveDraft(doNotSave: Boolean) {
        context.draftDataStore.edit { preferences ->
            preferences[DO_NOT_SAVE_DRAFT] = doNotSave
        }
    }

    val draftFlow: Flow<Draft> = context.draftDataStore.data
        .map { preferences ->
            val uriStrings = preferences[DRAFT_IMAGE_URIS] ?: ""
            val imageUris = uriStrings.split(",")
                .filter { it.isNotEmpty() }
                .map { Uri.parse(it) }
            
            Draft(
                title = preferences[DRAFT_TITLE] ?: "",
                content = preferences[DRAFT_CONTENT] ?: "",
                imageUris = imageUris,
                imageUrls = preferences[DRAFT_IMAGE_URLS] ?: "", // 恢复图片URL
                subsectionId = preferences[DRAFT_SUBSECTION_ID] ?: 11,
                hasDraft = preferences[HAS_DRAFT] ?: false,
                autoRestoreDraft = preferences[AUTO_RESTORE_DRAFT] ?: false, // 恢复自动恢复草稿选项
                doNotSaveDraft = preferences[DO_NOT_SAVE_DRAFT] ?: false // 恢复不存储草稿选项
            )
        }
    
    data class Draft(
        val title: String,
        val content: String,
        val imageUris: List<Uri>,
        val imageUrls: String, // 新增图片URL字段
        val subsectionId: Int,
        val hasDraft: Boolean,
        val autoRestoreDraft: Boolean, // 新增自动恢复草稿选项
        val doNotSaveDraft: Boolean // 新增不存储草稿选项
    )
}