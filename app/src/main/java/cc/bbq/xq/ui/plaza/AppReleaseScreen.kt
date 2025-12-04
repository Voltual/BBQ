// /app/src/main/java/cc/bbq/xq/ui/plaza/AppReleaseScreen.kt
//Copyright (C) 2025 Voltual
package cc.bbq.xq.ui.plaza

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cc.bbq.xq.AppStore
import cc.bbq.xq.R
import cc.bbq.xq.ui.ImagePreview
import cc.bbq.xq.ui.theme.*
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import cc.bbq.xq.ui.theme.AppStoreDropdownMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppReleaseScreen(
    viewModel: AppReleaseViewModel,
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val internalSnackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val selectedStore by viewModel.selectedStore.collectAsStateWithLifecycle()

    val processFeedback by viewModel.processFeedback.collectAsStateWithLifecycle()
    LaunchedEffect(processFeedback) {
        processFeedback?.let { result ->
            val message = result.fold(
                onSuccess = { it },
                onFailure = { it.message ?: "发生未知错误" }
            )
            val duration = if (result.isSuccess) SnackbarDuration.Short else SnackbarDuration.Long
            scope.launch {
                internalSnackbarHostState.showSnackbar(message, duration = duration)
            }
            viewModel.clearProcessFeedback()
        }
    }

    val apkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.parseAndUploadApk(it) }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            if (selectedStore == AppStore.XIAOQU_SPACE) {
                viewModel.uploadIntroductionImages(uris)
            } else {
                viewModel.addScreenshots(uris)
            }
        }
    }

    val isUpdateMode by viewModel.isUpdateMode

    Box(modifier = modifier.fillMaxSize()) {
        val isAnyTaskRunning = viewModel.isApkUploading.value || viewModel.isIconUploading.value || viewModel.isIntroImagesUploading.value || viewModel.isReleasing.value

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            
            // 商店选择
            item {
                AppStoreDropdownMenu(
                    selectedStore = selectedStore,
                    onStoreChange = { viewModel.onStoreSelected(it) },
                    // 仅支持小趣空间和弦开放平台
                    appStores = listOf(AppStore.XIAOQU_SPACE, AppStore.SINE_OPEN_MARKET)
                )
            }

            // --- 公共部分：APK上传与图标 ---
            item {
                if (selectedStore == AppStore.XIAOQU_SPACE) {
                    ApkUploadServiceDropdown(viewModel)
                }
            }

            item {
                val selectedService by viewModel.selectedApkUploadService
                val buttonText = if (selectedStore == AppStore.XIAOQU_SPACE) {
                    if (isUpdateMode) "1. 选择新版 APK (上传至${selectedService.displayName})" else "1. 选择并上传 APK (至${selectedService.displayName})"
                } else {
                    "1. 选择 APK (解析并准备上传)"
                }
                
                BBQButton(
                    onClick = { apkLauncher.launch("application/vnd.android.package-archive") },
                    modifier = Modifier.fillMaxWidth(),
                    text = { Text(buttonText) }
                )
            }

            item {
                val iconUrl by viewModel.iconUrl
                val localIconUri by viewModel.localIconUri

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val model: Any? = iconUrl ?: localIconUri
                    if (model != null) {
                        SubcomposeAsyncImage(
                            model = model,
                            contentDescription = "应用图标",
                            modifier = Modifier.size(64.dp),
                            loading = { CircularProgressIndicator() },
                            error = { Icon(Icons.Filled.BrokenImage, contentDescription = "加载失败") }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            if (iconUrl != null) "当前图标" else "已解析图标",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // --- 小趣空间表单 ---
            if (selectedStore == AppStore.XIAOQU_SPACE) {
                item { FormTextField(label = "应用名称", state = viewModel.appName) }
                item { FormTextField(label = "APK 下载链接 (自动填充)", state = viewModel.apkDownloadUrl, singleLine = true) }
                item { FormTextField(label = "包名 (自动填充)", state = viewModel.packageName, enabled = !isUpdateMode) }
                item { FormTextField(label = "版本名 (自动填充)", state = viewModel.versionName, enabled = false) }
                item { FormTextField(label = "文件大小 (MB)", state = viewModel.appSize, enabled = false) }
                item { FormTextField(label = "资源介绍", state = viewModel.appIntroduce, singleLine = false, minLines = 3) }
                item { FormTextField(label = "适配性能描述", state = viewModel.appExplain, singleLine = false, minLines = 4) }
                
                item {
                    Column {
                        Text("2. 上传应用介绍图", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        BBQOutlinedButton(
                            onClick = { imageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            text = { Text("选择图片") }
                        )
                        // 显示已上传图片预览 (复用逻辑)
                         if (viewModel.introductionImageUrls.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.introductionImageUrls.forEach { url ->
                                    ImagePreviewItem(
                                        imageUrl = url,
                                        onRemoveClick = { viewModel.removeIntroductionImage(url) },
                                        onImageClick = { navController.navigate(ImagePreview(url).createRoute()) }
                                    )
                                }
                            }
                        }
                    }
                }
                
                item { CategoryDropdown(viewModel) }
                item { PaymentSettings(viewModel) }
            }

            // --- 弦开放平台表单 ---
            if (selectedStore == AppStore.SINE_OPEN_MARKET) {
                item { FormTextField(label = "应用名称", state = viewModel.appName) }
                item { FormTextField(label = "包名", state = viewModel.packageName) }
                item { FormTextField(label = "版本名", state = viewModel.versionName) }
                item { FormTextField(label = "开发者", state = viewModel.developer) }
                item { FormTextField(label = "应用来源", state = viewModel.source) }
                item { FormTextField(label = "关键字 (空格分隔)", state = viewModel.keyword) }
                
                item {
                    FormTextField(label = "应用描述", state = viewModel.describe, singleLine = false, minLines = 3)
                }
                item {
                    FormTextField(label = "更新日志", state = viewModel.updateLog, singleLine = false, minLines = 2)
                }
                item {
                    FormTextField(label = "给审核员的留言", state = viewModel.uploadMessage, singleLine = false, minLines = 2)
                }
                
                // 下拉选择: 应用类型, 版本类型, 标签, ABI (这里简化为文本输入或简单的Radio，实际可用Dropdown)
                // 为了演示，使用简单的文本输入或预设值
                item { Text("高级设置", style = MaterialTheme.typography.titleMedium) }
                // ... (可以添加更多Dropdown来选择类型，这里暂略，使用默认值)

                item {
                    Column {
                        Text("2. 添加应用截图 (发布时上传)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        BBQOutlinedButton(
                            onClick = { imageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            text = { Text("选择截图 (${viewModel.screenshotsUris.size})") }
                        )
                         if (viewModel.screenshotsUris.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.screenshotsUris.forEach { uri ->
                                    ImagePreviewItem(
                                        imageUrl = uri.toString(), // 本地URI
                                        onRemoveClick = { viewModel.removeScreenshot(uri) },
                                        onImageClick = { /* 预览逻辑 */ }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 提交按钮 ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BBQButton(
                    onClick = {
                        viewModel.releaseApp {
                            // 发布成功回调
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAnyTaskRunning,
                    text = { Text(if (isUpdateMode) "3. 确认更新" else "3. 确认发布") }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (isAnyTaskRunning) {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        BBQSnackbarHost(
            hostState = internalSnackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ... (保留 ApkUploadServiceDropdown, PaymentSettings, CategoryDropdown, FormTextField 等辅助组件) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApkUploadServiceDropdown(viewModel: AppReleaseViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val services = ApkUploadService.values()
    val selectedService by viewModel.selectedApkUploadService

    Column {
        Text("APK 上传服务", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedService.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("选择上传服务") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor( // 添加这行
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, // 使用正确的类型
                        enabled = true
                    )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                services.forEach { service ->
                    DropdownMenuItem(
                        text = { Text(service.displayName) },
                        onClick = {
                            viewModel.selectedApkUploadService.value = service
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentSettings(viewModel: AppReleaseViewModel) {
    Column {
        Text("付费设置", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = viewModel.isPay.value == 0, onClick = { viewModel.isPay.value = 0 })
            Text("免费")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = viewModel.isPay.value == 1, onClick = { viewModel.isPay.value = 1 })
            Text("付费")
        }
        if (viewModel.isPay.value == 1) {
            FormTextField(
                label = "价格 (整数)",
                state = viewModel.payMoney,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(viewModel: AppReleaseViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategoryName =
        viewModel.categories.getOrNull(viewModel.selectedCategoryIndex.value)?.categoryName ?: "请选择"

    Column {
        Text("应用分类", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCategoryName,
                onValueChange = {},
                readOnly = true,
                label = { Text("选择一个分类") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor( // 添加这行
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, // 使用正确的类型
                        enabled = true
                    )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                viewModel.categories.forEachIndexed { index, category ->
                    DropdownMenuItem(
                        text = { Text(category.categoryName) },
                        onClick = {
                            viewModel.selectedCategoryIndex.value = index
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FormTextField(
    label: String,
    state: MutableState<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = state.value,
        onValueChange = { state.value = it },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions
    )
}