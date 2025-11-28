// /app/src/main/java/cc/bbq/xq/data/unified/UnifiedModels.kt
package cc.bbq.xq.data.unified

import cc.bbq.xq.AppStore

/**
 * 统一的用户信息模型
 */
data class UnifiedUser(
    val id: String,
    val displayName: String,
    val avatarUrl: String?
)

/**
 * 统一的评论模型
 */
data class UnifiedComment(
    val id: String,
    val content: String,
    val sendTime: Long,
    val sender: UnifiedUser,
    val childCount: Int,
    val childComments: List<UnifiedComment> = emptyList(),
    val fatherReply: UnifiedComment? = null,
    val raw: Any
)

/**
 * 统一的应用详情模型
 */
data class UnifiedAppDetail(
    val id: String,
    val store: AppStore,
    val packageName: String,
    val name: String,
    val versionCode: Long,
    val versionName: String,
    val iconUrl: String,
    val type: String,
    val previews: List<String>?,
    val description: String?,
    val updateLog: String?,
    val developer: String?,
    val size: String?,
    val uploadTime: Long,
    val user: UnifiedUser,
    val tags: List<String>?,
    val downloadCount: Int,
    val isFavorite: Boolean,
    val favoriteCount: Int,
    val reviewCount: Int,
    val downloadUrl: String?,
    val raw: Any
)

/**
 * 统一的应用列表项模型
 */
data class UnifiedAppItem(
    val uniqueId: String,
    val navigationId: String,
    val navigationVersionId: Long,
    val store: AppStore,
    val name: String,
    val iconUrl: String,
    val versionName: String
)

/**
 * 统一的应用分类模型
 */
data class UnifiedCategory(
    val id: String,
    val name: String,
    val icon: String? = null
)

/**
 * 统一的下载源模型
 */
data class UnifiedDownloadSource(
    val name: String,
    val url: String,
    val isOfficial: Boolean
)