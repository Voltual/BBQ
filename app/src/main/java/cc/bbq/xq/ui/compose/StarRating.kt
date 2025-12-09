// /app/src/main/java/cc/bbq/xq/ui/compose/StarRating.kt
package cc.bbq.xq.ui.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size // 导入 size 扩展函数
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 星级评分组件
 *
 * @param rating 当前评分值 (0-5)
 * @param maxStars 最大星数 (默认5星)
 * @param starSize 星星大小 (默认18.dp)
 * @param activeColor 激活状态颜色 (默认primary颜色)
 * @param inactiveColor 未激活状态颜色 (默认outline颜色)
 */
@Composable
fun StarRating(
    rating: Int,
    maxStars: Int = 5,
    starSize: Dp = 18.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 显示实心星
        repeat(rating.coerceAtMost(maxStars)) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = activeColor,
                modifier = Modifier.size(starSize) // 使用 size 扩展函数
            )
        }
        // 显示空心星补足剩余
        repeat(maxStars - rating.coerceAtMost(maxStars)) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = inactiveColor,
                modifier = Modifier.size(starSize) // 使用 size 扩展函数
            )
        }
        // 评分文本
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$rating.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}