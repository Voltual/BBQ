// /app/src/main/java/cc/bbq/xq/data/repository/SineOpenMarketRepository.kt
package cc.bbq.xq.data.repository

import cc.bbq.xq.AppStore
import cc.bbq.xq.OpenMarketSineWorldClient
import cc.bbq.xq.data.unified.*
import java.io.File

class SineOpenMarketRepository : IAppStoreRepository {    
    // 不需要单独的图片上传接口，图片随表单一起提交
    override suspend fun uploadImage(file: File, type: String): Result<String> = Result.success(file.absolutePath)
    override suspend fun uploadApk(file: File, serviceType: String): Result<String> = Result.success(file.absolutePath)

    override suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit> {
        return try {
            if (params.iconFile == null) throw Exception("必须提供图标文件")
            if (params.apkFile == null) throw Exception("必须提供APK文件")
            if (params.screenshots.isNullOrEmpty()) throw Exception("至少提供一张截图")

            // 1. 构造预上传信息
            val preUploadInfo = OpenMarketSineWorldClient.AppReleaseInfo(
                appName = params.appName,
                packageName = params.packageName,
                versionName = params.versionName,
                versionCode = params.versionCode.toString(),
                appTypeId = params.appTypeId ?: 1,
                appVersionTypeId = params.appVersionTypeId ?: 1,
                appTags = params.appTags ?: "1",
                appSdkMin = params.sdkMin ?: 1,
                appSdkTarget = params.sdkTarget ?: 1,
                appDeveloper = params.developer ?: "",
                appSource = params.source ?: "互联网",
                appDescribe = params.describe ?: "",
                appUpdateLog = params.updateLog ?: "",
                uploadMessage = params.uploadMessage ?: "",
                keyword = params.keyword ?: "",
                appIsWearos = params.isWearOs ?: 0,
                appAbi = params.abi ?: 0,
                downloadSize = (params.sizeInMb * 1024 * 1024).toLong(), // MB 转 Byte
                iconFile = params.iconFile,
                screenshotFiles = params.screenshots
            )

            // 2. 执行预上传
            val preResult = OpenMarketSineWorldClient.preUpload(preUploadInfo).getOrThrow()
            val uploadToken = preResult.uploadToken

            // 3. 上传 APK
            OpenMarketSineWorldClient.uploadApk(params.apkFile, uploadToken).getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}