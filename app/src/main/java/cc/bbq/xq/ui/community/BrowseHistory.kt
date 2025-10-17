//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.community

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat // 修正 import
import java.util.Date
import java.util.Locale

// 核心修改 #1: 添加 @Entity 和 @PrimaryKey 注解
@Entity(tableName = "browse_history")
data class BrowseHistory(
    @PrimaryKey val postId: Long,
    val title: String,
    val previewContent: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    // 核心修改 #2: 添加 @Ignore 注解，告诉 ROOM 不要尝试存储这个计算属性
    @Ignore
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun formattedTime(): String {
        val date = Date(timestamp)
        return formatter.format(date)
    }
}