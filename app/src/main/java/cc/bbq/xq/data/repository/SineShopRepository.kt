// /app/src/main/java/cc/bbq/xq/data/repository/SineShopRepository.kt
package cc.bbq.xq.data.repository

import cc.bbq.xq.SineShopClient
import cc.bbq.xq.data.unified.*
import java.io.File
import kotlin.math.ceil

class SineShopRepository : IAppStoreRepository {

    private fun calculateTotalPages(totalItems: Int, pageSize: Int = 10): Int {
        if (totalItems <= 0) return 1
        return ceil(totalItems.toDouble() / pageSize).toInt()
    }

    override suspend fun getCategories(): Result<List<UnifiedCategory>> {
        return try {
            val result = SineShopClient.getAppTagList()
            result.map { tagList ->
                val specialCategories = listOf(
                    UnifiedCategory(id = "-1", name = "最新上传"),
                    UnifiedCategory(id = "-2", name = "最多下载"),
                    UnifiedCategory(id = "-3", name = "我的上传"),
                    UnifiedCategory(id = "-4", name = "我的收藏"),
                    UnifiedCategory(id = "-5", name = "历史足迹")
                )
                specialCategories + tagList.map { it.toUnifiedCategory() }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        return try {
            val result = when {
                userId != null -> {
                    // 获取指定用户上传的应用列表
                    SineShopClient.getAppsList(userId = userId.toInt(), page = page)
                }
                categoryId == "-1" -> SineShopClient.getLatestAppsList(page = page)
                categoryId == "-2" -> SineShopClient.getMostDownloadedAppsList(page = page)
                categoryId == "-3" -> SineShopClient.getMyUploadAppsList(page = page)      // 我的上传
                categoryId == "-4" -> SineShopClient.getMyFavouriteAppsList(page = page)  // 我的收藏
                categoryId == "-5" -> SineShopClient.getMyHistoryAppsList(page = page)     // 我的历史足迹
                else -> {
                    // 修正：对于其他情况，使用 appid 参数而不是 tag
                    if (categoryId != null && categoryId.toIntOrNull() != null) {
                        SineShopClient.getAppsList(appid = categoryId.toInt(), page = page)
                    } else {
                        // 如果 categoryId 为 null 或不是数字，返回空列表
                        SineShopClient.AppListData(0, emptyList())
                    }
                }
            }
            result.map { appListData ->
                val unifiedItems = appListData.list.map { it.toUnifiedAppItem() }
                val totalPages = calculateTotalPages(appListData.total)
                Pair(unifiedItems, totalPages)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
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
        return try {
            val result = SineShopClient.getSineShopAppInfo(appId = appId.toInt())
            result.map { it.toUnifiedAppDetail() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> {
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

    override suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> {
        return try {
            // 弦应用商店忽略 versionId
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
            result.map { Unit }
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
        return Result.failure(UnsupportedOperationException("弦应用商店不支持收藏功能。"))
    }

    override suspend fun deleteApp(appId: String, versionId: Long): Result<Unit> {
        return Result.failure(UnsupportedOperationException("弦应用商店不支持删除应用。"))
    }

    override suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> {
        return try {
            val result = SineShopClient.getAppDownloadSources(appId.toInt())
            result.map { sources ->
                sources.map { it.toUnifiedDownloadSource() }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit> {
        return Result.failure(UnsupportedOperationException("弦应用商店不支持发布应用。"))
    }

    override suspend fun uploadImage(file: File, type: String): Result<String> {
        return Result.failure(UnsupportedOperationException("弦应用商店不支持上传图片。"))
    }

    override suspend fun uploadApk(file: File, serviceType: String): Result<String> {
        return Result.failure(UnsupportedOperationException("弦应用商店不支持上传 APK。"))
    }
}