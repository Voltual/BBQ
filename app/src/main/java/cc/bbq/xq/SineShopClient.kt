//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package cc.bbq.xq

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

object SineShopClient {
    private const val BASE_URL = "https://api.market.sineworld.cn"
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY = 1000L
    private const val REQUEST_TIMEOUT = 30000L
    private const val CONNECT_TIMEOUT = 30000L
    private const val SOCKET_TIMEOUT = 30000L

    // 用户代理信息 - 需要根据实际设备信息调整
    private const val USER_AGENT = "Token:"

    // Ktor HttpClient 实例
    val httpClient = HttpClient(OkHttp) {
        initConfig(this)
        defaultRequest {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            //  header(HttpHeaders.UserAgent, USER_AGENT) // 暂时不在这里设置，在每个请求中动态获取
        }
    }


    private fun initConfig(client: HttpClientConfig<OkHttpConfig>) {
        // 默认请求配置
        client.defaultRequest {
            url(BASE_URL)
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            //  header(HttpHeaders.UserAgent, USER_AGENT) // 暂时不在这里设置，在每个请求中动态获取
        }

        // JSON 序列化配置
        client.install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            })
        }

        // 日志配置
        client.install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }

        // 超时配置
        client.install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT
            connectTimeoutMillis = CONNECT_TIMEOUT
            socketTimeoutMillis = SOCKET_TIMEOUT
        }
    }

    // 基础响应模型 - 弦应用商店的统一响应格式
    @kotlinx.serialization.Serializable
    data class BaseResponse<T>(
        val code: Int,
        val msg: String,
        val data: T? = null
    ) {
        val isSuccess: Boolean get() = code == 0
    }

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
        @SerialName("ban_time") val banTime: Long,
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
        @SerialName("version_count") val version_count: Int?  // 改为可空类型
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

    // 新增：弦应用商店专用用户信息模型
    @Serializable
    data class SineShopUserInfoLite(
        val id: Int,
        val username: String,
        @SerialName("display_name") val displayName: String,
        @SerialName("user_avatar") val userAvatar: String?
    )

@Serializable
data class SineShopAppDetail(
    val id: Int,
    @SerialName("package_name") val package_name: String,
    @SerialName("app_name") val app_name: String,
    @SerialName("version_code") val version_code: Int,
    @SerialName("version_name") val version_name: String,
    @SerialName("app_icon") val app_icon: String,
    @SerialName("app_type") val app_type: String,
    @SerialName("app_version_type") val app_version_type: String,
    @SerialName("app_abi") val app_abi: Int,
    @SerialName("app_sdk_min") val app_sdk_min: Int,
    @SerialName("app_sdk_target") val app_sdk_target: Int?, // 新增：目标SDK版本（可为空）
    @SerialName("app_previews") val app_previews: List<String>?,
    @SerialName("app_describe") val app_describe: String?,
    @SerialName("app_update_log") val app_update_log: String?,
    @SerialName("app_developer") val app_developer: String?,
    @SerialName("app_source") val app_source: String?,
    @SerialName("upload_message") val upload_message: String?,
    @SerialName("download_size") val download_size: String?,
    @SerialName("upload_time") val upload_time: Long,
    @SerialName("update_time") val update_time: Long,
    @SerialName("user") val user: SineShopUserInfoLite,
    @SerialName("tags") val tags: List<AppTag>?,
    @SerialName("download_count") val download_count: Int,
    @SerialName("is_favourite") val is_favourite: Int,
    @SerialName("favourite_count") val favourite_count: Int,
    @SerialName("review_count") val review_count: Int,
    // 新增：审核状态和审核原因字段
    @SerialName("audit_status") val audit_status: Int?,
    @SerialName("audit_reason") val audit_reason: String?,
    // 新增：审核员信息
    @SerialName("audit_user") val audit_user: SineShopUserInfoLite?
)

    // 新增：弦应用商店专用评论模型
    @Serializable
    data class SineShopComment(
        val id: Int,
        val content: String,
        @SerialName("send_time") val send_time: Long,
        @SerialName("father_reply_id") val father_reply_id: Int,
        @SerialName("sender") val sender: SineShopUserInfoLite, // 使用 SineShopUserInfoLite
        @SerialName("child_count") val child_count: Int,
        @SerialName("father_reply") val father_reply: SineShopComment? // 可能为 null
    )

    // 为评论列表定义单独的数据模型
    @Serializable
    data class SineShopCommentListData(
        val total: Int,
        val list: List<SineShopComment>
    )
    
    // 新增：下载源模型
    @Serializable
    data class SineShopDownloadSource(
        val id: Int,
        @SerialName("app_id") val appId: Int,
        val name: String,
        val url: String,
        @SerialName("is_extra") val isExtra: Int
    )

    @Serializable
    data class DownloadSourceResponse(
        val actions: List<String>, // 通常为空
        val code: Int,
        val data: List<SineShopDownloadSource>?,
        val msg: String
    )

    /**
     * 安全地执行 Ktor 请求，并处理异常和重试
     */
      @Suppress("RedundantSuspendModifier")
    internal suspend inline fun <reified T> safeApiCall(block: suspend () -> HttpResponse): Result<T> {
        var attempts = 0
        while (attempts < MAX_RETRIES) {
            try {
                val response = block()
                if (!response.status.isSuccess()) {
                    println("SineShop Request failed with status: ${response.status}")
                    throw IOException("Request failed with status: ${response.status}")
                }
                val responseBody: T = try {
                    response.body()
                } catch (e: Exception) {
                    println("SineShop Failed to deserialize response body: ${e.message}")
                    throw e
                }
                return Result.success(responseBody)
            } catch (e: IOException) {
                attempts++
                println("SineShop Request failed, retrying in $RETRY_DELAY ms... (Attempt $attempts/$MAX_RETRIES)")
                if (attempts < MAX_RETRIES) {
                    delay(RETRY_DELAY)
                }
            } catch (e: Exception) {
                println("SineShop Request failed: ${e.message}")
                return Result.failure(e)
            }
        }
        println("SineShop Request failed after $MAX_RETRIES attempts.")
        return Result.failure(IOException("Request failed after $MAX_RETRIES attempts."))
    }

    /**
     * 发起 GET 请求
     */
    internal suspend inline fun <reified T> get(
        url: String,
        parameters: Parameters = Parameters.Empty
    ): Result<T> {
        return safeApiCall {
            httpClient.get(url) {
                parameters.entries().forEach { (key, values) ->
                    values.forEach { value ->
                        parameter(key, value)
                    }
                }
                val token = getToken()
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }
    }

    /**
     * 发起 POST 请求（表单格式）
     */
    internal suspend inline fun <reified T> postForm(
        url: String,
        parameters: Parameters = Parameters.Empty
    ): Result<T> {
        return safeApiCall {
            httpClient.post(url) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(parameters))
                val token = getToken()
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }
    }

    /**
     * 发起 POST 请求（JSON 格式）
     */
    internal suspend inline fun <reified T> postJson(
        url: String,
        body: Any? = null
    ): Result<T> {
        return safeApiCall {
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
                val token = getToken()
                 header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }
    }

    /**
     * 关闭 HttpClient（在应用退出时调用）
     */
    fun close() {
        httpClient.close()
    }

    // 扩展函数，便于参数构建
    internal fun sineShopParameters(block: ParametersBuilder.() -> Unit): Parameters {
        return Parameters.build(block)
    }

    // 新增：弦应用商店登录方法
    suspend fun login(username: String, password: String): Result<String> {
        val url = "/user/login"
        val parameters = sineShopParameters {
            append("username", username)
            append("password", password)
        }
        return safeApiCall<BaseResponse<String>> { // 显式指定类型参数
            httpClient.post(url) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(parameters))
                header(HttpHeaders.UserAgent, USER_AGENT) // 登录时不需要token
            }
        }.map { response: BaseResponse<String> ->
            if (response.code == 0) {
                response.data ?: "" // 返回 token，如果 data 为 null 则返回空字符串
            } else {
                throw IOException(response.msg)
            }
        }
    }

    // 新增：获取用户信息方法
    suspend fun getUserInfo(): Result<SineShopUserInfo> {
        val url = "/user/info"
        return safeApiCall<BaseResponse<SineShopUserInfo>> {
            httpClient.get(url) {
                val token = getToken()
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }.map { response: BaseResponse<SineShopUserInfo> ->
            if (response.code == 0) {
                response.data ?: throw IOException("Failed to get user info: Data is null")
            } else {
                throw IOException("Failed to get user info: ${response.msg}")
            }
        }
    }

    // 新增：获取应用分类标签列表方法
    suspend fun getAppTagList(): Result<List<AppTag>> {
        val url = "/tag/list"
        return safeApiCall<BaseResponse<AppTagListData>> {
            httpClient.get(url) {
                val token = getToken()
                // 即使 token 为空，也发送 User-Agent 头
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }.map { response: BaseResponse<AppTagListData> ->
            if (response.code == 0) {
                response.data?.list ?: emptyList()
            } else {
                throw IOException("Failed to get app tag list: ${response.msg}")
            }
        }
    }
    
    // 新增：根据用户ID获取弦应用商店用户信息方法
suspend fun getUserInfoById(userId: Long): Result<SineShopUserInfo> {
    val url = "/user/info"
    val parameters = sineShopParameters {
        append("id", userId.toString())
    }
    return safeApiCall<BaseResponse<SineShopUserInfo>> {
        httpClient.get(url) {
            parameters.entries().forEach { (key, values) ->
                values.forEach { value ->
                    parameter(key, value)
                }
            }
            val token = getToken()
            header(HttpHeaders.UserAgent, USER_AGENT + token)
        }
    }.map { response: BaseResponse<SineShopUserInfo> ->
        if (response.code == 0) {
            response.data ?: throw IOException("Failed to get user info: Data is null")
        } else {
            throw IOException("Failed to get user info: ${response.msg}")
        }
    }
}

suspend fun getAppsList(tag: Int? = null, page: Int = 1, keyword: String? = null, userId: Int? = null): Result<AppListData> {
    val url = "/app/list"
    val parameters = sineShopParameters {
        tag?.let { append("tag", it.toString()) }
        keyword?.let { append("keyword", it) }
        userId?.let { append("userid", it.toString()) }
        append("page", page.toString())
    }
    return safeApiCall<BaseResponse<AppListData>> {
        httpClient.get(url) {
            parameters.entries().forEach { (key, values) ->
                values.forEach { value ->
                    parameter(key, value)
                }
                val token = getToken()
                // 即使 token 为空，也发送 User-Agent 头
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }
    }.map { response: BaseResponse<AppListData> ->
        if (response.code == 0) {
            response.data ?: AppListData(0, emptyList()) // 如果 data 为 null，则返回一个空的 AppListData
        } else {
            throw IOException("Failed to get app list: ${response.msg}")
        }
    }
}


    // 新增：获取最新上传的应用列表方法
    suspend fun getLatestAppsList(page: Int = 1): Result<AppListData> {
        val url = "/app/list"
        val parameters = sineShopParameters {
            append("time", "") // 添加 time 参数以获取最新上传的应用
            append("page", page.toString())
        }
        return safeApiCall<BaseResponse<AppListData>> {
            httpClient.get(url) {
                parameters.entries().forEach { (key, values) ->
                    values.forEach { value ->
                        parameter(key, value)
                    }
                    val token = getToken()
                    // 即使 token 为空，也发送 User-Agent 头
                    header(HttpHeaders.UserAgent, USER_AGENT + token)
                }
            }
        }.map { response: BaseResponse<AppListData> ->
            if (response.code == 0) {
                response.data ?: AppListData(0, emptyList()) // 如果 data 为 null，则返回一个空的 AppListData
            } else {
                throw IOException("Failed to get latest app list: ${response.msg}")
            }
        }
    }

    // 新增：获取最多下载的应用列表方法
    suspend fun getMostDownloadedAppsList(page: Int = 1): Result<AppListData> {
        val url = "/leaderboard/app_download"
        val parameters = sineShopParameters {
            append("page", page.toString())
        }
        return safeApiCall<BaseResponse<AppListData>> {
            httpClient.get(url) {
                parameters.entries().forEach { (key, values) ->
                    values.forEach { value ->
                        parameter(key, value)
                    }
                    val token = getToken()
                    // 即使 token 为空，也发送 User-Agent 头
                    header(HttpHeaders.UserAgent, USER_AGENT + token)
                }
            }
        }.map { response: BaseResponse<AppListData> ->
            if (response.code == 0) {
                response.data ?: AppListData(0, emptyList()) // 如果 data 为 null，则返回一个空的 AppListData
            } else {
                throw IOException("Failed to get most downloaded app list: ${response.msg}")
            }
        }
    }
    
    // 新增：获取弦应用商店应用详情方法
suspend fun getSineShopAppInfo(appId: Int): Result<SineShopAppDetail> {
    val url = "/app/info"
    val parameters = sineShopParameters {
        append("appid", appId.toString())
    }
    return safeApiCall<BaseResponse<SineShopAppDetail>> {
        httpClient.get(url) {
            parameters.entries().forEach { (key, values) ->
                values.forEach { value ->
                    parameter(key, value)
                }
                val token = getToken()
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }
    }.map { response: BaseResponse<SineShopAppDetail> ->
        if (response.code == 0) {
            response.data ?: throw IOException("Failed to get app info: Data is null")
        } else {
            throw IOException("Failed to get app info: ${response.msg}")
        }
    }
}

// 新增：获取弦应用商店应用评论列表方法
suspend fun getSineShopAppComments(appId: Int, page: Int = 1): Result<SineShopCommentListData> {
    val url = "/reply/list"
    val parameters = sineShopParameters {
        append("appid", appId.toString())
        append("page", page.toString())
    }
    return safeApiCall<BaseResponse<SineShopCommentListData>> {
        httpClient.get(url) {
            parameters.entries().forEach { (key, values) ->
                values.forEach { value ->
                    parameter(key, value)
                }
                val token = getToken()
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }
    }.map { response: BaseResponse<SineShopCommentListData> ->
        if (response.code == 0) {
            response.data ?: SineShopCommentListData(0, emptyList()) // 如果 data 为 null，则返回一个空的 SineShopCommentListData
        } else {
            throw IOException("Failed to get app comments: ${response.msg}")
        }
    }
}

// 新增：向弦应用商店应用发送根评论方法
suspend fun postSineShopAppRootComment(appId: Int, content: String): Result<Int> { // 返回评论ID
    val url = "/reply/send"
    val parameters = sineShopParameters {
        append("appid", appId.toString())
        append("content", content)
        append("father", "-1") // -1 表示根评论
    }
    return safeApiCall<BaseResponse<Int>> {
        httpClient.post(url) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(parameters))
            val token = getToken()
            header(HttpHeaders.UserAgent, USER_AGENT + token)
        }
    }.map { response: BaseResponse<Int> ->
        if (response.code == 0) {
            response.data ?: throw IOException("Failed to post root comment: Data is null")
        } else {
            throw IOException("Failed to post root comment: ${response.msg}")
        }
    }
}

// 新增：向弦应用商店指定评论发送回复方法 (不@用户)
suspend fun postSineShopAppReplyComment(commentId: Int, content: String): Result<Int> { // 返回评论ID
    val url = "/reply/send"
    val parameters = sineShopParameters {
        append("appid", "-1") // 回复评论时 appid 固定为 -1
        append("content", content)
        append("father", commentId.toString()) // father 是要回复的评论ID
    }
    return safeApiCall<BaseResponse<Int>> {
        httpClient.post(url) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(parameters))
            val token = getToken()
            header(HttpHeaders.UserAgent, USER_AGENT + token)
        }
    }.map { response: BaseResponse<Int> ->
        if (response.code == 0) {
            response.data ?: throw IOException("Failed to post reply comment: Data is null")
        } else {
            throw IOException("Failed to post reply comment: ${response.msg}")
        }
    }
}

// 新增：向弦应用商店指定评论发送回复方法 (并@用户)
suspend fun postSineShopAppReplyCommentWithMention(commentId: Int, content: String, mentionUserId: Int): Result<Int> { // 返回评论ID
    val url = "/reply/send"
    val parameters = sineShopParameters {
        append("appid", "-1") // 回复评论时 appid 固定为 -1
        append("content", content)
        append("father", commentId.toString()) // father 是要回复的评论ID
        append("mention", mentionUserId.toString()) // mention 是要@的用户ID
    }
    return safeApiCall<BaseResponse<Int>> {
        httpClient.post(url) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(parameters))
            val token = getToken()
            header(HttpHeaders.UserAgent, USER_AGENT + token)
        }
    }.map { response: BaseResponse<Int> ->
        if (response.code == 0) {
            response.data ?: throw IOException("Failed to post reply comment with mention: Data is null")
        } else {
            throw IOException("Failed to post reply comment with mention: ${response.msg}")
        }
    }
}

// 新增：删除弦应用商店评论方法
suspend fun deleteSineShopComment(commentId: Int): Result<Unit> {
    val url = "/reply/delete"
    val parameters = sineShopParameters {
        append("id", commentId.toString())
    }
    return safeApiCall<BaseResponse<Unit?>> {
        httpClient.get(url) {
            parameters.entries().forEach { (key, values) ->
                values.forEach { value ->
                    parameter(key, value)
                }
                val token = getToken()
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }
    }.map { response: BaseResponse<Unit?> ->
        if (response.code == 0) {
            // 删除成功，返回 Unit
        } else {
            throw IOException("Failed to delete comment: ${response.msg}")
        }
    }
}

// 新增：获取应用下载源列表方法
    suspend fun getAppDownloadSources(appId: Int): Result<List<SineShopDownloadSource>> {
        val url = "/download/app"
        val parameters = sineShopParameters {
            append("appid", appId.toString())
        }
        return safeApiCall<DownloadSourceResponse> {
            httpClient.get(url) {
                parameters.entries().forEach { (key, values) ->
                    values.forEach { value ->
                        parameter(key, value)
                    }
                }
                val token = getToken()
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }.map { response ->
            if (response.code == 0) {
                response.data ?: emptyList()
            } else {
                throw IOException("Failed to get download sources: ${response.msg}")
            }
        }
    }
    
    // 新增：获取弦应用商店我的收藏应用列表方法
suspend fun getMyFavouriteAppsList(page: Int = 1): Result<AppListData> {
    val url = "/user/favourite"
    val parameters = sineShopParameters {
        append("page", page.toString())
    }
    return safeApiCall<BaseResponse<AppListData>> {
        httpClient.get(url) {
            parameters.entries().forEach { (key, values) ->
                values.forEach { value ->
                    parameter(key, value)
                }
                val token = getToken()
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }
    }.map { response: BaseResponse<AppListData> ->
        if (response.code == 0) {
            response.data ?: AppListData(0, emptyList()) // 如果 data 为 null，则返回一个空的 AppListData
        } else {
            throw IOException("Failed to get my favourite app list: ${response.msg}")
        }
    }
}

// 新增：获取弦应用商店我的上传应用列表方法
suspend fun getMyUploadAppsList(page: Int = 1): Result<AppListData> {
    val url = "/user/upload"
    val parameters = sineShopParameters {
        append("page", page.toString())
    }
    return safeApiCall<BaseResponse<AppListData>> {
        httpClient.get(url) {
            parameters.entries().forEach { (key, values) ->
                values.forEach { value ->
                    parameter(key, value)
                }
                val token = getToken()
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }
    }.map { response: BaseResponse<AppListData> ->
        if (response.code == 0) {
            response.data ?: AppListData(0, emptyList()) // 如果 data 为 null，则返回一个空的 AppListData
        } else {
            throw IOException("Failed to get my upload app list: ${response.msg}")
        }
    }
}

// 新增：获取弦应用商店我的历史足迹应用列表方法
suspend fun getMyHistoryAppsList(page: Int = 1): Result<AppListData> {
    val url = "/user/history"
    val parameters = sineShopParameters {
        append("page", page.toString())
    }
    return safeApiCall<BaseResponse<AppListData>> {
        httpClient.get(url) {
            parameters.entries().forEach { (key, values) ->
                values.forEach { value ->
                    parameter(key, value)
                }
                val token = getToken()
                header(HttpHeaders.UserAgent, USER_AGENT + token)
            }
        }
    }.map { response: BaseResponse<AppListData> ->
        if (response.code == 0) {
            response.data ?: AppListData(0, emptyList()) // 如果 data 为 null，则返回一个空的 AppListData
        } else {
            throw IOException("Failed to get my history app list: ${response.msg}")
        }
    }
}
    
    private fun getToken(): String {
        return runBlocking {
            AuthManager.getSineMarketToken(BBQApplication.instance).first()
        }
    }
}