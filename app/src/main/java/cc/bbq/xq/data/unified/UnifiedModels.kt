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

/**
 * 统一的用户详情模型（支持小趣空间和弦应用商店）
 */
data class UnifiedUserDetail(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val description: String?,
    
    // 小趣空间特有字段
    val hierarchy: String? = null,  // 等级
    val followersCount: String? = null,
    val fansCount: String? = null,
    val postCount: String? = null,
    val likeCount: String? = null,
    val money: Int? = null,
    val commentCount: Int? = null,
    val seriesDays: Int? = null,  // 连续签到天数
    val lastActivityTime: String? = null,
    
    // 弦应用商店特有字段
    val userOfficial: String? = null,
    val userBadge: String? = null,
    val userStatus: Int? = null,
    val userStatusReason: String? = null,
    val banTime: Long? = null,
    val joinTime: Long? = null,
    val userPermission: Int? = null,
    val bindQq: Long? = null,
    val bindEmail: String? = null,
    val bindBilibili: Int? = null,
    val verifyEmail: Int? = null,
    val lastLoginDevice: String? = null,
    val lastOnlineTime: Long? = null,
    val uploadCount: Int? = null,
    val replyCount: Int? = null,
    
    val store: AppStore,
    val raw: Any? = null
)