//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui

import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri
import java.net.URLEncoder
import cc.bbq.xq.AppStore
import java.nio.charset.StandardCharsets

/**
 * 定义应用中所有导航目的地的类型安全契约。
 * 这是单 Activity 架构的核心。
 */
sealed interface AppDestination {
    val route: String

    companion object {
        // 参数键常量，避免魔法字符串
        const val ARG_POST_ID = "postId"
        const val ARG_USER_ID = "userId"
        const val ARG_IS_MY_RESOURCE = "isMyResource"
        const val ARG_BVID = "bvid"
        const val ARG_IMAGE_URL = "imageUrl"
        const val ARG_APP_ID = "appId"
        const val ARG_VERSION_ID = "versionId"
        const val ARG_APP_NAME = "appName"
        const val ARG_PAY_MONEY = "payMoney"
        const val ARG_PRICE = "price" // 修复缺失的常量
        const val ARG_POST_TITLE = "postTitle"
        const val ARG_ICON_URL = "iconUrl"
        const val ARG_PREVIEW_CONTENT = "previewContent"
        const val ARG_AUTHOR_NAME = "authorName"
        const val ARG_AUTHOR_AVATAR = "authorAvatar"
        const val ARG_POST_TIME = "postTime"
        const val ARG_APP_DETAIL_JSON = "appDetailJson"
    }
}

// --- 核心导航 ---

object Home : AppDestination {
    override val route = "home"
}

object Login : AppDestination {
    override val route = "login"
}

object Search : AppDestination {
    override val route = "search"
}

object About : AppDestination {
    override val route = "about"
}

object LogViewer : AppDestination {
    override val route = "log_viewer"
}

object ThemeCustomize : AppDestination {
    override val route = "theme_customize"
}

object StoreManager : AppDestination {
    override val route = "store_manager"
}

object UpdateSettings : AppDestination {
    override val route = "update_settings"
}

// 新增：应用更新屏幕目的地
object Update : AppDestination {
    override val route = "update"
}


// --- 社区与帖子 ---

object Community : AppDestination {
    override val route = "community"
}

object MyLikes : AppDestination {
    override val route = "my_likes"
}

object HotPosts : AppDestination {
    override val route = "hot_posts"
}

object FollowingPosts : AppDestination {
    override val route = "following_posts"
}

object BrowseHistory : AppDestination {
    override val route = "browse_history"
}

object Download : AppDestination {
    override val route = "download"
}

data class PostDetail(val postId: Long) : AppDestination {
    override val route = "post_detail/{${AppDestination.ARG_POST_ID}}"
    fun createRoute() = "post_detail/$postId"

    companion object {
        val arguments = listOf(
            navArgument(AppDestination.ARG_POST_ID) { type = NavType.LongType }
        )
    }
}

object CreatePost : AppDestination {
    override val route = "create_post"
}

data class CreateRefundPost(
    val appId: Long,
    val versionId: Long,
    val appName: String,
    val payMoney: Int
) : AppDestination {
    override val route = "create_refund_post/{${AppDestination.ARG_APP_ID}}/{${AppDestination.ARG_VERSION_ID}}/{${AppDestination.ARG_APP_NAME}}/{${AppDestination.ARG_PAY_MONEY}}"
    fun createRoute() = "create_refund_post/$appId/$versionId/${encode(appName)}/$payMoney"

    companion object {
        val arguments = listOf(
            navArgument(AppDestination.ARG_APP_ID) { type = NavType.LongType },
            navArgument(AppDestination.ARG_VERSION_ID) { type = NavType.LongType },
            navArgument(AppDestination.ARG_APP_NAME) { type = NavType.StringType },
            navArgument(AppDestination.ARG_PAY_MONEY) { type = NavType.IntType }
        )
    }
}

data class ImagePreview(val imageUrl: String) : AppDestination {
    override val route = "image_preview?url={${AppDestination.ARG_IMAGE_URL}}"
    fun createRoute() = "image_preview?url=${encode(imageUrl)}"

    companion object {
        val arguments = listOf(
            navArgument(AppDestination.ARG_IMAGE_URL) {
                type = NavType.StringType
                nullable = true
            }
        )
    }
}

// --- 用户相关 ---

data class UserDetail(val userId: Long, val store: AppStore = AppStore.XIAOQU_SPACE) : AppDestination {
    override val route = "user_detail/{${AppDestination.ARG_USER_ID}}/{store}"
    fun createRoute() = "user_detail/$userId/${store.name}"
    companion object {
        val arguments = listOf(
            navArgument(AppDestination.ARG_USER_ID) { type = NavType.LongType },
            navArgument("store") { type = NavType.StringType; defaultValue = AppStore.XIAOQU_SPACE.name }
        )
    }
}

data class MyPosts(val userId: Long) : AppDestination {
    override val route = "my_posts/{${AppDestination.ARG_USER_ID}}"
    fun createRoute() = "my_posts/$userId"

    companion object {
        val arguments = listOf(
            navArgument(AppDestination.ARG_USER_ID) { type = NavType.LongType }
        )
    }
}

object FollowList : AppDestination {
    override val route = "follow_list"
}

object FanList : AppDestination {
    override val route = "fan_list"
}

object AccountProfile : AppDestination {
    override val route = "account_profile"
}

// --- 资源广场与应用 ---

data class ResourcePlaza(val isMyResource: Boolean, val userId: Long = -1L, val mode: String = "public") : AppDestination {
    override val route = "plaza?${AppDestination.ARG_IS_MY_RESOURCE}={${AppDestination.ARG_IS_MY_RESOURCE}}&${AppDestination.ARG_USER_ID}={${AppDestination.ARG_USER_ID}}&mode={mode}"
    fun createRoute() = "plaza?${AppDestination.ARG_IS_MY_RESOURCE}=$isMyResource&${AppDestination.ARG_USER_ID}=$userId&mode=$mode"
    
    companion object {
        val arguments = listOf(
            navArgument(AppDestination.ARG_IS_MY_RESOURCE) { type = NavType.BoolType; defaultValue = false },
            navArgument(AppDestination.ARG_USER_ID) { type = NavType.LongType; defaultValue = -1L },
            navArgument("mode") { type = NavType.StringType; defaultValue = "public" }
        )
    }
}

data class AppDetail(val appId: String, val versionId: Long, val storeName: String) : AppDestination {
    override val route = "app_detail/{${AppDestination.ARG_APP_ID}}/{${AppDestination.ARG_VERSION_ID}}/{storeName}"
    fun createRoute() = "app_detail/${Uri.encode(appId)}/$versionId/$storeName"

    companion object {
        val arguments = listOf(
            navArgument(AppDestination.ARG_APP_ID) { type = NavType.StringType },
            navArgument(AppDestination.ARG_VERSION_ID) { type = NavType.LongType },
            navArgument("storeName") { type = NavType.StringType }
        )
    }
}

object CreateAppRelease : AppDestination {
    override val route = "app_release_create"
}

data class UpdateAppRelease(val appDetailJson: String) : AppDestination {
    override val route = "app_release_update?json={${AppDestination.ARG_APP_DETAIL_JSON}}"
    fun createRoute() = "app_release_update?json=${encode(appDetailJson)}"

    companion object {
        val arguments = listOf(
            navArgument(AppDestination.ARG_APP_DETAIL_JSON) {
                type = NavType.StringType
                nullable = true
            }
        )
    }
}

// --- 消息、账单、支付 ---

object MessageCenter : AppDestination {
    override val route = "message_center"
}

object Billing : AppDestination {
    override val route = "billing"
}

object PaymentCenterAdvanced: AppDestination {
    override val route = "payment_center_advanced"
}

data class PaymentForApp(
    val appId: Long,
    val appName: String,
    val versionId: Long,
    val price: Int,
    val iconUrl: String,
    val previewContent: String
) : AppDestination {
    override val route = "payment_app?appId={${AppDestination.ARG_APP_ID}}&versionId={${AppDestination.ARG_VERSION_ID}}&price={${AppDestination.ARG_PRICE}}&appName={${AppDestination.ARG_APP_NAME}}&iconUrl={${AppDestination.ARG_ICON_URL}}&previewContent={${AppDestination.ARG_PREVIEW_CONTENT}}"
    fun createRoute() = "payment_app?appId=$appId&versionId=$versionId&price=$price&appName=${encode(appName)}&iconUrl=${encode(iconUrl)}&previewContent=${encode(previewContent)}"

    companion object {
        val arguments = listOf(
            navArgument(AppDestination.ARG_APP_ID) { type = NavType.LongType },
            navArgument(AppDestination.ARG_VERSION_ID) { type = NavType.LongType },
            navArgument(AppDestination.ARG_PRICE) { type = NavType.IntType },
            navArgument(AppDestination.ARG_APP_NAME) { type = NavType.StringType },
            navArgument(AppDestination.ARG_ICON_URL) { type = NavType.StringType },
            navArgument(AppDestination.ARG_PREVIEW_CONTENT) { type = NavType.StringType }
        )
    }
}

data class PaymentForPost(
    val postId: Long,
    val postTitle: String,
    val previewContent: String,
    val authorName: String,
    val authorAvatar: String,
    val postTime: String
) : AppDestination {
     override val route = "payment_post?postId={${AppDestination.ARG_POST_ID}}&postTitle={${AppDestination.ARG_POST_TITLE}}&previewContent={${AppDestination.ARG_PREVIEW_CONTENT}}&authorName={${AppDestination.ARG_AUTHOR_NAME}}&authorAvatar={${AppDestination.ARG_AUTHOR_AVATAR}}&postTime={${AppDestination.ARG_POST_TIME}}"
    fun createRoute() = "payment_post?postId=$postId&postTitle=${encode(postTitle)}&previewContent=${encode(previewContent)}&authorName=${encode(authorName)}&authorAvatar=${encode(authorAvatar)}&postTime=${encode(postTime)}"

    companion object {
        val arguments = listOf(
            navArgument(AppDestination.ARG_POST_ID) { type = NavType.LongType },
            navArgument(AppDestination.ARG_POST_TITLE) { type = NavType.StringType },
            navArgument(AppDestination.ARG_PREVIEW_CONTENT) { type = NavType.StringType },
            navArgument(AppDestination.ARG_AUTHOR_NAME) { type = NavType.StringType },
            navArgument(AppDestination.ARG_AUTHOR_AVATAR) { type = NavType.StringType },
            navArgument(AppDestination.ARG_POST_TIME) { type = NavType.StringType }
        )
    }
}

// --- 其他 ---

object RankingList : AppDestination {
    override val route = "ranking_list"
}

data class Player(val bvid: String): AppDestination {
    override val route = "player/{${AppDestination.ARG_BVID}}"
    fun createRoute() = "player/$bvid"
    companion object {
        val arguments = listOf(
            navArgument(AppDestination.ARG_BVID) { type = NavType.StringType }
        )
    }
}

// --- 辅助函数 ---
private fun encode(input: String): String {
    return URLEncoder.encode(input, StandardCharsets.UTF_8.toString())
}