//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import cc.bbq.xq.network.NoCache // 导入注解
import cc.bbq.xq.network.SuperCacheInterceptor // 导入拦截器
import retrofit2.http.POST
import retrofit2.http.GET
import okhttp3.RequestBody
import retrofit2.http.Query
import okhttp3.ResponseBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part

object RetrofitClient {
    private const val BASE_URL = "http://apk.xiaoqu.online/"
    private const val UPLOAD_BASE_URL = "https://file.bz6.top/"
    // **NEW**: 挽悦云的基址
    private const val WANYUEYUN_UPLOAD_BASE_URL = "http://wanyueyun-x.xbjstd.cn:9812/"
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    // **NEW**: Helper object to handle JSON conversion
    object JsonConverter {
        private val appDetailAdapter = moshi.adapter(models.AppDetail::class.java)

        fun toJson(appDetail: models.AppDetail): String {
            return appDetailAdapter.toJson(appDetail)
        }

        fun fromJson(json: String): models.AppDetail? {
            return try {
                appDetailAdapter.fromJson(json)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    // 核心修改 #1: 创建一个带有超级缓存拦截器的 OkHttpClient
    private val cachedOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(SuperCacheInterceptor()) // 安装我们的魔法拦截器
            .build()
    }

    val instance: ApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cachedOkHttpClient) // 使用新的 client
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }

    val uploadInstance: ImageUploadService by lazy {
        val client = OkHttpClient.Builder().build()

        Retrofit.Builder()
            .baseUrl(UPLOAD_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ImageUploadService::class.java)
    }
    
        // **NEW**: Retrofit instance for Wanyueyun
    val wanyueyunUploadInstance: WanyueyunUploadService by lazy {
        val client = OkHttpClient.Builder().build()
        Retrofit.Builder()
            .baseUrl(WANYUEYUN_UPLOAD_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WanyueyunUploadService::class.java)
    }

    // 模型类定义
    object models {
        // 基础响应模型
        @JsonClass(generateAdapter = true)
data class BaseResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "msg") val msg: String,
    @Json(name = "data") val data: Any?, // 修改为 Any?
    @Json(name = "timestamp") val timestamp: Long
){
            // 辅助方法：从 data 字段获取下载链接
            fun getDownloadUrl(): String? {
                return if (data is Map<*, *>) {
                    data["download"] as? String
                } else {
                    null
                }
            }
        }

        // 帖子详情响应模型
        @JsonClass(generateAdapter = true)
        data class PostDetailResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: PostDetail,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class PostDetail(
            @Json(name = "id") val id: Long,
            @Json(name = "title") val title: String,
            @Json(name = "content") val content: String,
            @Json(name = "userid") val userid: Long,
            @Json(name = "create_time") val create_time: String,
            @Json(name = "update_time") val update_time: String,
            @Json(name = "username") val username: String,
            @Json(name = "nickname") val nickname: String,
            @Json(name = "usertx") val usertx: String,
            @Json(name = "hierarchy") val hierarchy: String,
            @Json(name = "section_name") val section_name: String,
            @Json(name = "sub_section_name") val sub_section_name: String,
            @Json(name = "view") val view: String,
            @Json(name = "thumbs") val thumbs: String,
            @Json(name = "comment") val comment: String,
            @Json(name = "img_url") val img_url: List<String>? = null,
            @Json(name = "video_url") val video_url: String? = null,
            @Json(name = "ip_address") val ip_address: String,
            @Json(name = "create_time_ago") val create_time_ago: String,
            @Json(name = "is_thumbs") val is_thumbs: Int,
            @Json(name = "is_collection") val is_collection: Int
        )

        // 评论列表响应模型
        @JsonClass(generateAdapter = true)
        data class CommentListResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: CommentListData,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class CommentListData(
            @Json(name = "list") val list: List<Comment>,
            @Json(name = "pagecount") val pagecount: Int,
            @Json(name = "current_number") val current_number: Int
        )

        @JsonClass(generateAdapter = true)
        data class Comment(
            @Json(name = "id") val id: Long,
            @Json(name = "content") val content: String,
            @Json(name = "userid") val userid: Long,
            @Json(name = "time") val time: String,
            @Json(name = "username") val username: String,
            @Json(name = "nickname") val nickname: String,
            @Json(name = "usertx") val usertx: String,
            @Json(name = "hierarchy") val hierarchy: String,
            @Json(name = "parentid") val parentid: Long? = null,
            @Json(name = "parentnickname") val parentnickname: String? = null,
            @Json(name = "parentcontent") val parentcontent: String? = null,
            @Json(name = "image_path") val image_path: List<String>? = null,
            @Json(name = "sub_comments_count") val sub_comments_count: Int
        )

        // 帖子列表响应模型
        @JsonClass(generateAdapter = true)
        data class PostListResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: PostListData,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class PostListData(
            @Json(name = "list") val list: List<Post>,
            @Json(name = "pagecount") val pagecount: Int,
            @Json(name = "current_number") val current_number: Int
        )

        @JsonClass(generateAdapter = true)
        data class Post(
            @Json(name = "postid") val postid: Long,
            @Json(name = "title") val title: String,
            @Json(name = "content") val content: String,
            @Json(name = "userid") val userid: Long,
            @Json(name = "create_time") val create_time: String,
            @Json(name = "update_time") val update_time: String,
            @Json(name = "username") val username: String,
            @Json(name = "nickname") val nickname: String,
            @Json(name = "usertx") val usertx: String,
            @Json(name = "hierarchy") val hierarchy: String,
            @Json(name = "section_name") val section_name: String,
            @Json(name = "sub_section_name") val sub_section_name: String,
            @Json(name = "view") val view: String,
            @Json(name = "thumbs") val thumbs: String,
            @Json(name = "comment") val comment: String,
            @Json(name = "img_url") val img_url: List<String>? = null
        )

        @JsonClass(generateAdapter = true)
        data class LoginResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: LoginData?, // 可空类型
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class LoginData(
            @Json(name = "usertoken") val usertoken: String,
            @Json(name = "id") val id: Long,
            @Json(name = "username") val username: String,
            @Json(name = "is_section_moderator") val isSectionModerator: Int = 0
        )

        @JsonClass(generateAdapter = true)
        data class HeartbeatResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class UserInfoResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: UserData,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class UserData(
            @Json(name = "id") val id: Long,
            @Json(name = "username") val username: String,
            @Json(name = "usertx") val usertx: String,
            @Json(name = "nickname") val nickname: String,
            @Json(name = "hierarchy") val hierarchy: String,
            @Json(name = "money") val money: Int,
            @Json(name = "followerscount") val followerscount: String,
            @Json(name = "fanscount") val fanscount: String,
            @Json(name = "postcount") val postcount: String,
            @Json(name = "likecount") val likecount: String,
            @Json(name = "create_time") val create_time: String = "",
            @Json(name = "signlasttime") val signlasttime: String = "",
            @Json(name = "series_days") val series_days: Int = 0
        )

        // 应用列表模型
        @JsonClass(generateAdapter = true)
        data class AppListResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: AppListData,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class AppListData(
            @Json(name = "list") val list: List<AppItem>,
            @Json(name = "pagecount") val pagecount: Int,
            @Json(name = "current_number") val current_number: Int
        )

        @JsonClass(generateAdapter = true)
        data class AppItem(
            @Json(name = "id") val id: Long,
            @Json(name = "appname") val appname: String,
            @Json(name = "app_icon") val app_icon: String,
            @Json(name = "app_size") val app_size: String,
            @Json(name = "download_count") val download_count: Int,
            @Json(name = "create_time") val create_time: String,
            @Json(name = "nickname") val nickname: String,
            @Json(name = "apps_version_id") val apps_version_id: Long
        )

        // 应用详情响应模型
        @JsonClass(generateAdapter = true)
        data class AppDetailResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: AppDetail,
            @Json(name = "timestamp") val timestamp: Long
        )

        // 消息通知响应
        @JsonClass(generateAdapter = true)
        data class MessageNotificationResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: MessageNotificationData,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class MessageNotificationData(
            @Json(name = "list") val list: List<MessageNotification>,
            @Json(name = "pagecount") val pagecount: Int,
            @Json(name = "current_number") val current_number: Int
        )

        @JsonClass(generateAdapter = true)
        data class MessageNotification(
            @Json(name = "id") val id: Long,
            @Json(name = "title") val title: String,
            @Json(name = "content") val content: String,
            @Json(name = "send_to") val send_to: Long,
            @Json(name = "appid") val appid: Int,
            @Json(name = "type") val type: Int,
            @Json(name = "time") val time: String,
            @Json(name = "postid") val postid: Long?,
            @Json(name = "pic_url") val pic_url: String?,
            @Json(name = "user_id") val user_id: Long,
            @Json(name = "status") val status: Int,
            @Json(name = "is_admin") val is_admin: Int
        )

        @JsonClass(generateAdapter = true)
        data class AppDetail(
            @Json(name = "id") val id: Long,
            @Json(name = "appname") val appname: String,
            @Json(name = "app_icon") val app_icon: String,
            @Json(name = "app_size") val app_size: String,
            @Json(name = "app_explain") val app_explain: String?,
            @Json(name = "app_introduce") val app_introduce: String?,
            @Json(name = "app_introduction_image") val app_introduction_image: String?,
            @Json(name = "is_pay") val is_pay: Int,
            @Json(name = "pay_money") val pay_money: Int,
            @Json(name = "download") val download: String?,
            @Json(name = "create_time") val create_time: String,
            @Json(name = "update_time") val update_time: String,
            @Json(name = "userid") val userid: Long,
            @Json(name = "appid") val appid: Int,
            @Json(name = "category_id") val category_id: Int,
            @Json(name = "sub_category_id") val sub_category_id: Int,
            @Json(name = "category_name") val category_name: String,
            @Json(name = "category_icon") val category_icon: String,
            @Json(name = "sub_category_name") val sub_category_name: String,
            @Json(name = "username") val username: String,
            @Json(name = "nickname") val nickname: String,
            @Json(name = "usertx") val usertx: String,
            @Json(name = "sex") val sex: Int,
            @Json(name = "signature") val signature: String,
            @Json(name = "exp") val exp: Int,
            @Json(name = "version") val version: String,
            @Json(name = "apps_version_id") val apps_version_id: Long,
            @Json(name = "version_create_time") val version_create_time: String,
            @Json(name = "app_introduction_image_array") val app_introduction_image_array: List<String>?,
            @Json(name = "ip_address") val ip_address: String,
            @Json(name = "sexName") val sexName: String,
            @Json(name = "badge") val badge: List<Any>,
            @Json(name = "vip") val vip: Boolean,
            @Json(name = "hierarchy") val hierarchy: String,
            @Json(name = "is_user_pay") val is_user_pay: Boolean,
            @Json(name = "download_count") val download_count: Int,
            @Json(name = "comment_count") val comment_count: Int,
            @Json(name = "user_pay_count") val user_pay_count: Int,
            @Json(name = "reward_count") val reward_count: Int,
            @Json(name = "posturl") val posturl: String?
        )

        @JsonClass(generateAdapter = true)
        data class BrowseHistoryResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: BrowseHistoryData,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class BrowseHistoryData(
            @Json(name = "list") val list: List<BrowseHistoryItem>,
            @Json(name = "pagecount") val pagecount: Int,
            @Json(name = "current_number") val current_number: Int
        )

        @JsonClass(generateAdapter = true)
        data class BrowseHistoryItem(
            @Json(name = "postid") val postid: Long,
            @Json(name = "title") val title: String,
            @Json(name = "content") val content: String,
            @Json(name = "userid") val userid: Long,
            @Json(name = "create_time") val create_time: String,
            @Json(name = "update_time") val update_time: String,
            @Json(name = "username") val username: String,
            @Json(name = "nickname") val nickname: String,
            @Json(name = "usertx") val usertx: String,
            @Json(name = "hierarchy") val hierarchy: String,
            @Json(name = "section_name") val section_name: String,
            @Json(name = "sub_section_name") val sub_section_name: String,
            @Json(name = "view") val view: String,
            @Json(name = "thumbs") val thumbs: String,
            @Json(name = "comment") val comment: String,
            @Json(name = "img_url") val img_url: List<String>? = null
        )

        // 应用评论列表响应模型
        @JsonClass(generateAdapter = true)
        data class AppCommentListResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: AppCommentListData,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class AppCommentListData(
            @Json(name = "list") val list: List<AppComment>,
            @Json(name = "pagecount") val pagecount: Int,
            @Json(name = "current_number") val current_number: Int
        )

        @JsonClass(generateAdapter = true)
        data class AppComment(
            @Json(name = "id") val id: Long,
            @Json(name = "content") val content: String,
            @Json(name = "userid") val userid: Long,
            @Json(name = "time") val time: String,
            @Json(name = "username") val username: String,
            @Json(name = "nickname") val nickname: String,
            @Json(name = "usertx") val usertx: String,
            @Json(name = "hierarchy") val hierarchy: String,
            @Json(name = "parentid") val parentid: Long? = null,
            @Json(name = "parentnickname") val parentnickname: String? = null,
            @Json(name = "parentcontent") val parentcontent: String? = null,
            @Json(name = "image_path") val image_path: List<String>? = null
        )

        // 用户列表响应模型
        @JsonClass(generateAdapter = true)
        data class UserListResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: UserListData,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class UserListData(
            @Json(name = "list") val list: List<UserItem>,
            @Json(name = "pagecount") val pagecount: Int,
            @Json(name = "current_number") val current_number: Int
        )

        @JsonClass(generateAdapter = true)
        data class UserItem(
            @Json(name = "id") val id: Long,
            @Json(name = "username") val username: String,
            @Json(name = "nickname") val nickname: String,
            @Json(name = "usertx") val usertx: String,
            @Json(name = "sex") val sex: Int,
            @Json(name = "signature") val signature: String,
            @Json(name = "title") val title: List<String>,
            @Json(name = "badge") val badge: List<Any>,
            @Json(name = "vip") val vip: Boolean,
            @Json(name = "hierarchy") val hierarchy: String,
            @Json(name = "status") val status: Int,
            @Json(name = "sexName") val sexName: String
        )

        @JsonClass(generateAdapter = true)
        data class UserInformationResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: UserInformationData,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class UserInformationData(
            @Json(name = "id") val id: Long,
            @Json(name = "follow_status") val follow_status: String,
            @Json(name = "username") val username: String,
            @Json(name = "usertx") val usertx: String,
            @Json(name = "nickname") val nickname: String,
            @Json(name = "money") val money: Int,
            @Json(name = "exp") val exp: Int,
            @Json(name = "followerscount") val followerscount: String,
            @Json(name = "fanscount") val fanscount: String,
            @Json(name = "postcount") val postcount: String,
            @Json(name = "likecount") val likecount: String,
            @Json(name = "hierarchy") val hierarchy: String,
            @Json(name = "last_activity_time") val last_activity_time: String?,
            @Json(name = "series_days") val series_days: Int?,
            @Json(name = "commentcount") val commentcount: String?
        )

        @JsonClass(generateAdapter = true)
        data class BillingResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "msg") val msg: String,
            @Json(name = "data") val data: BillingData,
            @Json(name = "timestamp") val timestamp: Long
        )

        @JsonClass(generateAdapter = true)
        data class BillingData(
            @Json(name = "list") val list: List<BillingItem>,
            @Json(name = "pagecount") val pagecount: Int,
            @Json(name = "current_number") val current_number: Int
        )

        @JsonClass(generateAdapter = true)
        data class BillingItem(
            @Json(name = "id") val id: Long,
            @Json(name = "transaction_type") val transaction_type: Int,
            @Json(name = "transaction_date") val transaction_date: String,
            @Json(name = "transaction_amount") val transaction_amount: String,
            @Json(name = "remark") val remark: String,
            @Json(name = "type") val type: Int
        )
        
            @JsonClass(generateAdapter = true)
       data class RankingUser(
           @Json(name = "id") val id: Long,
           @Json(name = "username") val username: String,
           @Json(name = "nickname") val nickname: String,
           @Json(name = "usertx") val usertx: String,
           @Json(name = "money") val money: Int,
           @Json(name = "title") val title: List<String>?,
           @Json(name = "badge") val badge: List<Any>?
       )       
       
       @JsonClass(generateAdapter = true)
data class RankingListResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "msg") val msg: String,
    @Json(name = "data") val data: List<RankingUser>,
    @Json(name = "timestamp") val timestamp: Long
)

    }

    interface ApiService {
        // 登录接口 为所有不应缓存的方法添加 @NoCache 注解
        @NoCache
        @POST("api/login")
        @FormUrlEncoded
        suspend fun login(
            @Field("appid") appid: Int = 1,
            @Field("username") username: String,
            @Field("password") password: String,
            @Field("device") device: String
        ): Response<models.LoginResponse>

        // 心跳接口
        @NoCache
        @POST("api/user_heartbeat")
        @FormUrlEncoded
        suspend fun heartbeat(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String
        ): Response<models.HeartbeatResponse>

        // 帖子列表接口
        @POST("api/get_posts_list")
        @FormUrlEncoded
        suspend fun getPostsList(
            @Field("appid") appid: Int = 1,
            @Field("limit") limit: Int,
            @Field("page") page: Int,
            @Field("sort") sort: String = "create_time",
            @Field("sortOrder") sortOrder: String = "desc",
            @Field("sectionid") sectionId: Int? = null,
            @Field("userid") userId: Long? = null
        ): Response<models.PostListResponse>

        // 用户信息接口
        @POST("api/get_user_other_information")
        @FormUrlEncoded
        suspend fun getUserInfo(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String
        ): Response<models.UserInfoResponse>

        // 帖子详情接口
        @POST("api/get_post_information")
        @FormUrlEncoded
        suspend fun getPostDetail(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("postid") postId: Long
        ): Response<models.PostDetailResponse>

        // 评论列表接口
        @POST("api/get_list_comments")
        @FormUrlEncoded
        suspend fun getPostComments(
            @Field("appid") appid: Int = 1,
            @Field("postid") postId: Long,
            @Field("limit") limit: Int,
            @Field("page") page: Int,
            @Field("sort") sort: String = "time",
            @Field("sortOrder") sortOrder: String = "desc"
        ): Response<models.CommentListResponse>

        // 创建帖子接口
        @POST("api/post")
        @FormUrlEncoded
        suspend fun createPost(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("title") title: String,
            @Field("content") content: String,
            @Field("subsectionid") sectionId: Int,
            @Field("network_picture") imageUrls: String? = null,
            @Field("paid_reading") paidReading: Int = 0,
            @Field("file_download_method") downloadMethod: Int = 0
        ): Response<models.BaseResponse>

        // 应用列表接口
        @POST("api/get_apps_list")
@FormUrlEncoded
suspend fun getAppsList(
    @Field("appid") appid: Int = 1,
    @Field("limit") limit: Int,
    @Field("page") page: Int,
    @Field("sort") sort: String = "update_time",
    @Field("sortOrder") sortOrder: String = "desc",
    @Field("category_id") categoryId: Int? = null,
    @Field("sub_category_id") subCategoryId: Int? = null, // 添加 subCategoryId
    @Field("appname") appName: String? = null,
    @Field("userid") userId: Long? = null
): Response<models.AppListResponse>

        // 应用详情接口
        @POST("api/get_apps_information")
        @FormUrlEncoded
        suspend fun getAppsInformation(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("apps_id") appsId: Long,
            @Field("apps_version_id") appsVersionId: Long
        ): Response<models.AppDetailResponse>

        // 应用评论接口
        @POST("api/get_apps_comment_list")
        @FormUrlEncoded
        suspend fun getAppsCommentList(
            @Field("appid") appid: Int = 1,
            @Field("apps_id") appsId: Long,
            @Field("apps_version_id") appsVersionId: Long,
            @Field("limit") limit: Int,
            @Field("page") page: Int,
            @Field("sortOrder") sortOrder: String = "asc"
        ): Response<models.AppCommentListResponse>
        
        @NoCache
        @POST("api/post_comment")
        @FormUrlEncoded
        suspend fun postComment(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("content") content: String,
            @Field("postid") postId: Long? = null,
            @Field("parentid") parentId: Long? = null,
            @Field("img") imageUrl: String? = null
        ): Response<models.BaseResponse>

        // 消息通知接口
        @POST("api/get_message_notifications")
        @FormUrlEncoded
        suspend fun getMessageNotifications(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("limit") limit: Int,
            @Field("page") page: Int
        ): Response<models.MessageNotificationResponse>

        // 浏览历史接口
        @POST("api/browse_history")
        @FormUrlEncoded
        suspend fun getBrowseHistory(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("limit") limit: Int = 10,
            @Field("page") page: Int
        ): Response<models.BrowseHistoryResponse>

        @POST("api/get_likes_records")
        @FormUrlEncoded
        suspend fun getLikesRecords(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("limit") limit: Int = 10,
            @Field("page") page: Int
        ): Response<models.PostListResponse>

        @POST("api/get_posts_list")
        @FormUrlEncoded
        suspend fun searchPosts(
            @Field("appid") appid: Int = 1,
            @Field("keyword") query: String,
            @Field("limit") limit: Int = 10,
            @Field("page") page: Int
        ): Response<models.PostListResponse>
        
                // 热点帖子列表接口 (按 score 排序)
        @POST("api/get_posts_list")
        @FormUrlEncoded
        suspend fun getHotPostsList(
            @Field("appid") appid: Int = 1,
            @Field("limit") limit: Int,
            @Field("page") page: Int,
            @Field("sortOrder") sortOrder: String = "desc",
            @Field("sort") sort: String = "score"
        ): Response<models.PostListResponse>
        
                // 获取关注的人的帖子列表接口
        @POST("api/get_my_following_posts")
        @FormUrlEncoded
        suspend fun getMyFollowingPosts(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("limit") limit: Int,
            @Field("page") page: Int
        ): Response<models.PostListResponse>

        // 点赞接口
        @NoCache
        @POST("api/like_posts")
        @FormUrlEncoded
        suspend fun likePost(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("postid") postId: Long
        ): Response<models.BaseResponse>

        @NoCache
        @POST("api/delete_post")
        @FormUrlEncoded
        suspend fun deletePost(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("postid") postId: Long
        ): Response<models.BaseResponse>

        // 获取粉丝列表
        @POST("api/get_fan_list")
        @FormUrlEncoded
        suspend fun getFanList(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("limit") limit: Int = 10,
            @Field("page") page: Int
        ): Response<models.UserListResponse>

        // 获取关注列表
        @POST("api/get_follow_list")
        @FormUrlEncoded
        suspend fun getFollowList(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("limit") limit: Int = 10,
            @Field("page") page: Int
        ): Response<models.UserListResponse>

        @POST("api/get_user_information")
        @FormUrlEncoded
        suspend fun getUserInformation(
            @Field("appid") appid: Int = 1,
            @Field("userid") userId: Long,
            @Field("usertoken") token: String
        ): Response<models.UserInformationResponse>
        
        @NoCache
        @POST("api/delete_comment")
        @FormUrlEncoded
        suspend fun deleteComment(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("commentid") commentId: Long
        ): Response<models.BaseResponse>

        @POST("api/get_user_billing")
        @FormUrlEncoded
        suspend fun getUserBilling(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("limit") limit: Int,
            @Field("page") page: Int
        ): Response<models.BillingResponse>
        
        @NoCache
        @POST("api/pay_for_apps")
        @FormUrlEncoded
        suspend fun payForApp(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("apps_id") appsId: Long,
            @Field("apps_version_id") appsVersionId: Long,
            @Field("money") money: Int,
            @Field("type") type: Int = 0
        ): Response<models.BaseResponse>

             // 帖子打赏接口
        @NoCache
        @POST("api/reward_posts")
        @FormUrlEncoded
        suspend fun rewardPost(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("postid") postId: Long,
            @Field("money") money: Int,
            @Field("payment") payment: Int = 0
        ): Response<models.BaseResponse>

        // 应用评论提交
        @NoCache
        @POST("api/apps_add_comment")
        @FormUrlEncoded
        suspend fun postAppComment(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("content") content: String,
            @Field("apps_id") appsId: Long,
            @Field("apps_version_id") appsVersionId: Long,
            @Field("parentid") parentId: Long? = null,
            @Field("img") imageUrl: String? = null
        ): Response<models.BaseResponse>

        // 应用评论删除
        @NoCache
        @POST("api/delete_apps_comment")
        @FormUrlEncoded
        suspend fun deleteAppComment(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("comment_id") commentId: Long
        ): Response<models.BaseResponse>

        // 用户签到接口
        @NoCache
        @POST("api/user_sign_in")
        @FormUrlEncoded
        suspend fun userSignIn(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String
        ): Response<models.BaseResponse>
            // 注册接口
        @NoCache
        @POST("api/register")
        @FormUrlEncoded
        suspend fun register(
            @Field("appid") appid: Int = 1,
            @Field("username") username: String,
            @Field("password") password: String,
            @Field("email") email: String,
            @Field("device") device: String,
            @Field("captcha") captcha: String
        ): Response<models.BaseResponse>
        
         // 修改用户信息接口 (用户名、QQ)
        @NoCache
        @POST("api/modify_user_information")
        @FormUrlEncoded
        suspend fun modifyUserInfo(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("nickname") nickname: String? = null,
            @Field("qq") qq: String? = null
        ): Response<models.BaseResponse>
        
                // 发布应用接口
        @NoCache
        @POST("api/release_apps")
        @FormUrlEncoded
        suspend fun releaseApp(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") usertoken: String,
            @Field("appname") appname: String,
            @Field("icon") icon: String?,
            @Field("app_size") app_size: String,
            @Field("app_introduce") app_introduce: String,
            @Field("app_introduction_image") app_introduction_image: String?,
            @Field("file") file: String,
            @Field("app_explain") app_explain: String?,
            @Field("app_version") app_version: String,
            @Field("is_pay") is_pay: Int,
            @Field("pay_money") pay_money: String?,
            @Field("category_id") category_id: Int,
            @Field("sub_category_id") sub_category_id: Int
        ): Response<models.BaseResponse>
        
                // 更新应用版本接口
        @NoCache
        @POST("api/release_new_version")
        @FormUrlEncoded
        suspend fun updateApp(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") usertoken: String,
            @Field("apps_id") apps_id: Long, // 新增字段
            @Field("appname") appname: String,
            @Field("icon") icon: String?,
            @Field("app_size") app_size: String,
            @Field("app_introduce") app_introduce: String,
            @Field("app_introduction_image") app_introduction_image: String,
            @Field("file") file: String,
            @Field("app_explain") app_explain: String?,
            @Field("app_version") app_version: String,
            @Field("is_pay") is_pay: Int,
            @Field("pay_money") pay_money: String?,
            @Field("category_id") category_id: Int,
            @Field("sub_category_id") sub_category_id: Int
        ): Response<models.BaseResponse>

        // 删除应用接口
        @NoCache
        @POST("api/delete_apps")
        @FormUrlEncoded
        suspend fun deleteApp(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") usertoken: String,
            @Field("apps_id") apps_id: Long,
            @Field("app_version_id") app_version_id: Long
        ): Response<models.BaseResponse>
        
                 // 天梯竞赛列表接口
@POST("api/ranking_list")
@FormUrlEncoded
suspend fun getRankingList(
    @Field("appid") appid: Int = 1,
    @Field("sort") sort: String = "money",
    @Field("sortOrder") sortOrder: String = "desc",
    @Field("limit") limit: Int = 15,
    @Field("page") page: Int
): Response<models.RankingListResponse>        

        // 关注/取消关注接口
        @NoCache
        @POST("api/follow_users")
        @FormUrlEncoded
        suspend fun followUser(
            @Field("appid") appid: Int = 1,
            @Field("usertoken") token: String,
            @Field("followedid") followedId: Long
        ): Response<models.BaseResponse>

@NoCache 
@Multipart
@POST("api/upload_avatar")
suspend fun uploadAvatar(
    @Part("appid") appid: RequestBody,
    @Part("usertoken") token: RequestBody,
    @Part file: MultipartBody.Part // 文件数据 - 移除名称
): retrofit2.Response<RetrofitClient.models.BaseResponse>
// 获取图片验证码
        @NoCache
        @GET("api/get_image_verification_code")
        suspend fun getImageVerificationCode(
            @Query("appid") appid: Int = 1,
            @Query("type") type: Int = 2
        ): Response<ResponseBody>
        
    }    

    //Image upload service interface
    interface ImageUploadService {
        @Multipart
        @POST("api.php")
        suspend fun uploadImage(
            @Part file: MultipartBody.Part
        ): Response<UploadResponse>
    }
    
        // **NEW**: Interface for Wanyueyun
    interface WanyueyunUploadService {
        @Multipart
        @POST("upload")
        suspend fun uploadFile(
            @Part("Api") apiName: RequestBody,
            @Part file: MultipartBody.Part
        ): Response<WanyueyunUploadResponse>
    }


// **FIXED**: Updated UploadResponse to include 'exists' and 'viewurl'
    @JsonClass(generateAdapter = true)
    data class UploadResponse(
        @Json(name = "code") val code: Int,
        @Json(name = "msg") val msg: String,
        @Json(name = "exists") val exists: Int? = null, // Added to handle file-exists case
        @Json(name = "downurl") val downurl: String?,
        @Json(name = "viewurl") val viewurl: String?
    )
    
        // **NEW**: Response model for Wanyueyun
    @JsonClass(generateAdapter = true)
    data class WanyueyunUploadResponse(
        @Json(name = "code") val code: Int,
        @Json(name = "msg") val msg: String,
        @Json(name = "data") val data: String?
    )
}
