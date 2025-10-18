//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.plaza

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AuthManager
//import cc.bbq.xq.RetrofitClient // 移除 RetrofitClient
import cc.bbq.xq.KtorClient // 导入 KtorClient
import cc.bbq.xq.util.ApkInfo
import cc.bbq.xq.util.ApkParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import io.ktor.client.request.forms.* // 导入 Ktor 表单相关类
import io.ktor.http.* // 导入 Ktor HTTP 相关类
import io.ktor.utils.io.*
import io.ktor.http.content.*
import io.ktor.client.call.* // 导入 Ktor call
import io.ktor.client.statement.* // 导入 Ktor statement

enum class ApkUploadService(val displayName: String) {
    KEYUN("氪云"),
    WANYUEYUN("挽悦云")
}

class AppReleaseViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val MAX_INTRO_IMAGES = 3

    val categories = listOf(
        AppCategory(45, 47, "影音阅读"),
        AppCategory(45, 55, "音乐听歌"),
        AppCategory(45, 61, "休闲娱乐"),
        AppCategory(45, 58, "文件管理"),
        AppCategory(45, 59, "图像摄影"),
        AppCategory(45, 53, "输入方式"),
        AppCategory(45, 54, "生活出行"),
        AppCategory(45, 50, "社交通讯"),
        AppCategory(45, 56, "上网浏览"),
        AppCategory(45, 60, "其他类型"),
        AppCategory(45, 62, "跑酷竞技"),
        AppCategory(45, 49, "系统工具"),
        AppCategory(45, 48, "桌面插件"),
        AppCategory(45, 65, "学习教育")
    ).filter { it.categoryId != null && it.subCategoryId != null }
     .distinctBy { it.subCategoryId }
     .sortedBy { it.categoryName }

    val isUpdateMode = mutableStateOf(false)
    private var appId: Long = 0
    private var appVersionId: Long = 0

    val selectedApkUploadService = mutableStateOf(ApkUploadService.KEYUN)

    val appName = mutableStateOf("")
    val packageName = mutableStateOf("")
    val versionName = mutableStateOf("")
    val appVersion = mutableStateOf("")
    val appSize = mutableStateOf("")
    val appIntroduce = mutableStateOf("资源介绍【密码:】 ")
    val appExplain = mutableStateOf("适配性能描述 •\n包名：\n版本：")
    val isPay = mutableStateOf(0)
    val payMoney = mutableStateOf("")
    val selectedCategoryIndex = mutableStateOf(0)
    val apkDownloadUrl = mutableStateOf("")

    val iconUrl = mutableStateOf<String?>(null)
    val localIconUri = mutableStateOf<Uri?>(null)
    val introductionImageUrls = mutableStateListOf<String>()

    val isApkUploading = mutableStateOf(false)
    val isIconUploading = mutableStateOf(false)
    val isIntroImagesUploading = mutableStateOf(false)

    private val _processFeedback = MutableStateFlow<Result<String>?>(null)
    val processFeedback = _processFeedback.asStateFlow()
    val isReleasing = mutableStateOf(false)

    private fun generateUniqueFileName(prefix: String, extension: String): String {
        val timestamp = System.currentTimeMillis()
        val randomSuffix = (100..999).random()
        return "${prefix}_${timestamp}_${randomSuffix}.$extension"
    }

    fun parseAndUploadApk(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _processFeedback.value = Result.success("正在解析APK...")
            val parsedInfo = ApkParser.parse(context, uri)

            if (parsedInfo == null) {
                _processFeedback.value = Result.failure(Throwable("APK 文件解析失败"))
                return@launch
            }

            withContext(Dispatchers.Main) {
                appName.value = parsedInfo.appName
                packageName.value = parsedInfo.packageName
                versionName.value = parsedInfo.versionName
                appVersion.value = parsedInfo.versionName
                appSize.value = parsedInfo.sizeInMb.toString()
                appExplain.value = "适配性能描述 •\n包名：${parsedInfo.packageName}\n版本：${parsedInfo.versionName}"
                localIconUri.value = parsedInfo.tempIconFileUri
                iconUrl.value = null
            }

            val uploadJobs = mutableListOf<kotlinx.coroutines.Job>()

            uploadJobs += launch {
                isApkUploading.value = true
                when (selectedApkUploadService.value) {
                    ApkUploadService.KEYUN -> uploadToKeyun(parsedInfo.tempApkFile) { url -> apkDownloadUrl.value = url }
                    ApkUploadService.WANYUEYUN -> uploadToWanyueyun(parsedInfo.tempApkFile) { url -> apkDownloadUrl.value = url }
                }
                isApkUploading.value = false
            }

            parsedInfo.tempIconFile?.let { iconFile ->
                uploadJobs += launch {
                    isIconUploading.value = true
                    uploadToKeyun(iconFile, "image/*", "图标") { url ->
                        iconUrl.value = url
                    }
                    isIconUploading.value = false
                }
            }
            uploadJobs.joinAll()
        }
    }

    fun uploadIntroductionImages(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentCount = introductionImageUrls.size
            if (currentCount >= MAX_INTRO_IMAGES) {
                _processFeedback.value = Result.failure(Throwable("最多只能上传 $MAX_INTRO_IMAGES 张介绍图"))
                return@launch
            }

            isIntroImagesUploading.value = true
            val remainingSlots = MAX_INTRO_IMAGES - currentCount
            val urisToUpload = uris.take(remainingSlots)

            val uploadJobs = urisToUpload.map { uri ->
                launch {
                    val tempFileName = generateUniqueFileName("intro", "jpg")
                    val tempFile = uriToTempFile(context, uri, tempFileName)
                    tempFile?.let {
                        // **THE FINAL, ULTIMATE FIX**: The illegal `withContext` wrapper is removed.
                        // The UI update now happens directly inside the lambda, as `uploadToKeyun`
                        // guarantees this callback is already on the Main thread.
                        uploadToKeyun(it, "image/*", "介绍图") { url ->
                            if (introductionImageUrls.size < MAX_INTRO_IMAGES) {
                                introductionImageUrls.add(url)
                            }
                        }
                    }
                }
            }
            uploadJobs.joinAll()
            isIntroImagesUploading.value = false
        }
    }
    
    fun removeIntroductionImage(url: String) {
        viewModelScope.launch(Dispatchers.Main) {
            introductionImageUrls.remove(url)
        }
    }

    fun populateFromAppDetail(appDetail: KtorClient.AppDetail) {
        isUpdateMode.value = true
        appId = appDetail.id
        appVersionId = appDetail.apps_version_id

        appName.value = appDetail.appname
        apkDownloadUrl.value = appDetail.download ?: ""

        val explainLines = appDetail.app_explain?.split("\n")
        val pkgNameLine = explainLines?.find { it.startsWith("包名：") }
        packageName.value = pkgNameLine?.substringAfter("包名：")?.trim() ?: ""

        versionName.value = appDetail.version
        appVersion.value = appDetail.version
        appSize.value = appDetail.app_size.replace("MB", "").trim()
        appIntroduce.value = appDetail.app_introduce?.replace("<br>", "\n") ?: ""
        appExplain.value = appDetail.app_explain ?: ""
        isPay.value = appDetail.is_pay
        payMoney.value = if (appDetail.pay_money > 0) appDetail.pay_money.toString() else ""
        selectedCategoryIndex.value = categories.indexOfFirst {
            it.categoryId == appDetail.category_id && it.subCategoryId == appDetail.sub_category_id
        }.takeIf { it != -1 } ?: 0

        iconUrl.value = appDetail.app_icon
        localIconUri.value = null
        introductionImageUrls.clear()
        appDetail.app_introduction_image_array?.let {
            introductionImageUrls.addAll(it)
        }
    }

    fun releaseApp(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val token = AuthManager.getCredentials(context)?.third
            if (token == null) {
                _processFeedback.value = Result.failure(Throwable("错误: 未登录")); return@launch
            }
            if (apkDownloadUrl.value.isBlank()) {
                _processFeedback.value = Result.failure(Throwable("错误: APK下载链接为空, 请等待上传完成或手动填写")); return@launch
            }
            if (iconUrl.value.isNullOrBlank()) {
                _processFeedback.value = Result.failure(Throwable("错误: 应用图标未上传成功")); return@launch
            }
            if (introductionImageUrls.isEmpty()) {
                _processFeedback.value = Result.failure(Throwable("错误: 请至少上传一张应用介绍图")); return@launch
            }

            isReleasing.value = true
            val action = if (isUpdateMode.value) "更新" else "发布"
            _processFeedback.value = Result.success("正在提交${action}信息...")
            try {
                val selectedCategory = categories[selectedCategoryIndex.value]
                val introImagesString = introductionImageUrls.take(MAX_INTRO_IMAGES).joinToString(",")

                //val response = if (isUpdateMode.value) {
                val response = if (isUpdateMode.value) {
                    KtorClient.ApiServiceImpl.updateApp(
                        usertoken = token,
                        apps_id = appId,
                        appname = appName.value,
                        icon = iconUrl.value,
                        app_size = appSize.value,
                        app_introduce = appIntroduce.value,
                        app_introduction_image = introImagesString,
                        file = apkDownloadUrl.value,
                        app_explain = appExplain.value,
                        app_version = appVersion.value,
                        is_pay = isPay.value,
                        pay_money = if (isPay.value == 1) payMoney.value else "",
                        category_id = selectedCategory.categoryId!!,
                        sub_category_id = selectedCategory.subCategoryId!!
                    )
                } else {
                    KtorClient.ApiServiceImpl.releaseApp(
                        usertoken = token,
                        appname = appName.value,
                        icon = iconUrl.value,
                        app_size = appSize.value,
                        app_introduce = appIntroduce.value,
                        app_introduction_image = introImagesString,
                        file = apkDownloadUrl.value,
                        app_explain = appExplain.value,
                        app_version = appVersion.value,
                        is_pay = isPay.value,
                        pay_money = if (isPay.value == 1) payMoney.value else "",
                        category_id = selectedCategory.categoryId!!,
                        sub_category_id = selectedCategory.subCategoryId!!
                    )
                }

                response.onSuccess { result ->
                    _processFeedback.value = Result.success(result.msg ?: "${action}成功, 等待审核")
                    withContext(Dispatchers.Main) { onSuccess() }
                }.onFailure { error ->
                    _processFeedback.value = Result.failure(Throwable(error.message ?: "${action}失败"))
                }
            } catch (e: Exception) {
                _processFeedback.value = Result.failure(e)
            } finally {
                isReleasing.value = false
            }
        }
    }

        fun deleteApp(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val token = AuthManager.getCredentials(context)?.third
            if (token == null) {
                _processFeedback.value = Result.failure(Throwable("错误: 未登录")); return@launch
            }
            if (!isUpdateMode.value || appId == 0L || appVersionId == 0L) {
                _processFeedback.value = Result.failure(Throwable("错误: 应用ID无效，无法删除")); return@launch
            }

            isReleasing.value = true // Reuse releasing flag for loading state
            _processFeedback.value = Result.success("正在删除应用...")
            try {
                //val response = RetrofitClient.instance.deleteApp(
                val response = KtorClient.ApiServiceImpl.deleteApp(
                    usertoken = token,
                    apps_id = appId,
                    app_version_id = appVersionId
                )

                response.onSuccess { result ->
                    _processFeedback.value = Result.success(result.msg ?: "删除成功")
                    withContext(Dispatchers.Main) { onSuccess() }
                }.onFailure { error ->
                    _processFeedback.value = Result.failure(Throwable(error.message ?: "删除失败"))
                }
            } catch (e: Exception) {
                _processFeedback.value = Result.failure(e)
            } finally {
                isReleasing.value = false
            }
        }
    }

    //private suspend fun uploadToKeyun(file: File, mediaType: String = "application/octet-stream", contextMessage: String = "文件", onSuccess: (String) -> Unit) {
    private suspend fun uploadToKeyun(file: File, mediaType: String = "image/*", contextMessage: String = "文件", onSuccess: (String) -> Unit) {
        try {
            val response: HttpResponse = KtorClient.uploadHttpClient.post("api.php") {
                body = MultiPartFormDataContent(
                    formData {
                        append("file", file.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, mediaType)
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        })
                    }
                )
            }

            //val uploadResponse: KtorClient.UploadResponse = response.body()

            val uploadResponse: KtorClient.UploadResponse? = try {
                response.body()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _processFeedback.value = Result.failure(Throwable("$contextMessage (氪云): 响应解析失败: ${e.message}"))
                }
                return
            }

            if (uploadResponse?.code == 1 || uploadResponse?.exists == 1) {
                withContext(Dispatchers.Main) {
                    _processFeedback.value = Result.success("$contextMessage (氪云): ${uploadResponse.msg}")
                    onSuccess(uploadResponse.downurl ?: "")
                }
            } else {
                withContext(Dispatchers.Main) {
                    _processFeedback.value = Result.failure(Throwable("$contextMessage (氪云): ${uploadResponse?.msg ?: "上传失败"}"))
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _processFeedback.value = Result.failure(Throwable("$contextMessage (氪云): ${e.message}"))
            }
        } finally {
            file.delete()
        }
    }

    private suspend fun uploadToWanyueyun(file: File, onSuccess: (String) -> Unit) {
        try {
            val response: HttpResponse = KtorClient.wanyueyunUploadHttpClient.post("upload") {
                body = MultiPartFormDataContent(
                    formData {
                        append("Api", "小趣API")
                        append("file", file.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        })
                    }
                )
            }
            //val uploadResponse: KtorClient.WanyueyunUploadResponse = response.body()
            val uploadResponse: KtorClient.WanyueyunUploadResponse? = try {
                response.body()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _processFeedback.value = Result.failure(Throwable("APK (挽悦云): 响应解析失败: ${e.message}"))
                }
                return
            }

            if (uploadResponse?.code == 200 && !uploadResponse.data.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    _processFeedback.value = Result.success("APK (挽悦云): ${uploadResponse.msg}")
                    onSuccess(uploadResponse.data)
                }
            } else {
                withContext(Dispatchers.Main) {
                    _processFeedback.value = Result.failure(Throwable("APK (挽悦云): ${uploadResponse?.msg ?: "上传失败"}"))
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _processFeedback.value = Result.failure(Throwable("APK (挽悦云): ${e.message}"))
            }
        } finally {
            file.delete()
        }
    }

    fun clearProcessFeedback() {
        _processFeedback.value = null
    }

    private fun uriToTempFile(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, fileName)
            file.outputStream().use { outputStream ->
                inputStream.use { it.copyTo(outputStream) }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}