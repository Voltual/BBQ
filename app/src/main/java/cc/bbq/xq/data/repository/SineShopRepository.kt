//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.data.repository

import cc.bbq.xq.SineShopClient
import cc.bbq.xq.data.unified.*
import kotlin.math.ceil

/**
 * 弦应用商店数据仓库实现。
 * 封装了所有与 SineShopClient (弦应用商店API) 的交互。
 */
class SineShopRepository : IAppStoreRepository {

    private fun calculateTotalPages(totalItems: Int, pageSize: Int = 10): Int {
        if (totalItems <= 0) return 1
        return ceil(totalItems.toDouble() / pageSize).toInt()
    }

    override suspend fun getCategories(): Result<List<UnifiedCategory>> {
        // 弦应用商店的分类是从API获取的
        return try {
            val result = SineShopClient.getAppTagList()
            result.map { tagList ->
                val specialCategories = listOf(
                    UnifiedCategory(id = "-1", name = "最新上传"),
                    UnifiedCategory(id = "-2", name = "最多下载")
                )
                specialCategories + tagList.map { it.toUnifiedCategory() }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        // 弦应用商店API不支持按用户ID获取应用列表，因此忽略 userId 参数
        return try {
            val result = when (categoryId) {
                "-1" -> SineShopClient.getLatestAppsList(page = page)
                "-2" -> SineShopClient.getMostDownloadedAppsList(page = page)
                else -> SineShopClient.getAppsList(tag = categoryId?.toIntOrNull(), page = page)
            }
            result.map { appListData ->
                val unifiedItems = appListData.list.map { it.toUnifiedAppItem() }
                // 弦应用商店的API返回总条目数，我们需要自己计算总页数
                val totalPages = calculateTotalPages(appListData.total)
                Pair(unifiedItems, totalPages)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        // 弦应用商店API不支持按用户ID搜索，因此忽略 userId 参数
        return try {
            val result = SineShopClient.getAppsList(keyword = query, page = page)
            result.map { appListData ->
                val unifiedItems = appListData.list.map { it.toUnifiedAppItem() }
                val totalPages = calculateTotalPages(appListData.total)
                Pair(unifiedItems, totalPages)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> {
        // versionId 在弦应用商店中不使用
        return try {
            val result = SineShopClient.getSineShopAppInfo(appId = appId.toInt())
            result.map { it.toUnifiedAppDetail() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppComments(appId: String, page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        return try {
            val result = SineShopClient.getSineShopAppComments(appId = appId.toInt(), page = page)
            result.map { commentData ->
                val unifiedComments = commentData.list.map { it.toUnifiedComment() }
                val totalPages = calculateTotalPages(commentData.total)
                Pair(unifiedComments, totalPages)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun postComment(appId: String, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> {
        return try {
            val result: Result<Int> = if (parentCommentId == null) {
                // 发表根评论
                SineShopClient.postSineShopAppRootComment(appId = appId.toInt(), content = content)
            } else {
                // 回复评论
                if (mentionUserId != null) {
                    SineShopClient.postSineShopAppReplyCommentWithMention(
                        commentId = parentCommentId.toInt(),
                        content = content,
                        mentionUserId = mentionUserId.toInt()
                    )
                } else {
                    SineShopClient.postSineShopAppReplyComment(
                        commentId = parentCommentId.toInt(),
                        content = content
                    )
                }
            }
            result.map { Unit } // 只要成功就返回 Unit
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        return try {
            SineShopClient.deleteSineShopComment(commentId = commentId.toInt())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(appId: String, isCurrentlyFavorite: Boolean): Result<Boolean> {
        // 弦应用商店似乎没有提供收藏接口，暂时返回一个模拟的失败结果。
        // 如果未来API支持了，可以在这里实现。
        return Result.failure(UnsupportedOperationException("弦应用商店不支持收藏功能。"))
    }
}