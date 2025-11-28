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
import cc.bbq.xq.ui.plaza.AppCategory

/**
 * 小趣空间数据仓库实现。
 * 封装了所有与 KtorClient (小趣空间API) 的交互。
 */
class XiaoQuRepository(private val apiClient: KtorClient.ApiServiceImpl) : IAppStoreRepository {

    override suspend fun getCategories(): Result<List<UnifiedCategory>> {
        // 小趣空间的分类是本地硬编码的
        val categories = listOf(
            AppCategory(null, null, "最新分享"),
            AppCategory(45, 47, "影音阅读"),
            AppCategory(45, 55, "音乐听歌"),
            AppCategory(45, 61, "休闲娱乐"),
            AppCategory(45, 58, "文件管理"),
            AppCategory(45, 59, "图像摄影"),
            AppCategory(45, 53, "输入方式"),
            AppCategory(45, 54, "生活出行"),
            AppCategory(45, 50, "社交通讯"),
            AppCategory(45, 56, "上网浏览"),
            AppCategory(45, 60, "其他类型"),
            AppCategory(45, 62, "跑酷竞技")
        )

        val unifiedCategories = categories.map {
            // 使用 categoryId 和 subCategoryId 组合成一个唯一的ID
            val id = "${it.categoryId ?: "latest"}_${it.subCategoryId ?: "all"}"
            UnifiedCategory(id = id, name = it.categoryName)
        }
        return Result.success(unifiedCategories)
    }

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        return try {
            val parts = categoryId?.split("_")
            val catId = parts?.getOrNull(0)?.takeIf { it != "latest" }?.toIntOrNull()
            val subCatId = parts?.getOrNull(1)?.takeIf { it != "all" }?.toIntOrNull()

            val result = apiClient.getAppsList(
                limit = 12, // 每页数量
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
                    val totalPages = response.data.pagecount.coerceAtLeast(1)
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
                    val totalPages = response.data.pagecount.coerceAtLeast(1)
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
            val result = apiClient.getAppDetails(
                id = appId.toLong(),
                versionId = versionId
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
            val result = apiClient.getAppComments(
                appsId = appId.toLong(),
                page = page
            )
            result.map { response ->
                if (response.code == 1) {
                    val unifiedComments = response.data.list.map { it.toUnifiedComment() }
                    val totalPages = response.data.page_count.coerceAtLeast(1)
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
            val result = apiClient.submitAppComment(
                appsId = appId.toLong(),
                content = content,
                commentId = parentCommentId?.toLongOrNull() ?: 0L // 小趣空间API中，0代表根评论
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
            val result = apiClient.deleteComment(id = commentId.toLong())
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
        return try {
            val result = apiClient.collectApp(appsId = appId.toLong())
            result.map { response ->
                // 小趣空间API的收藏接口是toggle形式，不区分当前状态
                // 响应msg会是"收藏成功"或"已取消收藏"
                !isCurrentlyFavorite // 假设操作总是成功的，直接返回相反的状态
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}