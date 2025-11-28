//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.data.unified

import cc.bbq.xq.AppStore

/**
 * 统一的用户信息模型，用于在UI层显示评论者、上传者等信息。
 */
data class UnifiedUser(
    val id: String,
    val displayName: String,
    val avatarUrl: String?
)

/**
 * 统一的评论模型。
 */
data class UnifiedComment(
    val id: String,
    val content: String,
    val sendTime: Long,
    val sender: UnifiedUser,
    val childCount: Int,
    val fatherReply: UnifiedComment? = null,
    // 特定于平台的原始数据，以备不时之需，但应尽量避免在UI层直接使用
    val raw: Any 
)

/**
 * 统一的应用详情模型，包含AppDetailScreen所需的所有信息。
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
    // 特定于平台的原始数据
    val raw: Any
)

/**
 * 统一的应用列表项模型，用于ResourcePlazaScreen的网格布局。
 */
data class UnifiedAppItem(
    // 用于LazyVerticalGrid的唯一key
    val uniqueId: String,
    // 用于导航到详情页的ID
    val navigationId: String,
    // 用于导航到详情页的版本ID（对于弦应用商店可能为0）
    val navigationVersionId: Long,
    val store: AppStore,
    val name: String,
    val iconUrl: String,
    val versionName: String
)

/**
 * 统一的应用分类模型。
 */
data class UnifiedCategory(
    val id: String,
    val name: String,
    val icon: String? = null
)