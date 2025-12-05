// /app/src/main/java/cc/bbq/xq/ui/plaza/AppReleaseViewModel.kt
package cc.bbq.xq.ui.plaza

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import cc.bbq.xq.data.repository.IAppStoreRepository
import cc.bbq.xq.data.repository.SineOpenMarketRepository
import cc.bbq.xq.data.repository.XiaoQuRepository
import cc.bbq.xq.data.unified.UnifiedAppReleaseParams
import cc.bbq.xq.util.ApkParser
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.net.toUri // 导入 toUri 扩展函数
import androidx.compose.runtime.mutableIntStateOf
import cc.bbq.xq.SineShopClient

// 小趣空间分类模型
data class AppCategory(
    val categoryId: Int?,
    val subCategoryId: Int?,
    val categoryName: String
)

enum class ApkUploadService(val displayName: String) {
    KEYUN("氪云"),
    WANYUEYUN("挽悦云")
}

class AppReleaseViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    
    // --- 商店选择 ---
    private val _selectedStore = MutableStateFlow(AppStore.XIAOQU_SPACE)
    val selectedStore = _selectedStore.asStateFlow()
    
    fun onStoreSelected(store: AppStore) {
        _selectedStore.value = store
        // 切换商店时重置部分状态
        clearProcessFeedback()
    }

    // --- 仓库实例 ---
    private val xiaoQuRepo: IAppStoreRepository = XiaoQuRepository(KtorClient.ApiServiceImpl)
    private val sineOpenRepo: IAppStoreRepository = SineOpenMarketRepository()

    private fun getCurrentRepo(): IAppStoreRepository {
        return when (_selectedStore.value) {
            AppStore.XIAOQU_SPACE -> xiaoQuRepo
            AppStore.SINE_OPEN_MARKET -> sineOpenRepo
            else -> xiaoQuRepo // 默认
        }
    }

    // --- 通用状态 ---
    val appName = mutableStateOf("")
    val packageName = mutableStateOf("")
    val versionName = mutableStateOf("")
    val versionCode = mutableStateOf(0L)
    val appSize = mutableStateOf("") // MB
    val localIconUri = mutableStateOf<Uri?>(null)
    val tempIconFile = mutableStateOf<File?>(null)
    val tempApkFile = mutableStateOf<File?>(null) // 保存本地APK文件引用
    
    // --- 小趣空间特定状态 ---
    val isUpdateMode = mutableStateOf(false)
    private var appId: Long = 0
    private var appVersionId: Long = 0
    val selectedApkUploadService = mutableStateOf(ApkUploadService.KEYUN)
    val appIntroduce = mutableStateOf("资源介绍【密码:】 ")
    val appExplain = mutableStateOf("适配性能描述 •\n包名：\n版本：")
    val isPay = mutableStateOf(0)
    val payMoney = mutableStateOf("")
    val selectedCategoryIndex = mutableStateOf(0)
    val apkDownloadUrl = mutableStateOf("") // 小趣空间用外链
    val iconUrl = mutableStateOf<String?>(null) // 小趣空间用网络图标
    val introductionImageUrls = mutableStateListOf<String>() // 小趣空间用网络图床
    
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

    // --- 弦开放平台特定状态 ---
    // 应用类型
    val appTypeOptions = listOf("手表应用", "手机应用", "大屏应用", "TV应用", "WearOS应用")
    // val selectedAppTypeIndex = mutableStateOf(1) // 手机应用 (默认)
    // val appTypeId: Int get() = selectedAppTypeIndex.value + 1
    val appTypeId = mutableStateOf(2) // 手机应用 (默认 ID 为 2)

    // 版本类型
    val versionTypeOptions = listOf("官方版", "正式版", "测试版", "公测版", "美化版", "破解版", "修改版", "免费版", "定制版", "手表版")
    // val selectedVersionTypeIndex = mutableStateOf(1) // 正式版 (默认)
    // val appVersionTypeId: Int get() = selectedVersionTypeIndex.value + 1
    val appVersionTypeId = mutableStateOf(2) // 正式版 (默认 ID 为 2)

    // 应用标签 (从API获取，这里简化)
    //val tagOptions = listOf(
    //    "星标应用", "弦-应用", "应用商店", "实用工具", "视频播放", "通讯社交", "游戏", "搞怪整活", "数字消费", "搞机刷机",
    //    "教育学习", "输入法", "WearOS", "文本编辑", "文件管理", "图像处理", "浏览器", "厂商提取", "系统优化", "启动器",
    //    "生活便利", "表盘", "音乐播放", "地图导航", "图文阅读"
    //)
    // val selectedTagIndex = mutableStateOf(3) // 实用工具 (默认)
    // val appTags: String get() = (selectedTagIndex.value).toString() // 将索引转换为字符串
    val appTags = mutableStateOf(3) // 实用工具 (默认 ID 为 3)

    val sdkMin = mutableStateOf(21)
    val sdkTarget = mutableStateOf(33)
    val developer = mutableStateOf("")
    val source = mutableStateOf("互联网")
    val describe = mutableStateOf("介绍一下你的应用…")
    val updateLog = mutableStateOf("本次更新的内容…")
    val uploadMessage = mutableStateOf("给审核员的留言…")
    val keyword = mutableStateOf("关键字")
    val isWearOs = mutableStateOf(0)
    val abi = mutableStateOf(0) // 0: 不限
    val screenshotsUris = mutableStateListOf<Uri>() // 本地截图URI
    val tempScreenshotFiles = mutableListOf<File>()
    private val _tagOptions = MutableStateFlow<List<String>>(emptyList())
    val tagOptions: StateFlow<List<String>> = _tagOptions.asStateFlow()

    // 新增：选中的标签索引
    val selectedTagIndex = mutableIntStateOf(0)

    // --- 进度状态 ---
    val isApkUploading = mutableStateOf(false)
    val isIconUploading = mutableStateOf(false)
    val isIntroImagesUploading = mutableStateOf(false)
    val isReleasing = mutableStateOf(false)
    private val _processFeedback = MutableStateFlow<Result<String>?>(null)
    val processFeedback = _processFeedback.asStateFlow()
    
    private val MAX_INTRO_IMAGES = 3

    init {
        loadAppTags()
    }

    // 新增：加载应用标签
    private fun loadAppTags() {
        viewModelScope.launch {
            try {
                val result = SineShopClient.getAppTagList()
                if (result.isSuccess) {
                    val tags = result.getOrNull()?.map { it.name } ?: emptyList()
                    _tagOptions.value = tags
                } else {
                    _processFeedback.value = Result.failure(Throwable("加载标签失败: ${result.exceptionOrNull()?.message}"))
                }
            } catch (e: Exception) {
                _processFeedback.value = Result.failure(Throwable("加载标签时发生异常: ${e.message}"))
            }
        }
    }

    // --- APK 解析 ---
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
                versionCode.value = parsedInfo.versionCode
                appSize.value = parsedInfo.sizeInMb.toString()
                
                // 保存临时文件引用
                tempApkFile.value = parsedInfo.tempApkFile
                tempIconFile.value = parsedInfo.tempIconFile
                localIconUri.value = parsedInfo.tempIconFileUri
                
                // 自动填充字段
                appExplain.value = "适配性能描述 •\n包名：${parsedInfo.packageName}\n版本：${parsedInfo.versionName}"
                
                // 如果是弦平台，自动填充 SDK 信息 (ApkParser 未来支持 minSdk/targetSdk)
                // 假设 ApkParser 未来支持 minSdk/targetSdk
            }

            // 仅小趣空间需要立即上传 APK 和图标到图床
            if (_selectedStore.value == AppStore.XIAOQU_SPACE) {
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
            } else {
                _processFeedback.value = Result.success("APK解析完成，准备发布")
            }
        }
    }

    // --- 图片处理 ---
    
    // 小趣空间：上传介绍图到图床
    fun uploadIntroductionImages(uris: List<Uri>) {
        if (_selectedStore.value != AppStore.XIAOQU_SPACE) return
        
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
                    val tempFileName = "intro_${System.currentTimeMillis()}.jpg"
                    val tempFile = uriToTempFile(context, uri, tempFileName)
                    tempFile?.let {
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
    
    // 弦开放平台：选择截图（保存本地URI，发布时一起上传）
    fun addScreenshots(uris: List<Uri>) {
        if (_selectedStore.value != AppStore.SINE_OPEN_MARKET) return
        screenshotsUris.addAll(uris)
        // 可以在这里异步将 URI 转为 File 存入 tempScreenshotFiles
        viewModelScope.launch(Dispatchers.IO) {
            uris.forEach { uri ->
                val file = uriToTempFile(context, uri, "screenshot_${System.currentTimeMillis()}.png")
                file?.let { tempScreenshotFiles.add(it) }
            }
        }
    }

    fun removeIntroductionImage(url: String) {
        introductionImageUrls.remove(url)
    }
    
    fun removeScreenshot(uri: Uri) {
        screenshotsUris.remove(uri)
        // 同步移除 tempScreenshotFiles 逻辑略复杂，建议重置或简单处理
        tempScreenshotFiles.removeIf { it.toUri() == uri }
    }

    private fun createStreamInputProvider(file: File): InputProvider {
        return InputProvider { file.inputStream().asInput() }
    }
    
     // --- 发布逻辑 ---
    
    fun releaseApp(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            isReleasing.value = true
            _processFeedback.value = Result.success("正在准备发布...")
            
            try {
                val repo = getCurrentRepo()
                
                // 构建参数
                val params = UnifiedAppReleaseParams(
                    store = _selectedStore.value,
                    appName = appName.value,
                    packageName = packageName.value,
                    versionName = versionName.value,
                    versionCode = versionCode.value,
                    sizeInMb = appSize.value.toDoubleOrNull() ?: 0.0,
                    iconFile = tempIconFile.value,
                    iconUrl = iconUrl.value,
                    apkFile = tempApkFile.value,
                    apkUrl = apkDownloadUrl.value,
                    
                    // 小趣空间
                    introduce = appIntroduce.value,
                    explain = appExplain.value,
                    introImages = introductionImageUrls.toList(),
                    categoryId = categories.getOrNull(selectedCategoryIndex.value)?.categoryId,
                    subCategoryId = categories.getOrNull(selectedCategoryIndex.value)?.subCategoryId,
                    isPay = isPay.value,
                    payMoney = if (isPay.value == 1) payMoney.value else "",
                    isUpdate = isUpdateMode.value,
                    appId = appId,
                    appVersionId = appVersionId,
                    
                    // 弦开放平台
                    appTypeId = appTypeId.value,
                    appVersionTypeId = appVersionTypeId.value,
                    appTags = tagOptions.value.getOrNull(selectedTagIndex.intValue) ?: "3",
                    sdkMin = sdkMin.value,
                    sdkTarget = sdkTarget.value,
                    developer = developer.value,
                    source = source.value,
                    describe = describe.value,
                    updateLog = updateLog.value,
                    uploadMessage = uploadMessage.value,
                    keyword = keyword.value,
                    isWearOs = isWearOs.value,
                    abi = abi.value,
                    screenshots = tempScreenshotFiles.toList()
                )
                
                val result = repo.releaseApp(params)
                
                if (result.isSuccess) {
                    _processFeedback.value = Result.success("发布成功！")
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    _processFeedback.value = Result.failure(result.exceptionOrNull() ?: Exception("发布失败"))
                }
                
            } catch (e: Exception) {
                _processFeedback.value = Result.failure(e)
            } finally {
                isReleasing.value = false
            }
        }
    }
    
    private suspend fun uploadToKeyun(file: File, mediaType: String = "application/octet-stream", contextMessage: String = "文件", onSuccess: (String) -> Unit) {
        try {
            val response = KtorClient.uploadHttpClient.submitFormWithBinaryData(
                url = "api.php",
                formData = formData {
                    append("file", createStreamInputProvider(file), Headers.build {
                        append(HttpHeaders.ContentType, mediaType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                    })
                }
            )

            if (response.status.isSuccess()) {
                val responseBody: KtorClient.UploadResponse = response.body<KtorClient.UploadResponse>()
                if (responseBody.code == 0 && !responseBody.downurl.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        _processFeedback.value = Result.success("$contextMessage (氪云): ${responseBody.msg}")
                        onSuccess(responseBody.downurl)
                    }
                } else {
                    withContext(Dispatchers.Main){
                        _processFeedback.value = Result.failure(Throwable("$contextMessage (氪云): ${responseBody.msg}"))
                    }
                }
            } else {
                withContext(Dispatchers.Main){
                    _processFeedback.value = Result.failure(Throwable("$contextMessage (氪云): 网络错误 ${response.status}"))
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main){
                                _processFeedback.value = Result.failure(Throwable("$contextMessage (氪云): ${e.message}"))
            }
        } finally {
            // file.delete() // 暂时注释掉，因为可能需要复用
        }
    }

    private suspend fun uploadToWanyueyun(file: File, onSuccess: (String) -> Unit) {
        try {
            val response = KtorClient.wanyueyunUploadHttpClient.submitFormWithBinaryData(
                url = "upload",
                formData = formData {
                    append("Api", "小趣API")
                    append("file", createStreamInputProvider(file), Headers.build {
                        append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
                        append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                    })
                }
            )

            if (response.status.isSuccess()) {
                val responseBody: KtorClient.WanyueyunUploadResponse = response.body<KtorClient.WanyueyunUploadResponse>()
                if (responseBody.code == 200 && !responseBody.data.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        _processFeedback.value = Result.success("APK (挽悦云): ${responseBody.msg}")
                        onSuccess(responseBody.data)
                    }
                } else {
                    withContext(Dispatchers.Main){
                        _processFeedback.value = Result.failure(Throwable("APK (挽悦云): ${responseBody.msg}"))
                    }
                }
            } else {
                withContext(Dispatchers.Main){
                    _processFeedback.value = Result.failure(Throwable("APK (挽悦云): 网络错误 ${response.status}"))
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main){
                _processFeedback.value = Result.failure(Throwable("APK (挽悦云): ${e.message}"))
            }
        } finally {
             // file.delete()
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

    // --- Setter 方法用于下拉菜单 ---
    fun setAppTypeId(id: Int) {
        appTypeId.value = id
    }
    
    fun setAppVersionTypeId(id: Int) {
        appVersionTypeId.value = id
    }
    
    fun setAppTags(id: Int) {
        appTags.value = id
    }

    fun removeIntroductionImage(url: String) {
        introductionImageUrls.remove(url)
    }
}