// /app/src/main/java/cc/bbq/xq/data/repository/IAppStoreRepository.kt
package cc.bbq.xq.data.repository

import cc.bbq.xq.data.unified.UnifiedAppDetail
import cc.bbq.xq.data.unified.UnifiedAppItem
import cc.bbq.xq.data.unified.UnifiedCategory
import cc.bbq.xq.data.unified.UnifiedComment

interface IAppStoreRepository {
    suspend fun getCategories(): Result<List<UnifiedCategory>>
    suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>>
    suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>>
    suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail>
    suspend fun getAppComments(appId: String, page: Int): Result<Pair<List<UnifiedComment>, Int>>
    suspend fun postComment(appId: String, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit>
    suspend fun deleteComment(commentId: String): Result<Unit>
    suspend fun toggleFavorite(appId: String, isCurrentlyFavorite: Boolean): Result<Boolean>
    
    // [新增] 删除应用
    suspend fun deleteApp(appId: String, versionId: Long): Result<Unit>
}