//network/SuperCacheInterceptor.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.network

import android.util.Log
import cc.bbq.xq.BBQApplication
import cc.bbq.xq.data.StorageSettingsDataStore
import cc.bbq.xq.data.db.NetworkCacheEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Invocation
import java.security.MessageDigest

class SuperCacheInterceptor : Interceptor {

    private val TAG = "SuperCacheInterceptor"
    private val cacheDao = BBQApplication.instance.database.networkCacheDao()
    private val storageSettingsDataStore = StorageSettingsDataStore(BBQApplication.instance)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // --- 1. 检查是否需要跳过缓存 ---
        val invocation = request.tag(Invocation::class.java)
        val noCacheAnnotation = invocation?.method()?.getAnnotation(NoCache::class.java)
        if (noCacheAnnotation != null) {
            Log.d(TAG, "[SKIP] Request for ${request.url} has @NoCache, proceeding without cache.")
            return chain.proceed(request)
        }

        // --- 2. 检查“超级缓存模式”是否开启 ---
        // runBlocking 在这里是可接受的，因为拦截器本身在 I/O 线程上运行，
        // 且我们需要同步地决定是走网络还是走缓存。
        val isCacheModeEnabled = runBlocking { storageSettingsDataStore.isSuperCacheEnabledFlow.first() }

        // --- 3. 生成唯一的请求 Key ---
        val requestKey = generateRequestKey(request)

        // --- 4. 缓存模式下的逻辑 ---
        if (isCacheModeEnabled) {
            Log.d(TAG, "[CACHE MODE] Intercepting request for ${request.url}")
            val cachedResponse = runBlocking { cacheDao.getCache(requestKey) }

            if (cachedResponse != null) {
                Log.i(TAG, "[CACHE HIT] Found cache for key: $requestKey")
                // 从数据库构建一个伪造的成功响应
                return Response.Builder()
                    .code(200)
                    .message("OK (from SuperCache)")
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_2)
                    .body(cachedResponse.responseJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .addHeader("content-type", "application/json")
                    .build()
            } else {
                Log.w(TAG, "[CACHE MISS] No cache found for key: $requestKey")
                // 在缓存模式下，如果未命中，则返回一个特定的“客户端错误”，告知上层无可用离线数据
                return Response.Builder()
                    .code(418) // 418 I'm a teapot, 一个有趣的、表示“我无法处理这个请求”的客户端错误码
                    .message("No offline data available in SuperCache")
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_2)
                    .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }
        }

        // --- 5. 正常模式下的逻辑 ---
        Log.d(TAG, "[NORMAL MODE] Proceeding with network request for ${request.url}")
        val originalResponse: Response
        try {
            originalResponse = chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TAG, "[NETWORK ERROR] Request failed: ${e.message}")
            throw e // 网络错误直接抛出，不进行缓存
        }

        // --- 6. 缓存成功的响应 ---
        if (originalResponse.isSuccessful) {
            // 响应体只能被读取一次，所以我们需要先读取它，然后再用它创建一个新的响应体返回
            val responseBody = originalResponse.body
            if (responseBody != null) {
                val responseString = responseBody.string()

                // 在这里可以添加业务码检查，例如，只缓存 code == 1 的响应
                // 为了简单起见，我们暂时缓存所有成功的 HTTP 响应
                if (responseString.contains("\"code\":1")) {
                    Log.i(TAG, "[CACHE WRITE] Caching successful response for key: $requestKey")
                    runBlocking {
                        cacheDao.insert(NetworkCacheEntry(requestKey, responseString))
                    }
                } else {
                    Log.d(TAG, "[CACHE SKIP] Response code is not 1, skipping cache write.")
                }

                // 将读取过的字符串重新包装成一个新的响应体返回
                val newResponseBody = responseString.toResponseBody(responseBody.contentType())
                return originalResponse.newBuilder().body(newResponseBody).build()
            }
        }

        return originalResponse
    }

    /**
     * 为一个请求生成一个唯一的、可预测的 Key。
     * 它考虑了 URL、方法和请求体。
     */
    private fun generateRequestKey(request: Request): String {
        val keyBuilder = StringBuilder()
        keyBuilder.append(request.method).append("_")
        keyBuilder.append(request.url.toString())

        // 将请求体的内容也加入到 key 的计算中，以区分 POST 请求
        request.body?.let {
            val buffer = okio.Buffer()
            it.writeTo(buffer)
            keyBuilder.append("_").append(buffer.readByteString().utf8())
        }

        // 使用 MD5 哈希来确保 key 的长度是固定的，并且是唯一的
        return md5(keyBuilder.toString())
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val result = digest.digest(input.toByteArray())
        return result.joinToString("") { "%02x".format(it) }
    }
}