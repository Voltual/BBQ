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

// KtorClient (小趣空间) -> Unified Models

fun KtorClient.AppItem.toUnifiedAppItem(): UnifiedAppItem {
    return UnifiedAppItem(
        uniqueId = "${AppStore.XIAOQU_SPACE}-${this.id}-${this.version_id}",
        navigationId = this.id.toString(),
        navigationVersionId = this.version_id,
        store = AppStore.XIAOQU_SPACE,
        name = this.name,
        iconUrl = this.icon_image_url,
        versionName = this.version
    )
}

fun KtorClient.User.toUnifiedUser(): UnifiedUser {
    return UnifiedUser(
        id = this.id.toString(),
        displayName = this.name,
        avatarUrl = this.avatar
    )
}

fun KtorClient.Comment.toUnifiedComment(): UnifiedComment {
    return UnifiedComment(
        id = this.id.toString(),
        content = this.content,
        sendTime = this.time.toLongOrNull() ?: 0L,
        sender = this.user.toUnifiedUser(),
        childCount = this.son_num,
        fatherReply = null, // 小趣空间的API不直接提供父评论的完整对象
        raw = this
    )
}

fun KtorClient.AppDetail.toUnifiedAppDetail(): UnifiedAppDetail {
    return UnifiedAppDetail(
        id = this.id.toString(),
        store = AppStore.XIAOQU_SPACE,
        packageName = this.apps_package_name,
        name = this.name,
        versionCode = this.version_code.toLong(),
        versionName = this.version,
        iconUrl = this.icon_image_url,
        type = this.type_name,
        previews = this.images_url,
        description = this.description,
        updateLog = this.update_description,
        developer = null, // 小趣空间API无此字段
        size = this.size,
        uploadTime = this.time.toLongOrNull() ?: 0L,
        user = this.user.toUnifiedUser(),
        tags = null, // 小趣空间API无此字段
        downloadCount = this.downloads,
        isFavorite = this.is_collection == 1,
        favoriteCount = this.collection,
        reviewCount = this.comments_num,
        raw = this
    )
}


// SineShopClient (弦应用商店) -> Unified Models

fun SineShopClient.SineShopApp.toUnifiedAppItem(): UnifiedAppItem {
    return UnifiedAppItem(
        uniqueId = "${AppStore.SIENE_SHOP}-${this.id}",
        navigationId = this.id.toString(),
        navigationVersionId = 0L, // 弦应用商店详情页不需要versionId
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