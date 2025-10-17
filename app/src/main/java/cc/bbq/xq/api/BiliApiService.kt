package cc.bbq.xq.api

// **REMOVED**: 移除了 AuthManager 的导入（因为我懒得更新AuthManage）
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

object BiliApiManager {
    private const val BILI_API_BASE_URL = "https://api.bilibili.com/"
    private const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val instance: BiliApiService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", USER_AGENT_WEB)
                    .addHeader("Referer", "https://www.bilibili.com/")
                    // **REMOVED**: 移除了对 Cookie 的依赖
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BILI_API_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BiliApiService::class.java)
    }

    object models {
        // 视频信息响应 (对应 /x/web-interface/view)
        @JsonClass(generateAdapter = true)
        data class BiliVideoInfoResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "message") val message: String,
            @Json(name = "data") val data: VideoData?
        )

        @JsonClass(generateAdapter = true)
        data class VideoData(
            @Json(name = "bvid") val bvid: String,
            @Json(name = "aid") val aid: Long,
            @Json(name = "title") val title: String,
            @Json(name = "pic") val cover: String,
            @Json(name = "duration") val duration: Int,
            @Json(name = "owner") val owner: Owner,
            @Json(name = "pages") val pages: List<VideoPage>
        )

        @JsonClass(generateAdapter = true)
        data class Owner(
            @Json(name = "mid") val mid: Long,
            @Json(name = "name") val name: String,
            @Json(name = "face") val avatar: String
        )

        @JsonClass(generateAdapter = true)
        data class VideoPage(
            @Json(name = "cid") val cid: Long,
            @Json(name = "part") val part: String,
            @Json(name = "duration") val duration: Int
        )

        // 视频播放地址响应 (对应 /x/player/playurl)
        @JsonClass(generateAdapter = true)
        data class BiliPlayUrlResponse(
            @Json(name = "code") val code: Int,
            @Json(name = "message") val message: String,
            @Json(name = "data") val data: PlayUrlData?
        )

        @JsonClass(generateAdapter = true)
        data class PlayUrlData(
            @Json(name = "durl") val durl: List<DashUrl>?
        )

        @JsonClass(generateAdapter = true)
        data class DashUrl(
            @Json(name = "url") val url: String,
            @Json(name = "size") val size: Long,
            @Json(name = "backup_url") val backupUrl: List<String>?
        )
    }

    interface BiliApiService {
        // 获取视频详细信息（最重要的是cid）
        @GET("x/web-interface/view")
        suspend fun getVideoInfo(
            @Query("bvid") bvid: String
        ): Response<models.BiliVideoInfoResponse>

        // 获取视频播放地址
        @GET("x/player/playurl")
        suspend fun getPlayUrl(
            @Query("bvid") bvid: String,
            @Query("cid") cid: Long,
            @Query("qn") quality: Int = 80, // 80=1080P, 64=720P, 32=480P, 16=360P
            @Query("fnval") fnval: Int = 1 // 1=flv, 16=DASH
        ): Response<models.BiliPlayUrlResponse>

        // 获取弹幕，返回原始数据体，因为它是压缩的XML
        @GET("x/v1/dm/list.so")
        suspend fun getDanmaku(
            @Query("oid") cid: Long
        ): Response<ResponseBody>
    }
}