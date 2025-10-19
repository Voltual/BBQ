package cc.bbq.xq.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.http.content.*
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import cc.bbq.xq.BBQApplication
import cc.bbq.xq.data.StorageSettingsDataStore
import cc.bbq.xq.data.db.NetworkCacheEntry
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement

val SuperCachePlugin = createClientPlugin("SuperCachePlugin") {

    val cacheDao = BBQApplication.instance.database.networkCacheDao()
    val storageSettingsDataStore = StorageSettingsDataStore(BBQApplication.instance)

    // 生成唯一的请求 Key
    fun generateRequestKey(request: HttpRequestData): String {
        val keyBuilder = StringBuilder()
        keyBuilder.append(request.method.value).append("_")
        keyBuilder.append(request.url.toString())

        // 将请求体的内容也加入到 key 的计算中，以区分 POST 请求
        request.body?.let { body ->
            if (body is FormDataContent) {
                val formDataString = body.formData.formUrlEncode()
                keyBuilder.append("_").append(formDataString)
            } else if (body is OutgoingContent.ByteArrayContent) {
                keyBuilder.append("_").append(String(body.bytes(), Charsets.UTF_8))
            }
        }

        // 使用 MD5 哈希来确保 key 的长度是固定的，并且是唯一的
        return md5(keyBuilder.toString())
    }

    fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val result = digest.digest(input.toByteArray())
        return result.joinToString("") { "%02x".format(it) }
    }

    on(BeforeSend) { request ->
        val noCacheAnnotation = request.attributes.getOrNull(AttributeKey<Boolean>("NoCache"))
        if (noCacheAnnotation == true) {
            println("[SKIP] Request for ${request.url} has @NoCache, proceeding without cache.")
            return@on
        }
            // --- 2. 检查“超级缓存模式”是否开启 ---
        // runBlocking 在这里是可接受的，因为拦截器本身在 I/O 线程上运行，
        // 且我们需要同步地决定是走网络还是走缓存。
        val isCacheModeEnabled = runBlocking { storageSettingsDataStore.isSuperCacheEnabledFlow.first() }
        
            // --- 3. 生成唯一的请求 Key ---
        val requestKey = generateRequestKey(request)

        if (isCacheModeEnabled) {
            println("[CACHE MODE] Intercepting request for ${request.url}")
            val cachedResponse = runBlocking { cacheDao.getCache(requestKey) }

            if (cachedResponse != null) {
                println("[CACHE HIT] Found cache for key: $requestKey")
                // 从数据库构建一个伪造的成功响应
                 val body = TextContent(
                    cachedResponse.responseJson,
                    ContentType.Application.Json
                )
                return@on body
            } else {
                println("[CACHE MISS] No cache found for key: $requestKey")
                // 在缓存模式下，如果未命中，则返回一个特定的“客户端错误”，告知上层无可用离线数据
                
                 val errorBody = """{"code":400,"msg":"缓存未命中","data":[],"timestamp":0}"""
                    val body = TextContent(
                    errorBody,
                    ContentType.Application.Json,
                    HttpStatusCode.BadRequest
                )
                 return@on body
            }
        }
    }
    
        on(ResponseReceived){response ->
        val request = response.request
        val noCacheAnnotation = request.attributes.getOrNull(AttributeKey<Boolean>("NoCache"))
        if (noCacheAnnotation == true) {
            println("[SKIP] Response for ${request.url} has @NoCache, proceeding without cache.")
            return@on
        }
                // --- 3. 生成唯一的请求 Key ---
        val requestKey = generateRequestKey(request)
        
            if (response.status.isSuccess()) {
            val responseBody = response.bodyAsChannel()
            val responseBytes = responseBody.toByteArray()
            val responseString = String(responseBytes, Charsets.UTF_8)

            println("[CACHE WRITE] Caching successful response for key: $requestKey")
                runBlocking {
                    cacheDao.insert(NetworkCacheEntry(requestKey, responseString))
                }
        }
    }
}

// Extension function to easily mark requests as not cacheable
fun HttpRequestBuilder.noCache() {
    attributes.put(AttributeKey("NoCache"), true)
}