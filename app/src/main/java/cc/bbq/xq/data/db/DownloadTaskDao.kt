// 文件路径: cc/bbq/xq/data/db/DownloadTaskDao.kt
package cc.bbq.xq.data.db

import androidx.room.*
import cc.bbq.xq.service.download.DownloadTask
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(downloadTask: DownloadTask)

    @Update
    suspend fun update(downloadTask: DownloadTask)

    @Delete
    suspend fun delete(downloadTask: DownloadTask)

    @Query("SELECT * FROM download_tasks WHERE url = :url")
    fun getDownloadTask(url: String): Flow<DownloadTask?>

    @Query("SELECT * FROM download_tasks")
    fun getAllDownloadTasks(): Flow<List<DownloadTask>>
}