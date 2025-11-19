// RoundScreenAdapter.kt
package cc.bbq.xq

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.aspectRatio

// 定义一个 Shape，用于裁剪成内切正方形
class InsetSquare(private val insetRatio: Float = 0f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val side = minOf(size.width, size.height)
        val xOffset = (size.width - side) / 2
        val yOffset = (size.height - side) / 2

        // 计算内边距
        val inset = side * insetRatio

        return Outline.Rectangle(
            androidx.compose.ui.geometry.Rect(
                xOffset + inset,
                yOffset + inset,
                xOffset + side - inset,
                yOffset + side - inset
            )
        )
    }
}

// 创建一个 Modifier 扩展函数，用于应用内切正方形裁剪
fun Modifier.roundScreenAdaptation(insetRatio: Float = 0f): Modifier = this
    .aspectRatio(1f) // 强制宽高比为 1:1，形成正方形
    .clip(InsetSquare(insetRatio)) // 使用自定义 Shape 进行裁剪