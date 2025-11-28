//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.data.repository

import cc.bbq.xq.data.unified.UnifiedAppDetail
import cc.bbq.xq.data.unified.UnifiedAppItem
import cc.bbq.xq.data.unified.UnifiedCategory
import cc.bbq.xq.data.unified.UnifiedComment

/**
 * 应用商店数据仓库的抽象接口。
 * 定义了所有应用商店数据源必须实现的通用操作，
 * 使得 ViewModel 可以与任何应用商店进行交互，而无需了解其具体实现。
 */
interface IAppStoreRepository {

    /**
     * 获取应用分类列表。
     */
    suspend fun getCategories(): Result<List<UnifiedCategory>>

    /**
     * 根据分类、用户等条件获取应用列表（分页）。
     *
     * @param categoryId 分类ID。对于特殊列表（如最新、最热），可使用预定义的特殊ID。
     * @param page 页码。
     * @param userId （可选）用于获取特定用户的资源列表。
     * @return 包含应用列表和总页数的Pair。
     */
    suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>>

    /**
     * 搜索应用（分页）。
     *
     * @param query 搜索关键词。
     * @param page 页码。
     * @param userId （可选）在特定用户的资源中搜索。
     * @return 包含搜索结果和总页数的Pair。
     */
    suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>>

    /**
     * 获取应用详情。
     *
     * @param appId 应用的唯一ID。
     * @param versionId （可选）应用的版本ID，某些商店可能不需要。
     * @return 统一的应用详情模型。
     */
    suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail>

    /**
     * 获取应用的评论列表（分页）。
     *
     * @param appId 应用的唯一ID。
     * @param page 页码。
     * @return 包含评论列表和总页数的Pair。
     */
    suspend fun getAppComments(appId: String, page: Int): Result<Pair<List<UnifiedComment>, Int>>

    /**
     * 发表评论或回复。
     *
     * @param appId 应用ID。对于回复，某些商店可能要求为特殊值。
     * @param content 评论内容。
     * @param parentCommentId （可选）父评论的ID，如果为null则为根评论。
     * @param mentionUserId （可选）需要@的用户的ID。
     * @return 操作结果。
     */
    suspend fun postComment(appId: String, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit>

    /**
     * 删除评论。
     *
     * @param commentId 要删除的评论ID。
     * @return 操作结果。
     */
    suspend fun deleteComment(commentId: String): Result<Unit>

    /**
     * 切换应用的收藏状态。
     *
     * @param appId 应用ID。
     * @param isCurrentlyFavorite 当前是否已收藏。
     * @return 操作成功后新的收藏状态。
     */
    suspend fun toggleFavorite(appId: String, isCurrentlyFavorite: Boolean): Result<Boolean>
}