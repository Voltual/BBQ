//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.plaza

/**
 * 应用分类的共享数据模型
 * @param categoryId 主分类 ID (可空，用于"最新分享"等)
 * @param subCategoryId 子分类 ID (可空)
 * @param categoryName 分类显示名称
 */
data class AppCategory(
    val categoryId: Int?,
    val subCategoryId: Int?,
    val categoryName: String
)