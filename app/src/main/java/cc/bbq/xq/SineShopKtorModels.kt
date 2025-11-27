//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 新增：用户信息模型
@Serializable
data class SineShopUserInfo(
    val id: Int,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("user_describe") val userDescribe: String?,
    @SerialName("user_official") val userOfficial: String?,
    @SerialName("user_avatar") val userAvatar: String?,
    @SerialName("user_badge") val userBadge: String?,
    @SerialName("user_status") val userStatus: Int,
    @SerialName("user_status_reason") val userStatusReason: String?,
    @SerialName("ban_time") val banTime: Int,
    @SerialName("join_time") val joinTime: Long,
    @SerialName("user_permission") val userPermission: Int,
    @SerialName("bind_qq") val bindQq: Long?,
    @SerialName("bind_email") val bindEmail: String?,
    @SerialName("bind_bilibili") val bindBilibili: Int?,
    @SerialName("verify_email") val verifyEmail: Int,
    @SerialName("last_login_device") val lastLoginDevice: String?,
    @SerialName("last_online_time") val lastOnlineTime: Long,
    @SerialName("pub_favourite") val pubFavourite: JsonObjectWrapper?,
    @SerialName("upload_count") val uploadCount: Int,
    @SerialName("reply_count") val replyCount: Int
)

@Serializable
data class JsonObjectWrapper(
    @SerialName("Int64") val int64: Long?,
    @SerialName("Valid") val valid: Boolean?
)

// 新增：应用分类标签模型
@Serializable
data class AppTag(
    val id: Int,
    val name: String,
    val icon: String?
)

// 新增：应用数据模型
@Serializable
data class SineShopApp(
    val id: Int,
    @SerialName("package_name") val package_name: String,
    @SerialName("app_icon") val app_icon: String,
    @SerialName("app_name") val app_name: String,
    @SerialName("version_code") val version_code: Int,
    @SerialName("version_name") val version_name: String,
    @SerialName("app_type") val app_type: String,
    @SerialName("app_version_type") val app_version_type: String,
    @SerialName("app_abi") val app_abi: Int,
    @SerialName("app_sdk_min") val app_sdk_min: Int,
    @SerialName("version_count") val version_count: Int
)

// 为标签列表定义单独的数据模型，保持与 AppTag 一致
@Serializable
data class AppTagListData(
    val total: Int,
    val list: List<AppTag>
)

// 为应用列表定义单独的数据模型
@Serializable
data class AppListData(
    val total: Int,
    val list: List<SineShopApp>
)

// 新增：应用详情数据模型
@Serializable
data class SineShopAppDetail(
    val id: Int,
    @SerialName("package_name") val package_name: String,
    @SerialName("app_name") val app_name: String,
    @SerialName("version_code") val version_code: Int,
    @SerialName("version_name") val version_name: String,
    @SerialName("app_icon") val app_icon: String,
    @SerialName("app_type") val app_type: String,
    @SerialName("user_avatar") val userAvatar: String?,
    @SerialName("app_version_type") val app_version_type: String,
    @SerialName("app_abi") val app_abi: Int,
    @SerialName("app_sdk_min") val app_sdk_min: Int,
    @SerialName("app_previews") val app_previews: List<String>?,
    @SerialName("app_describe") val app_describe: String?,
    @SerialName("app_update_log") val app_update_log: String?,
    @SerialName("app_developer") val app_developer: String?,
    @SerialName("app_source") val app_source: String?,
    @SerialName("upload_message") val upload_message: String?,
    @SerialName("download_size") val download_size: String?,
    @SerialName("upload_time") val upload_time: Long,
    @SerialName("update_time") val update_time: Long,
    @SerialName("user") val user: SineShopUserInfo,
    @SerialName("tags") val tags: List<AppTag>?,
    @SerialName("download_count") val download_count: Int,
    @SerialName("is_favourite") val is_favourite: Int,
    @SerialName("favourite_count") val favourite_count: Int,
    @SerialName("review_count") val review_count: Int
    // 注意：这里没有评论列表，需要单独获取
)

// 新增：评论数据模型
@Serializable
data class SineShopComment(
    val id: Int,
    val content: String,
    @SerialName("send_time") val send_time: Long,
    @SerialName("father_reply_id") val father_reply_id: Int,
    @SerialName("sender") val sender: SineShopUserInfo,
    @SerialName("child_count") val child_count: Int,
    @SerialName("father_reply") val father_reply: SineShopComment? // 可能为 null
)

// 为评论列表定义单独的数据模型
@Serializable
data class SineShopCommentListData(
    val total: Int,
    val list: List<SineShopComment>
)