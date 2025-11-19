//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.plaza

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import cc.bbq.xq.ui.ImagePreview
import androidx.compose.foundation.background
import androidx.navigation.NavController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import cc.bbq.xq.ui.theme.BBQSnackbarHost // 导入 BBQSnackbarHost
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.bbq.xq.ui.theme.BBQButton
import cc.bbq.xq.ui.theme.BBQOutlinedButton
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import cc.bbq.xq.ui.theme.ImagePreviewItem // 导入 ImagePreviewItem
import cc.bbq.xq.R
import kotlinx.coroutines.launch

@Composable
fun AppReleaseScreen(
    viewModel: AppReleaseViewModel,
    navController: NavController, // 添加 NavController 参数
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope() // 创建 CoroutineScope
    val MAX_INTRO_IMAGES = 3

    val processFeedback by viewModel.processFeedback.collectAsStateWithLifecycle()
    LaunchedEffect(processFeedback) {
        processFeedback?.let { result ->
            val message = result.fold(
                onSuccess = { it },
                onFailure = { it.message ?: "发生未知错误" }
            )
            val duration = if (result.isSuccess) SnackbarDuration.Short else SnackbarDuration.Long
            scope.launch {
                snackbarHostState.showSnackbar(message, duration = duration)
            }
            viewModel.clearProcessFeedback()
        }
    }

    val apkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.parseAndUploadApk(it)
        } ?: scope.launch {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.no_file_selected),
                duration = SnackbarDuration.Short
            )
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.uploadIntroductionImages(uris)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.no_image_selected),
                    duration = SnackbarDuration.Short
                )
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

            item { ApkUploadServiceDropdown(viewModel) }

            item {
                val selectedService by viewModel.selectedApkUploadService
                BBQButton(
                    onClick = { apkLauncher.launch("application/vnd.android.package-archive") },
                    modifier = Modifier.fillMaxWidth(),
                    text = {
                        Text(
                            if (isUpdateMode) "1. 选择新版 APK (上传至${selectedService.displayName})" else "1. 选择并上传 APK (至${selectedService.displayName})"
                        )
                    }
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
                            modifier = Modifier.size(64.dp)
                        ) {
                            val state = painter.state
                            if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                                CircularProgressIndicator()
                            } else {
                                SubcomposeAsyncImageContent()
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            if (iconUrl != null) "当前图标" else "已解析图标",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            item { FormTextField(label = "应用名称", state = viewModel.appName) }
            item {
                FormTextField(
                    label = "APK 下载链接 (自动填充/可修改)",
                    state = viewModel.apkDownloadUrl,
                    singleLine = true
                )
            }

            item {
                val uploadUrl = "https://file.bz6.top/upload.php"
                val annotatedString = buildAnnotatedString {
                    append("如果氪云API上传不稳定? 你或许可以尝试直接使用氪云的网页端上传")
                    pushStringAnnotation(tag = "URL", annotation = uploadUrl)
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("点此访问网页版上传，但是记住复制下载链接回来填充")
                    }
                    pop()
                }

                // 修复：使用新的 LinkAnnotation API
                androidx.compose.foundation.text.selection.SelectionContainer {
                    androidx.compose.material3.Text(
                        text = annotatedString,
                        modifier = Modifier
                            .padding(top = 4.dp, start = 4.dp)
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uploadUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    //Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.unable_to_open_link),
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                FormTextField(
                    label = "包名 (自动填充)",
                    state = viewModel.packageName,
                    enabled = !isUpdateMode
                )
            }
            item { FormTextField(label = "版本名 (自动填充)", state = viewModel.versionName, enabled = false) }
            item { FormTextField(label = "版本号 (用户可见)", state = viewModel.appVersion) }
            item { FormTextField(label = "文件大小 (MB, 自动填充)", state = viewModel.appSize, enabled = false) }
            item {
                FormTextField(
                    label = "资源介绍 (支持密码格式)",
                    state = viewModel.appIntroduce,
                    singleLine = false,
                    minLines = 3
                )
            }
            item {
                FormTextField(
                    label = "适配性能描述 (支持换行)",
                    state = viewModel.appExplain,
                    singleLine = false,
                    minLines = 4
                )
            }

            item {
    Column {
        Text("2. 上传应用介绍图 (至氪云)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        BBQOutlinedButton(
            onClick = { imageLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel.introductionImageUrls.size < MAX_INTRO_IMAGES,
            text = { Text("选择图片 (${viewModel.introductionImageUrls.size}/$MAX_INTRO_IMAGES)") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (viewModel.introductionImageUrls.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.introductionImageUrls.forEach { url ->
                    ImagePreviewItem(
                        imageUrl = url,
                        onRemoveClick = { viewModel.removeIntroductionImage(url) },
                        onImageClick = {
                            navController.navigate(
                                ImagePreview(
                                    imageUrl = url
                                ).createRoute()
                            )
                        }
                    )
                }
            }
        }
    }
}

            item { CategoryDropdown(viewModel) }
            item { PaymentSettings(viewModel) }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                BBQButton(
                    onClick = {
                        viewModel.releaseApp {
                            // 发布成功后不需要手动调用 onBack，因为 NavGraph 会处理
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when {
                                viewModel.isApkUploading.value -> "正在上传 APK..."
                                viewModel.isIconUploading.value -> "正在上传图标..."
                                viewModel.isIntroImagesUploading.value -> "正在上传介绍图..."
                                viewModel.isReleasing.value -> if (isUpdateMode) "正在提交更新..." else "正在提交发布..."
                                else -> "请稍候..."
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    // Snackbar 宿主
    Box(modifier = Modifier.fillMaxSize()) {
        BBQSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

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
                    .menuAnchor()
                    .fillMaxWidth()
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
                    .menuAnchor()
                    .fillMaxWidth()
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