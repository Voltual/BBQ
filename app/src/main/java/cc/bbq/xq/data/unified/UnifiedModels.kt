// /app/src/main/java/cc/bbq/xq/data/unified/UnifiedModels.kt
package cc.bbq.xq.data.unified

import cc.bbq.xq.AppStore
import java.io.File

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
/**
 * 统一的发布应用参数
 * 包含两个平台所需的所有字段，使用可空类型处理差异
 */
data class UnifiedAppReleaseParams(
    val store: AppStore,
    // 通用字段
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sizeInMb: Double,
    val iconFile: File?, // 本地文件
    val iconUrl: String?, // 网络URL (小趣空间用)
    val apkFile: File?,   // 本地文件 (弦开放平台用)
    val apkUrl: String?,  // 网络URL (小趣空间用)
    
    // 小趣空间特有
    val introduce: String? = null, // 资源介绍
    val explain: String? = null,   // 适配说明
    val introImages: List<String>? = null, // 介绍图URL列表
    val categoryId: Int? = null,
    val subCategoryId: Int? = null,
    val isPay: Int? = null,
    val payMoney: String? = null,
    val isUpdate: Boolean = false,
    val appId: Long? = null, // 更新时需要
    val appVersionId: Long? = null, // 更新/删除时需要

    // 弦开放平台特有
    val appTypeId: Int? = null,
    val appVersionTypeId: Int? = null,
    val appTags: String? = null, // ID字符串
    val sdkMin: Int? = null,
    val sdkTarget: Int? = null,
    val developer: String? = null,
    val source: String? = null,
    val describe: String? = null, // 应用描述
    val updateLog: String? = null,
    val uploadMessage: String? = null,
    val keyword: String? = null,
    val isWearOs: Int? = null,
    val abi: Int? = null,
    val screenshots: List<File>? = null // 本地截图文件列表
)