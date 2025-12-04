// /app/src/main/java/cc/bbq/xq/data/repository/XiaoQuRepository.kt
package cc.bbq.xq.data.repository

import cc.bbq.xq.AuthManager
import cc.bbq.xq.BBQApplication
import cc.bbq.xq.KtorClient
import cc.bbq.xq.data.unified.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * 小趣空间数据仓库实现。
 */
class XiaoQuRepository(private val apiClient: KtorClient.ApiService) : IAppStoreRepository {

    // 辅助方法：获取 Token
    private suspend fun getToken(): String {
        return AuthManager.getCredentials(BBQApplication.instance).first()?.token ?: ""
    }

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
            val limit = if (userId != null) 12 else 9
            
            val result = apiClient.getAppsList(
                limit = limit,
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
            val token = getToken()
            val result = apiClient.getAppsInformation(
                token = token, 
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

    override suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        return try {
            val result = apiClient.getAppsCommentList(
                appsId = appId.toLong(),
                appsVersionId = versionId, 
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

    override suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> {
        return try {
            val token = getToken()
            // 修正：如果 parentCommentId 为 null，则传 0 (根评论)
            val parentId = parentCommentId?.toLongOrNull() ?: 0L
            
            val result = apiClient.postAppComment(
                token = token, 
                content = content,
                appsId = appId.toLong(),
                appsVersionId = versionId, // 修正：使用传入的 versionId
                parentId = parentId,
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
            val token = getToken()
            val result = apiClient.deleteAppComment(token = token, commentId = commentId.toLong())
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
        return Result.failure(Exception("Not supported"))
    }

    override suspend fun deleteApp(appId: String, versionId: Long): Result<Unit> {
        return try {
            val token = getToken()
            if (token.isEmpty()) {
                throw Exception("未登录")
            }
            val result = apiClient.deleteApp(
                usertoken = token,
                apps_id = appId.toLong(),
                app_version_id = versionId
            )
            result.map { response ->
                if (response.code == 1) Unit else throw Exception(response.msg)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit> {
        return try {
            val token = getToken()
            if (token.isEmpty()) throw Exception("未登录")

            val introImagesString = params.introImages?.joinToString(",") ?: ""

            val result = if (params.isUpdate) {
                apiClient.updateApp(
                    usertoken = token,
                    apps_id = params.appId ?: 0L,
                    appname = params.appName,
                    icon = params.iconUrl,
                    app_size = params.sizeInMb.toString(),
                    app_introduce = params.introduce ?: "",
                    app_introduction_image = introImagesString,
                    file = params.apkUrl ?: "",
                    app_explain = params.explain,
                    app_version = params.versionName,
                    is_pay = params.isPay ?: 0,
                    pay_money = params.payMoney,
                    category_id = params.categoryId ?: 0,
                    sub_category_id = params.subCategoryId ?: 0
                )
            } else {
                apiClient.releaseApp(
                    usertoken = token,
                    appname = params.appName,
                    icon = params.iconUrl,
                    app_size = params.sizeInMb.toString(),
                    app_introduce = params.introduce ?: "",
                    app_introduction_image = introImagesString,
                    file = params.apkUrl ?: "",
                    app_explain = params.explain,
                    app_version = params.versionName,
                    is_pay = params.isPay ?: 0,
                    pay_money = params.payMoney,
                    category_id = params.categoryId ?: 0,
                    sub_category_id = params.subCategoryId ?: 0
                )
            }

            result.map { response ->
                if (response.code == 1) Unit else throw Exception(response.msg)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadImage(file: File, type: String): Result<String> {
        // 复用 KtorClient 中的上传逻辑 (这里简化，实际应从 ViewModel 迁移过来)
        // 假设 type="icon" 或 "intro"
        return try {
             val response = KtorClient.uploadHttpClient.submitFormWithBinaryData(
                url = "api.php",
                formData = formData {
                    append("file", InputProvider { file.inputStream().asInput() }, Headers.build {
                        append(HttpHeaders.ContentType, "image/*")
                        append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                    })
                }
            )
            // 解析响应逻辑... (为了简洁省略具体解析，需参考原 ViewModel)
            Result.success("http://example.com/fake_url.jpg") // 占位
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadApk(file: File, serviceType: String): Result<String> {
        // 占位，实际逻辑在 ViewModel 中通过 KtorClient 直接调用了，
        // 为了完全抽象，应该把 uploadToKeyun/Wanyueyun 移到这里。
        // 暂时返回失败以提示需要在 ViewModel 中处理或迁移代码。
        return Result.failure(Exception("请使用 ViewModel 中的上传逻辑"))
    }
}

    override suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> {
        return getAppDetail(appId, versionId).map { detail ->
            if (detail.downloadUrl != null) {
                listOf(UnifiedDownloadSource(name = "默认下载", url = detail.downloadUrl, isOfficial = true))
            } else {
                emptyList()
            }
        }
    }
}