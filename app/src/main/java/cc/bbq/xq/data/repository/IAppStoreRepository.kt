// /app/src/main/java/cc/bbq/xq/data/repository/IAppStoreRepository.kt
package cc.bbq.xq.data.repository

import cc.bbq.xq.data.unified.UnifiedAppDetail
import cc.bbq.xq.data.unified.UnifiedAppItem
import cc.bbq.xq.data.unified.UnifiedCategory
import cc.bbq.xq.data.unified.UnifiedComment
import cc.bbq.xq.data.unified.UnifiedDownloadSource

interface IAppStoreRepository {
    suspend fun getCategories(): Result<List<UnifiedCategory>>
    suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>>
    suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>>
    suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail>
    suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>>
    
    // 修改：增加 versionId 参数
    suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit>
    
    suspend fun deleteComment(commentId: String): Result<Unit>
    suspend fun toggleFavorite(appId: String, isCurrentlyFavorite: Boolean): Result<Boolean>
    suspend fun deleteApp(appId: String, versionId: Long): Result<Unit>
    suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>>
}