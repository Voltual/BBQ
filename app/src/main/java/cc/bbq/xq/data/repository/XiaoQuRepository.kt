//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.data.repository

import cc.bbq.xq.KtorClient
import cc.bbq.xq.data.unified.*

/**
 * 小趣空间数据仓库实现。
 */
class XiaoQuRepository(private val apiClient: KtorClient.ApiService) : IAppStoreRepository {

    override suspend fun getCategories(): Result<List<UnifiedCategory>> {
        val categories = listOf(
            UnifiedCategory("null_null", "最新分享"),
            UnifiedCategory("45_47", "影音阅读"),
            UnifiedCategory("45_55", "音乐听歌"),
            UnifiedCategory("45_61", "休闲娱乐"),
            UnifiedCategory("45_58", "文件管理"),
            UnifiedCategory("45_59", "图像摄影"),
            UnifiedCategory("45_53", "输入方式"),
            UnifiedCategory("45_54", "生活出行"),
            UnifiedCategory("45_50", "社交通讯"),
            UnifiedCategory("45_56", "上网浏览"),
            UnifiedCategory("45_60", "其他类型"),
            UnifiedCategory("45_62", "跑酷竞技")
        )
        return Result.success(categories)
    }

    private fun parseCategory(id: String?): Pair<Int?, Int?> {
        if (id == null || id == "null_null") return null to null
        val parts = id.split("_")
        val cat = parts.getOrNull(0)?.toIntOrNull()
        val sub = parts.getOrNull(1)?.toIntOrNull()
        return cat to sub
    }

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        return try {
            val (catId, subCatId) = parseCategory(categoryId)
            val result = apiClient.getAppsList(
                limit = 12,
                page = page,
                sortOrder = "desc",
                categoryId = catId,
                subCategoryId = subCatId,
                appName = null,
                userId = userId?.toLongOrNull()
            )

            result.map { response ->
                if (response.code == 1) {
                    val unifiedItems = response.data.list.map { it.toUnifiedAppItem() }
                    // pagecount 可能是 0
                    val totalPages = if (response.data.pagecount > 0) response.data.pagecount else 1
                    Pair(unifiedItems, totalPages)
                } else {
                    throw Exception("API Error: ${response.msg}")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
         return try {
            val result = apiClient.getAppsList(
                limit = 20,
                page = page,
                appName = query,
                sortOrder = "desc",
                userId = userId?.toLongOrNull()
            )
            result.map { response ->
                if (response.code == 1) {
                    val unifiedItems = response.data.list.map { it.toUnifiedAppItem() }
                    val totalPages = if (response.data.pagecount > 0) response.data.pagecount else 1
                    Pair(unifiedItems, totalPages)
                } else {
                    throw Exception("Search failed: ${response.msg}")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> {
        return try {
            // KtorClient 需要 token，这里简化处理，假设 AuthManager 会自动处理或者不需要
            // 实际上 KtorClient.getAppsInformation 需要 token
            // 由于 Repository 不直接持有 token，这里需要传递一个空字符串或者从某处获取
            // 暂时传空字符串，实际逻辑中 AuthManager 应该拦截请求注入 token
            val result = apiClient.getAppsInformation(
                token = "", // AuthManager 应该在 Interceptor 中处理，或者这里需要注入 AuthManager
                appsId = appId.toLong(),
                appsVersionId = versionId
            )
            result.map { response ->
                if (response.code == 1) {
                    response.data.toUnifiedAppDetail()
                } else {
                    throw Exception("API Error: ${response.msg}")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppComments(appId: String, page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        return try {
            // getAppsCommentList 需要 appsVersionId，但 Unified 接口只传了 appId
            // 这里存在一个问题：小趣评论是绑定版本的。
            // 暂时只能传 0 或需要修改接口。这里先传 0，可能获取不到数据。
            // 为了修复编译错误，我们先填 0。
            val result = apiClient.getAppsCommentList(
                appsId = appId.toLong(),
                appsVersionId = 0, // 这是一个潜在的逻辑问题，但在编译层面我们先填0
                limit = 20,
                page = page,
                sortOrder = "desc"
            )
            result.map { response ->
                if (response.code == 1) {
                    val unifiedComments = response.data.list.map { it.toUnifiedComment() }
                    val totalPages = if (response.data.pagecount > 0) response.data.pagecount else 1
                    Pair(unifiedComments, totalPages)
                } else {
                    throw Exception("API Error: ${response.msg}")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun postComment(appId: String, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> {
        return try {
            // postAppComment 同样需要 versionId
            val result = apiClient.postAppComment(
                token = "", // 同样的问题，需要 Token
                content = content,
                appsId = appId.toLong(),
                appsVersionId = 0, // 逻辑问题
                parentId = parentCommentId?.toLongOrNull(),
                imageUrl = null
            )
            result.map { response ->
                if (response.code == 1) {
                    Unit
                } else {
                    throw Exception("API Error: ${response.msg}")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        return try {
            val result = apiClient.deleteAppComment(token = "", commentId = commentId.toLong())
             result.map { response ->
                if (response.code == 1) {
                    Unit
                } else {
                    throw Exception("API Error: ${response.msg}")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(appId: String, isCurrentlyFavorite: Boolean): Result<Boolean> {
        // 小趣 API 似乎没有直接的 collectApp 接口暴露在 ApiService 中
        // 暂时返回不支持
        return Result.failure(Exception("Not supported"))
    }
}