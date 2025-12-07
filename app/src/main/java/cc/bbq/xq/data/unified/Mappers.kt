                  // /app/src/main/java/cc/bbq/xq/data/unified/Mappers.kt
package cc.bbq.xq.data.unified

import cc.bbq.xq.AppStore
import cc.bbq.xq.KtorClient
import cc.bbq.xq.SineShopClient
import cc.bbq.xq.KtorClient.AppItem
import cc.bbq.xq.KtorClient.Comment
import cc.bbq.xq.KtorClient.AppComment
import cc.bbq.xq.KtorClient.AppDetail
import cc.bbq.xq.KtorClient.UserInformationData
import cc.bbq.xq.SineShopClient.SineShopApp
import cc.bbq.xq.SineShopClient.SineShopUserInfoLite
import cc.bbq.xq.SineShopClient.SineShopComment
import cc.bbq.xq.SineShopClient.SineShopAppDetail
import cc.bbq.xq.SineShopClient.AppTag
import cc.bbq.xq.SineShopClient.SineShopDownloadSource
import cc.bbq.xq.SineShopClient.SineShopUserInfo

// --- KtorClient (小趣空间) Mappers ---

fun KtorClient.AppItem.toUnifiedAppItem(): UnifiedAppItem {
    return UnifiedAppItem(
        uniqueId = "${AppStore.XIAOQU_SPACE}-${this.id}-${this.apps_version_id}",
        navigationId = this.id.toString(),
        navigationVersionId = this.apps_version_id,
        store = AppStore.XIAOQU_SPACE,
        name = this.appname,
        iconUrl = this.app_icon,
        versionName = ""
    )
}

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
        sender = createUnifiedUserFromKtor(this.userid, this.nickname, this.usertx),
        childCount = this.sub_comments_count,
        // KtorClient.Comment 没有直接包含子评论列表，设为空
        childComments = emptyList(), 
        fatherReply = null,
        raw = this
    )
}

fun KtorClient.AppComment.toUnifiedComment(): UnifiedComment {
    val father = if (this.parentid != null && this.parentid != 0L) {
        UnifiedComment(
            id = this.parentid.toString(),
            content = this.parentcontent ?: "",
            sendTime = 0L,
            sender = createUnifiedUserFromKtor(0L, this.parentnickname ?: "未知用户", ""),
            childCount = 0,
            fatherReply = null,
            raw = this
        )
    } else null

    return UnifiedComment(
        id = this.id.toString(),
        content = this.content,
        sendTime = this.time.toLongOrNull() ?: 0L,
        sender = createUnifiedUserFromKtor(this.userid, this.nickname, this.usertx),
        childCount = 0,
        fatherReply = father,
        raw = this
    )
}

fun KtorClient.AppDetail.toUnifiedAppDetail(): UnifiedAppDetail {
    return UnifiedAppDetail(
        id = this.id.toString(),
        store = AppStore.XIAOQU_SPACE,
        packageName = "", 
        name = this.appname,
        versionCode = 0L,
        versionName = this.version,
        iconUrl = this.app_icon,
        type = this.category_name,
        previews = this.app_introduction_image_array,
        description = this.app_introduce,
        updateLog = this.app_explain,
        developer = null,
        size = this.app_size,
        uploadTime = this.create_time.toLongOrNull() ?: 0L,
        user = createUnifiedUserFromKtor(this.userid, this.nickname, this.usertx),
        tags = listOf(this.category_name, this.sub_category_name),
        downloadCount = this.download_count,
        isFavorite = false,
        favoriteCount = 0,
        reviewCount = this.comment_count,
        downloadUrl = if (this.is_pay == 0 || this.is_user_pay) this.download else null,
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

// 修改 Mappers.kt 中的映射函数
fun SineShopClient.SineShopComment.toUnifiedComment(): UnifiedComment {
    return UnifiedComment(
        id = this.id.toString(),
        content = this.content,
        sendTime = this.send_time,
        sender = this.sender.toUnifiedUser(),
        childCount = this.child_count,
        fatherReply = this.father_reply?.toUnifiedComment(),
        raw = this,
        // 直接在这里处理 appId 的逻辑
        appId = if (this.app_id == -1) null else this.app_id.toString(),
        versionId = null
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
        downloadUrl = null,
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

fun SineShopClient.SineShopDownloadSource.toUnifiedDownloadSource(): UnifiedDownloadSource {
    return UnifiedDownloadSource(
        name = this.name,
        url = this.url,
        isOfficial = this.isExtra == 1 // 假设 1 代表官方/主要线路
    )
}

fun SineShopClient.SineShopUserInfo.toUnifiedUser(): UnifiedUser {
    return UnifiedUser(
        id = this.id.toString(),
        displayName = this.displayName,
        avatarUrl = this.userAvatar
    )
}

// 修正 KtorClient (小趣空间) 映射中的类型错误
fun KtorClient.UserInformationData.toUnifiedUserDetail(): UnifiedUserDetail {
    return UnifiedUserDetail(
        id = this.id,
        username = this.username,
        displayName = this.nickname,
        avatarUrl = this.usertx,
        description = null, // 小趣空间无此字段
        hierarchy = this.hierarchy,
        followersCount = this.fanscount,
        fansCount = this.followerscount,
        postCount = this.postcount,
        likeCount = this.likecount,
        money = this.money, // Int 类型，正确
        commentCount = this.commentcount?.toIntOrNull(), // 从 String? 转换为 Int?
        seriesDays = this.series_days,
        lastActivityTime = this.last_activity_time,
        store = AppStore.XIAOQU_SPACE,
        raw = this
    )
}

// 修正 SineShopClient (弦应用商店) 映射
fun SineShopClient.SineShopUserInfo.toUnifiedUserDetail(): UnifiedUserDetail {
    return UnifiedUserDetail(
        id = this.id.toLong(),
        username = this.username,
        displayName = this.displayName,
        avatarUrl = this.userAvatar,
        description = this.userDescribe,
        userOfficial = this.userOfficial,
        userBadge = this.userBadge,
        userStatus = this.userStatus,
        userStatusReason = this.userStatusReason,
        banTime = this.banTime?.toLong(),
        joinTime = this.joinTime,
        userPermission = this.userPermission,
        bindQq = this.bindQq,
        bindEmail = this.bindEmail,
        bindBilibili = this.bindBilibili,
        verifyEmail = this.verifyEmail,
        lastLoginDevice = this.lastLoginDevice,
        lastOnlineTime = this.lastOnlineTime,
        uploadCount = this.uploadCount,
        replyCount = this.replyCount,
        store = AppStore.SIENE_SHOP,
        raw = this
    )
}