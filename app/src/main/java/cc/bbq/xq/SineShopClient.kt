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

    // 新增：获取指定分类应用列表方法
    // SineShopClient.kt

// 新增：获取指定分类应用列表方法
suspend fun getAppsList(tag: Int, page: Int = 1): Result<AppListData> { // 修改返回类型为 AppListData
    val url = "/app/list"
    val parameters = sineShopParameters {
        append("tag", tag.toString())
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
    
    private fun getToken(): String {
        return runBlocking {
            AuthManager.getSineMarketToken(BBQApplication.instance).first()
        }
    }
}