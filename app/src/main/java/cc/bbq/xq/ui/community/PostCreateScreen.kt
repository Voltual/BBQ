//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.community

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.bbq.xq.AuthManager
//import cc.bbq.xq.RetrofitClient
import cc.bbq.xq.data.DeviceNameDataStore
import cc.bbq.xq.data.PostDraftDataStore
import coil.compose.rememberAsyncImagePainter
import com.github.dhaval2404.imagepicker.ImagePicker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 从原 Activity 中提取的常量和数据类
private const val MODE_CREATE = "create"
private const val MODE_REFUND = "refund"

data class Subsection(val id: Int, val name: String)
val SUBSECTIONS = listOf(
    Subsection(4, "手机应用"),
    Subsection(5, "适配应用"),
    Subsection(9, "技巧攻略"),
    Subsection(10, "许愿求助"),
    Subsection(11, "分享生活"),
    Subsection(13, "校园生活"),
    Subsection(14, "学习园地")
)

data class RefundReason(val name: String)
val REFUND_REASONS = listOf(
    RefundReason("无法下载"),
    RefundReason("适配不良"),
    RefundReason("与软件描述不符"),
    RefundReason("资源无法使用"),
    RefundReason("违法违规"),
    RefundReason("适配反馈")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCreateScreen(
    viewModel: PostCreateViewModel,
    onBackClick: () -> Unit,
    // 修复：移除未使用的 onSubmitClick 参数
    mode: String,
    refundAppName: String,
    refundAppId: Long,
    refundVersionId: Long,
    refundPayMoney: Int,
    modifier: Modifier = Modifier
) {
    val isRefundMode = mode == MODE_REFUND
    val uiState by viewModel.uiState.collectAsState()
    val postStatus by viewModel.postStatus.collectAsState()

    // 本地 UI 状态
    var bvNumber by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var tempDeviceName by rememberSaveable { mutableStateOf("") }
    var manualImageUrls by rememberSaveable { mutableStateOf("") }
    var selectedRefundReason by rememberSaveable { mutableStateOf(REFUND_REASONS.first().name) }

    // 新增：DataStore
    val context = LocalContext.current
    val activity = context as? Activity
    val deviceNameDataStore = remember { DeviceNameDataStore(context) }
    val postDraftDataStore = remember { PostDraftDataStore(context) }
    val coroutineScope = rememberCoroutineScope()

    // 新增：是否显示恢复草稿对话框
    var showRestoreDraftDialog by rememberSaveable { mutableStateOf(false) }
    // 新增：自动恢复草稿
    var autoRestoreDraft by rememberSaveable { mutableStateOf(false) }
    // 新增：不存储草稿
    var doNotSaveDraft by rememberSaveable { mutableStateOf(false) }

    // 处理发帖状态
    LaunchedEffect(postStatus) {
        when (postStatus) {
            is PostStatus.Success -> {
                // 发帖成功，关闭界面
                onBackClick()
                viewModel.resetPostStatus()
            }
            is PostStatus.Error -> {
                // 错误已经在 ViewModel 中显示 Toast，这里只需要重置状态
                viewModel.resetPostStatus()
            }
            else -> {}
        }
    }

    // 在 Composable 首次进入时，根据模式初始化标题和设备名, 并判断是否显示恢复草稿弹窗
    LaunchedEffect(Unit) {
        if (isRefundMode) {
            viewModel.onTitleChange("$refundAppName  【应用退币申请】")
        }
        val storedDeviceName = deviceNameDataStore.deviceNameFlow.first()
        tempDeviceName = storedDeviceName

        // 获取 DataStore 中的选项
        val draft = postDraftDataStore.draftFlow.first()
        autoRestoreDraft = draft.autoRestoreDraft
        doNotSaveDraft = draft.doNotSaveDraft

        // 如果有草稿且未设置自动恢复，则显示弹窗
        if (draft.hasDraft && !autoRestoreDraft) {
            showRestoreDraftDialog = true
        } else if (draft.hasDraft && autoRestoreDraft) {
            viewModel.restoreDraft()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                if (uiState.selectedImageUris.size < 2) {
                    viewModel.uploadImage(uri)
                } else {
                    android.widget.Toast.makeText(context, "最多只能上传两张图片", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val startImagePicker = {
        activity?.let {
            ImagePicker.with(it)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .createIntent { intent -> imagePickerLauncher.launch(intent) }
        }
    }

    if (uiState.showProgressDialog) {
        AlertDialog(
            onDismissRequest = { /* 不允许取消 */ },
            title = { Text("上传中") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(uiState.progressMessage)
                }
            },
            confirmButton = {}
        )
    }

    // 加载状态对话框
    if (postStatus is PostStatus.Loading) {
        AlertDialog(
            onDismissRequest = { /* 不允许取消 */ },
            title = { Text("发帖中") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("正在发布帖子...")
                }
            },
            confirmButton = {}
        )
    }

    // 新增：恢复草稿对话框
    if (showRestoreDraftDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDraftDialog = false },
            title = { Text("恢复草稿？") },
            text = {
                Text("是否恢复上次未发布的草稿？")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDraftDialog = false
                    viewModel.restoreDraft()
                }) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreDraftDialog = false
                    viewModel.clearDraft()
                }) {
                    Text("放弃")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val selectedTopicName = if (isRefundMode) {
            selectedRefundReason
        } else {
            SUBSECTIONS.find { it.id == uiState.selectedSubsectionId }?.name ?: ""
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                // 修复：使用新的 menuAnchor 重载方法
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                readOnly = true,
                value = selectedTopicName,
                onValueChange = {},
                label = { Text(if (isRefundMode) "问题类型" else "选择话题") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (isRefundMode) {
                    REFUND_REASONS.forEach { reason ->
                        DropdownMenuItem(
                            text = { Text(reason.name) },
                            onClick = {
                                selectedRefundReason = reason.name
                                expanded = false
                            }
                        )
                    }
                } else {
                    SUBSECTIONS.forEach { subsection ->
                        DropdownMenuItem(
                            text = { Text(subsection.name) },
                            onClick = {
                                viewModel.onSubsectionChange(subsection.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.title,
            onValueChange = { if (!isRefundMode) viewModel.onTitleChange(it) },
            label = { Text("标题") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = isRefundMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.content,
            onValueChange = { viewModel.onContentChange(it) },
            label = { Text(if (isRefundMode) "详细描述问题 (12字以上)" else "内容") },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            maxLines = 10
        )

        Spacer(modifier = Modifier.height(16.dp))

        ImageUploadSection(
            uris = uiState.selectedImageUris,
            onAddClick = { startImagePicker() },
            onRemoveClick = { uri -> viewModel.removeImage(uri) },
            autoRestoreDraft = autoRestoreDraft,
            onAutoRestoreDraftChange = {
                coroutineScope.launch {
                    postDraftDataStore.setAutoRestoreDraft(it)
                    autoRestoreDraft = it
                }
            },
            doNotSaveDraft = doNotSaveDraft,
            onDoNotSaveDraftChange = {
                coroutineScope.launch {
                    postDraftDataStore.setDoNotSaveDraft(it)
                    doNotSaveDraft = it
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = bvNumber,
            onValueChange = { bvNumber = it },
            label = { Text("BV号 (可选)") },
            placeholder = { Text("例如: BV1RohqzoEsy") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tempDeviceName,
            onValueChange = { tempDeviceName = it },
            label = { Text("设备名称 (临时修改)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = manualImageUrls,
            onValueChange = { manualImageUrls = it },
            label = { Text("或手动输入图片链接 (多个用逗号隔开)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (uiState.title.isBlank()) {
                    android.widget.Toast.makeText(context, "请填写标题", android.widget.Toast.LENGTH_SHORT).show()
                } else if (uiState.content.isBlank()) {
                    val message = if (isRefundMode) "请详细描述问题" else "请填写内容"
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                } else if (isRefundMode && uiState.content.length < 12) {
                    android.widget.Toast.makeText(context, "问题描述不能少于12个字", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    // 合并图片URL
                    val uploadedUrlsList = uiState.imageUrls.split(",").filter { it.isNotBlank() }
                    val manualUrlsList = manualImageUrls.split(",").filter { it.isNotBlank() }
                    val allImageUrls = (uploadedUrlsList + manualUrlsList).distinct().joinToString(",")

                    // 调用 ViewModel 的发帖方法
                    viewModel.createPost(
                        title = uiState.title,
//                        content = uiState.content,
                        imageUrls = allImageUrls,
                        subsectionId = uiState.selectedSubsectionId,
                        bvNumber = bvNumber,
                        tempDeviceName = tempDeviceName,
                        mode = mode,
                        refundAppId = refundAppId,
                        refundVersionId = refundVersionId,
                        refundPayMoney = refundPayMoney,
                        selectedRefundReason = selectedRefundReason
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = postStatus !is PostStatus.Loading
        ) {
            Text(if (isRefundMode) "提交申请" else "发布帖子")
        }
    }
}

@Composable
private fun ImageUploadSection(
    uris: List<Uri>,
    onAddClick: () -> Unit,
    onRemoveClick: (Uri) -> Unit,
    autoRestoreDraft: Boolean,
    onAutoRestoreDraftChange: (Boolean) -> Unit,
    doNotSaveDraft: Boolean,
    onDoNotSaveDraftChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("图片上传 (最多2张)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uris) { uri ->
                Box(modifier = Modifier.size(80.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = "预览图片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                    )
                    IconButton(
                        onClick = { onRemoveClick(uri) },
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "删除", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (uris.size < 2) {
                item {
                    OutlinedButton(onClick = onAddClick, modifier = Modifier.size(80.dp)) {
                        Icon(Icons.Default.Add, "添加图片")
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = autoRestoreDraft,
                onCheckedChange = onAutoRestoreDraftChange
            )
            Text("自动恢复草稿（不询问）")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = doNotSaveDraft,
                onCheckedChange = onDoNotSaveDraftChange
            )
            Text("不存储草稿")
        }
    }
}