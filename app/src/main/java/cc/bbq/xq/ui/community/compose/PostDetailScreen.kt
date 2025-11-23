//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.community.compose

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import cc.bbq.xq.KtorClient
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import io.ktor.client.call.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedVisibility
import cc.bbq.xq.ui.animation.materialSharedAxisYIn
import cc.bbq.xq.ui.animation.materialSharedAxisYOut
import cc.bbq.xq.ui.animation.rememberSlideDistance
import cc.bbq.xq.ui.theme.BBQSnackbarHost // 导入 BBQSnackbarHost
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cc.bbq.xq.AuthManager
import cc.bbq.xq.ui.*
import cc.bbq.xq.ui.community.PostDetailViewModel
import cc.bbq.xq.ui.compose.LinkifyText
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.github.dhaval2404.imagepicker.ImagePicker
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import cc.bbq.xq.ui.theme.SwitchWithText // 导入移动到公共位置的 SwitchWithText
import androidx.compose.ui.res.stringResource
import cc.bbq.xq.R

@Composable
fun PostDetailScreen(
    postId: Long,
    navController: NavController,
    onPostDeleted: () -> Unit,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState, //fixed: mark as unused
    modifier: Modifier = Modifier
) {
    val viewModel: PostDetailViewModel = viewModel()
    val context = LocalContext.current

    val postDetail by viewModel.postDetail.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val likeCount by viewModel.likeCount.collectAsState()
    val commentCount by viewModel.commentCount.collectAsState()
    val showCommentDialog by viewModel.showCommentDialog.collectAsState()
    val showReplyDialog by viewModel.showReplyDialog.collectAsState()
    val currentReplyComment by viewModel.currentReplyComment.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoadingComments by viewModel.isLoadingComments.collectAsState()
    val hasMoreComments by viewModel.hasMoreComments.collectAsState()

    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    
    val internalSnackbarHostState = remember { SnackbarHostState() } // fixed: rename to internalSnackbarHostState
    val coroutineScope = rememberCoroutineScope()

    var showShareDialog by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem?.let {
                it.index >= totalItems - 3 && !isLoadingComments && hasMoreComments
            } ?: false
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadNextPageComments(postId)
        }
    }

    val deleteSuccess by viewModel.deleteSuccess.collectAsState()
    if (deleteSuccess) {
        LaunchedEffect(Unit) {
            onPostDeleted()
        }
    }

   

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            coroutineScope.launch {
                internalSnackbarHostState.showSnackbar(errorMessage) //fixed: use internalSnackbarHostState
                viewModel.clearErrorMessage()
            }
        }
    }

    LaunchedEffect(postId) {
        viewModel.loadPostDetail(postId)
        viewModel.loadComments(postId)
    }

    val slideDistance = rememberSlideDistance()
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = listState
        ) {
            // 使用 AnimatedVisibility 包裹帖子内容卡片
            item {
                AnimatedVisibility(
                    visible = postDetail != null,
                    enter = materialSharedAxisYIn(forward = true, slideDistance = slideDistance),
                    exit = materialSharedAxisYOut(forward = true, slideDistance = slideDistance)
                ) {
                    postDetail?.let { detail ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            // 卡片内容保持不变
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    AsyncImage(
                                        model = detail.usertx,
                                        contentDescription = "头像",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                detail.userid.let { userId ->
                                                    navController.navigate(UserDetail(userId).createRoute())
                                                }
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = detail.nickname,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${detail.ip_address} · ${detail.create_time_ago}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Spacer(Modifier.weight(1f))
                                    Box {
                                        IconButton(
                                            onClick = { showMoreOptions = true }
                                        ) {
                                            Icon(Icons.Default.MoreVert, "更多")
                                        }
                                        
                                        DropdownMenu(
                                            expanded = showMoreOptions,
                                            onDismissRequest = { showMoreOptions = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("分享") },
                                                onClick = {
                                                    showMoreOptions = false
                                                    showShareDialog = true
                                                }
                                            )
                                            // 添加“刷新评论”菜单项
                                            DropdownMenuItem(
                                                text = { Text("刷新评论") },
                                                onClick = {
                                                    showMoreOptions = false
                                                    viewModel.refreshComments(postId)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("打赏") },
                                                onClick = {
                                                    showMoreOptions = false
                                                    val destination = PaymentForPost(
                                                        postId = detail.id,
                                                        postTitle = detail.title,
                                                        previewContent = detail.content.take(30),
                                                        authorName = detail.nickname,
                                                        authorAvatar = detail.usertx,
                                                        postTime = detail.create_time_ago
                                                    )
                                                    navController.navigate(destination.createRoute())
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("删除帖子") },
                                                onClick = {
                                                    showMoreOptions = false
                                                    viewModel.deletePost()
                                                }
                                            )
                                        }
                                    }
                                }

                                SelectionContainer {
                                    Text(
                                        text = detail.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))

                                LinkifyText(
                                    text = detail.content.replace("<br>", "\n"),
                                    navController = navController
                                )

                                // 使用安全调用符 ?. 和 let 函数来处理 img_url 可能为空的情况
                                postDetail?.img_url?.let { imgUrls ->
                                    imgUrls.forEach { imageUrl ->
                                        Spacer(Modifier.height(16.dp))
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = "帖子图片",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable {
                                                    // 导航到图片预览
                                                    navController.navigate(
                                                        ImagePreview(
                                                            imageUrl = imageUrl
                                                        ).createRoute()
                                                    )
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "浏览: ${detail.view}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { viewModel.toggleLike() }
                                    ) {
                                        Icon(
                                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "点赞",
                                            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "点赞: $likeCount",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "评论: $commentCount",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "评论 ($commentCount)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            itemsIndexed(comments) { _, comment ->
                CommentItem(
                    comment = comment,
                    navController = navController,
                    onReply = { viewModel.openReplyDialog(comment) },
                    onDelete = { viewModel.deleteComment(comment.id) },
                    clipboardManager = clipboardManager,
                    context = context,
                    snackbarHostState = internalSnackbarHostState //fixed: use internalSnackbarHostState
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoadingComments -> CircularProgressIndicator()
                        !hasMoreComments -> Text(
                            "没有更多评论了",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> Text(
                            "上拉加载更多",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 评论浮动按钮
        FloatingActionButton(
            onClick = { viewModel.openCommentDialog() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Comment, "评论")
        }
    }

    // 分享对话框
    if (showShareDialog) {
        val shareText = "http://apk.xiaoqu.online/post/${postDetail?.id}.html"
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("分享帖子") },
            text = { Text("复制链接: $shareText") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipData = ClipData.newPlainText("分享链接", shareText)
                        clipboardManager.setPrimaryClip(clipData)
                         coroutineScope.launch {
                                internalSnackbarHostState.showSnackbar( //fixed: use internalSnackbarHostState
                                    message = context.getString(R.string.copied_link),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        showShareDialog = false
                    }
                ) {
                    Text("复制链接")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) { Text("取消") }
            }
        )
    }

    if (showCommentDialog) {
        CommentDialog(
            hint = "输入评论内容...",
            onDismiss = { viewModel.closeCommentDialog() },
            context = context,
            onSubmit = { content, imageUrl ->
                viewModel.submitComment(content, imageUrl)
            }
        )
    }
    if (showReplyDialog && currentReplyComment != null) {
        CommentDialog(
            hint = "回复 @${currentReplyComment?.nickname}",
            onDismiss = { viewModel.closeReplyDialog() },
            context = context,
            onSubmit = { content, imageUrl ->
                viewModel.submitComment(content, imageUrl)
            }
        )
    }

    // Snackbar 宿主
    Box(modifier = Modifier.fillMaxSize()) {
        BBQSnackbarHost(
            hostState = internalSnackbarHostState, //fixed: use internalSnackbarHostState
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun CommentDialog(
    hint: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String?) -> Unit,
    context: Context
) {
    var commentText by remember { mutableStateOf("") }
    var includeImage by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showProgressDialog by remember { mutableStateOf(false) }
    // 新增：发送状态
    var isSubmitting by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    fun uploadImage(uri: Uri, onSuccess: (String) -> Unit) {
    showProgressDialog = true
    progressMessage = "上传图片中..."

    coroutineScope.launch(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File.createTempFile("upload_", ".jpg", context.cacheDir).apply {
                outputStream().use { output ->
                    inputStream?.copyTo(output)
                }
            }

            val response = KtorClient.uploadHttpClient.post("api.php") {
                setBody(MultiPartFormDataContent(
                    formData {
                        append("file", file.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "image/*")
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        })
                    }
                ))
            }

            withContext(Dispatchers.Main) {
                showProgressDialog = false
                if (response.status.isSuccess()) {
                    val responseBody: KtorClient.UploadResponse = response.body()
                    // 修复：移除不必要的 null 检查，因为 response.body() 返回非空类型
                    if ((responseBody.code == 1 || responseBody.code == 0) && !responseBody.downurl.isNullOrBlank()) {
                        onSuccess(responseBody.downurl)
                       // Toast.makeText(context, "图片上传成功", Toast.LENGTH_SHORT).show()
                    } else {
                        // 修复：直接访问 msg，因为它是非空类型
                       // Toast.makeText(context, "上传失败: ${responseBody.msg}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                   // Toast.makeText(context, "上传失败: 网络错误 ${response.status}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showProgressDialog = false
                //Toast.makeText(context, "上传错误: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                selectedImageUri = uri
                uploadImage(uri) { url ->
                    imageUrl = url
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    maxLines = 4,
                    placeholder = { Text("输入评论内容...") }
                )

                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SwitchWithText(
                        text = "添加图片",
                        checked = includeImage,
                        onCheckedChange = { includeImage = it },
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    if (includeImage) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            selectedImageUri?.let { uri ->
                                Image(
                                    painter = rememberAsyncImagePainter(model = uri),
                                    contentDescription = "预览图片",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } ?: run {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("无图片", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Button(
                                    onClick = {
                                        ImagePicker.with(context as Activity)
                                            .crop()
                                            .compress(1024)
                                            .maxResultSize(1080, 1080)
                                            .createIntent { intent ->
                                                imagePickerLauncher.launch(intent)
                                            }
                                    },
                                    modifier = Modifier.width(120.dp)
                                ) {
                                    Text("选择图片")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (selectedImageUri != null) {
                                    TextButton(
                                        onClick = {
                                            selectedImageUri = null
                                            imageUrl = null
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("移除图片")
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // 提交前设置发送状态为 true
                            isSubmitting = true
                            onSubmit(commentText, if (includeImage) imageUrl else null)
                        },
                        // 根据发送状态禁用按钮
                        enabled = commentText.isNotEmpty() && !isSubmitting
                    ) {
                        // 根据发送状态显示不同的文本
                        Text(if (isSubmitting) "发送中..." else "提交")
                    }
                }
            }
        }
    }

    if (showProgressDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("上传中") },
            text = { Text(progressMessage) },
            confirmButton = { }
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 监听 ViewModel 的 errorMessage，发送失败后重置发送状态
    val viewModel: PostDetailViewModel = viewModel()
    val errorMessage by viewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            isSubmitting = false
        }
    }
}

@Composable
fun CommentItem(
    comment: KtorClient.Comment,
    navController: NavController,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    clipboardManager: ClipboardManager,
    context: Context,
    snackbarHostState: SnackbarHostState // 添加 snackbarHostState 参数
) {
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 获取当前用户 ID 的 Flow
    val currentUserIdFlow = AuthManager.getUserId(context)
    // 使用 collectAsState() 观察当前用户 ID
    val currentUserId by currentUserIdFlow.collectAsState(initial = null)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除评论") },
            text = { Text("确定要删除这条评论吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        val clipData = ClipData.newPlainText("评论内容", comment.content)
                        clipboardManager.setPrimaryClip(clipData)
                        scope.launch {
                            snackbarHostState.showSnackbar( // fixed: snackbarHostState is used here
                                message = context.getString(R.string.comment_copied),
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    onLongPress = {
                        showDeleteDialog = true
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = comment.usertx,
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { navController.navigate(UserDetail(comment.userid).createRoute()) },
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = comment.nickname.ifEmpty { comment.username },
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = comment.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))
            LinkifyText(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                navController = navController
            )

            comment.image_path?.firstOrNull()?.takeIf { it.isNotEmpty() }?.let { imageUrl ->
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "评论图片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { 
                            navController.navigate(
                                ImagePreview(
                                    imageUrl = imageUrl
                                ).createRoute()
                            )
                         },
                    contentScale = ContentScale.Crop
                )
            }

            if (!comment.parentnickname.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "回复 @${comment.parentnickname}:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                LinkifyText(
                    text = comment.parentcontent ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    navController = navController
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReply) {
                    Text("回复")
                }

                Spacer(Modifier.width(8.dp))

                // 使用 currentUserId，而不是从 AuthManager 获取凭证
                if (comment.userid == currentUserId) {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("删除")
                    }
                }
            }
        }
    }
}