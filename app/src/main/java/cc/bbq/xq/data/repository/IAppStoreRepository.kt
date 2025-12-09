// /app/src/main/java/cc/bbq/xq/data/repository/IAppStoreRepository.kt
package cc.bbq.xq.data.repository

import cc.bbq.xq.data.unified.*
import java.io.File

interface IAppStoreRepository {
    suspend fun getCategories(): Result<List<UnifiedCategory>>
    suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>>
    suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>>
    suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail>
    suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>>
    suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit>    
    suspend fun deleteComment(commentId: String): Result<Unit>
    suspend fun toggleFavorite(appId: String, isCurrentlyFavorite: Boolean): Result<Boolean>
    suspend fun deleteApp(appId: String, versionId: Long): Result<Unit>
    suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>>
        // 新增：发布应用
    suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit>
    suspend fun getMyReviews(page: Int): Result<Pair<List<UnifiedComment>, Int>>
    // 新增：上传图片 (主要用于小趣空间图床)
    suspend fun uploadImage(file: File, type: String): Result<String>
    suspend fun getMyComments(page: Int): Result<Pair<List<UnifiedComment>, Int>>
    
    // 新增：上传APK (主要用于小趣空间第三方图床)
suspend fun deleteReview(reviewId: String): Result<Unit>
    suspend fun uploadApk(file: File, serviceType: String): Result<String>

}