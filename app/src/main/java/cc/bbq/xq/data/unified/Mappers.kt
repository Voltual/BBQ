//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.data.unified

import cc.bbq.xq.AppStore
import cc.bbq.xq.KtorClient
import cc.bbq.xq.SineShopClient

// --- KtorClient (小趣空间) Mappers ---

fun KtorClient.AppItem.toUnifiedAppItem(): UnifiedAppItem {
    return UnifiedAppItem(
        uniqueId = "${AppStore.XIAOQU_SPACE}-${this.id}-${this.apps_version_id}",
        navigationId = this.id.toString(),
        navigationVersionId = this.apps_version_id,
        store = AppStore.XIAOQU_SPACE,
        name = this.appname, // 修正字段名
        iconUrl = this.app_icon, // 修正字段名
        versionName = "" // 列表页无版本名
    )
}

// KtorClient 没有独立的 User 类，通常是扁平数据，这里需要手动处理
fun createUnifiedUserFromKtor(id: Long, name: String, avatar: String): UnifiedUser {
    return UnifiedUser(
        id = id.toString(),
        displayName = name,
        avatarUrl = avatar
    )
}

fun KtorClient.Comment.toUnifiedComment(): UnifiedComment {
    return UnifiedComment(
        id = this.id.toString(),
        content = this.content,
        sendTime = this.time.toLongOrNull() ?: 0L,
        // 从 Comment 对象中提取用户信息
        sender = createUnifiedUserFromKtor(this.userid, this.nickname, this.usertx),
        childCount = this.sub_comments_count, // 修正字段名
        fatherReply = null, // KtorClient.Comment 没有父评论对象字段
        raw = this
    )
}

fun KtorClient.AppDetail.toUnifiedAppDetail(): UnifiedAppDetail {
    return UnifiedAppDetail(
        id = this.id.toString(),
        store = AppStore.XIAOQU_SPACE,
        packageName = "", // AppDetail 中没有 package_name 字段? 检查发现确实没有直接的 package_name，可能在 app_explain 中
        name = this.appname,
        versionCode = 0L, // AppDetail 没有 version_code
        versionName = this.version,
        iconUrl = this.app_icon,
        type = this.category_name, // 使用分类名作为类型
        previews = this.app_introduction_image_array,
        description = this.app_introduce,
        updateLog = this.app_explain, // 使用说明作为更新日志
        developer = null,
        size = this.app_size,
        uploadTime = this.create_time.toLongOrNull() ?: 0L, // 可能是字符串时间，这里简化处理
        user = createUnifiedUserFromKtor(this.userid, this.nickname, this.usertx),
        tags = listOf(this.category_name, this.sub_category_name),
        downloadCount = this.download_count,
        isFavorite = false, // AppDetail 没有收藏状态字段
        favoriteCount = 0, // AppDetail 没有收藏数字段
        reviewCount = this.comment_count,
        raw = this
    )
}

// --- SineShopClient (弦应用商店) Mappers ---

fun SineShopClient.SineShopApp.toUnifiedAppItem(): UnifiedAppItem {
    return UnifiedAppItem(
        uniqueId = "${AppStore.SIENE_SHOP}-${this.id}",
        navigationId = this.id.toString(),
        navigationVersionId = 0L,
        store = AppStore.SIENE_SHOP,
        name = this.app_name,
        iconUrl = this.app_icon,
        versionName = this.version_name
    )
}

fun SineShopClient.SineShopUserInfoLite.toUnifiedUser(): UnifiedUser {
    return UnifiedUser(
        id = this.id.toString(),
        displayName = this.displayName,
        avatarUrl = this.userAvatar
    )
}

fun SineShopClient.SineShopComment.toUnifiedComment(): UnifiedComment {
    return UnifiedComment(
        id = this.id.toString(),
        content = this.content,
        sendTime = this.send_time,
        sender = this.sender.toUnifiedUser(),
        childCount = this.child_count,
        fatherReply = this.father_reply?.toUnifiedComment(),
        raw = this
    )
}

fun SineShopClient.SineShopAppDetail.toUnifiedAppDetail(): UnifiedAppDetail {
    return UnifiedAppDetail(
        id = this.id.toString(),
        store = AppStore.SIENE_SHOP,
        packageName = this.package_name,
        name = this.app_name,
        versionCode = this.version_code.toLong(),
        versionName = this.version_name,
        iconUrl = this.app_icon,
        type = this.app_type,
        previews = this.app_previews,
        description = this.app_describe,
        updateLog = this.app_update_log,
        developer = this.app_developer,
        size = this.download_size,
        uploadTime = this.upload_time,
        user = this.user.toUnifiedUser(),
        tags = this.tags?.map { it.name },
        downloadCount = this.download_count,
        isFavorite = this.is_favourite == 1,
        favoriteCount = this.favourite_count,
        reviewCount = this.review_count,
        raw = this
    )
}

fun SineShopClient.AppTag.toUnifiedCategory(): UnifiedCategory {
    return UnifiedCategory(
        id = this.id.toString(),
        name = this.name,
        icon = this.icon
    )
}