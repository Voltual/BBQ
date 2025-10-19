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
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.utils.io.charsets.Charsets
import io.ktor.client.call.body

class SuperCachePlugin private constructor() {
    companion object : HttpClientPlugin<SuperCachePlugin.Config, SuperCachePlugin> {
        override val key: AttributeKey<SuperCachePlugin> = AttributeKey("SuperCachePlugin")

        override fun prepare(block: Config.() -> Unit): SuperCachePlugin {
            val config = Config().apply(block)
            return SuperCachePlugin()
        }

        override fun install(plugin: SuperCachePlugin, scope: HttpClient) {
            plugin.install(scope)
        }
    }

    class Config {
        // 可以在这里定义插件配置
    }

    private val cacheDao = BBQApplication.instance.database.networkCacheDao()
    private val storageSettingsDataStore = StorageSettingsDataStore(BBQApplication.instance)

    // MD5哈希函数
    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val result = digest.digest(input.toByteArray(Charsets.UTF_8))
        return result.joinToString("") { "%02x".format(it) }
    }

    // 生成唯一的请求Key
    private fun generateRequestKey(request: HttpRequestBuilder): String {
        val keyBuilder = StringBuilder().apply {
            append(request.method.value).append("_")
            append(request.url.buildString())
            
            // 处理请求体
            when (val body = request.body) {
                is FormDataContent -> append("_").append(body.formData.formUrlEncode())
                is OutgoingContent.ByteArrayContent -> append("_").append(String(body.bytes(), Charsets.UTF_8))
                is OutgoingContent.ReadChannelContent -> {
                    val channel = body.readFrom()
                    val bytes = runBlocking { channel.toByteArray() }
                    append("_").append(String(bytes, Charsets.UTF_8))
                }
            }
        }
        return md5(keyBuilder.toString())
    }

    private fun install(client: HttpClient) {
        // 请求拦截
        client.sendPipeline.intercept(HttpSendPipeline.State) {
            val request = context
            val noCache = request.attributes.getOrNull(AttributeKey<Boolean>("NoCache")) ?: false
            if (noCache) {
                println("[SKIP] Request for ${request.url} has @NoCache")
                return@intercept
            }

            val isCacheModeEnabled = runBlocking { 
                storageSettingsDataStore.isSuperCacheEnabledFlow.first() 
            }

            if (isCacheModeEnabled) {
                println("[CACHE MODE] Intercepting request for ${request.url}")
                val requestKey = generateRequestKey(request)
                val cachedResponse = runBlocking { cacheDao.getCache(requestKey) }

                if (cachedResponse != null) {
                    println("[CACHE HIT] Found cache for key: $requestKey")
                    request.body = TextContent(
                        cachedResponse.responseJson,
                        ContentType.Application.Json
                    )
                } else {
                    println("[CACHE MISS] No cache found for key: $requestKey")
                    request.body = TextContent(
                        """{"code":400,"msg":"缓存未命中","data":[],"timestamp":0}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                }
            }
        }

        // 响应拦截
        client.receivePipeline.intercept(HttpReceivePipeline.State) {
            val response = subject
            val request = response.request
            val noCache = request.attributes.getOrNull(AttributeKey<Boolean>("NoCache")) ?: false
            if (noCache || !response.status.isSuccess()) {
                return@intercept
            }

            val requestKey = generateRequestKey(request)
            runBlocking {
                try {
                    val responseBody = response.body<String>()
                    println("[CACHE WRITE] Caching response for key: $requestKey")
                    cacheDao.insert(NetworkCacheEntry(requestKey, responseBody))
                } catch (e: Exception) {
                    println("Failed to cache response: ${e.message}")
                }
            }
        }
    }
}

// 标记请求不缓存
fun HttpRequestBuilder.noCache() {
    attributes.put(AttributeKey("NoCache"), true)
}