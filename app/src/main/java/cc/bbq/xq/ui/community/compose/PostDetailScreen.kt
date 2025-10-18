//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.community.compose

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import cc.bbq.xq.RetrofitClient
import cc.bbq.xq.KtorClient // 导入 KtorClient
import cc.bbq.xq.ui.*
import cc.bbq.xq.ui.community.PostDetailViewModel
import cc.bbq.xq.ui.compose.LinkifyText
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.github.dhaval2404.imagepicker.ImagePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Composable
fun SwitchWithText(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PostDetailScreen(
    postId: Long,
    navController: NavController,
    onBack: () -> Unit,
    onPostDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PostDetailViewModel = viewModel()
    val context = LocalContext.current

    //val postDetail by viewModel.postDetail.collectAsState()
    val postDetail by viewModel.postDetail.collectAsState()
    //val comments by viewModel.comments.collectAsState()
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

    var showShareDialog by remember { mutableStateOf(false) }
    // 修复：将 showMoreOptions 状态移到 PostDetailScreen 内部 
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

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(errorMessage)
                viewModel.clearErrorMessage()
            }
        }
    }

    LaunchedEffect(postId) {
        viewModel.loadPostDetail(postId)
        viewModel.loadComments(postId)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background), // 设置整体背景色
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = listState
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant, // 使用 surfaceVariant 作为卡片背景
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            AsyncImage(
                                model = postDetail?.usertx ?: "",
                                contentDescription = "头像",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        postDetail?.userid?.let { userId ->
                                            navController.navigate(UserDetail(userId).createRoute())
                                        }
                                    },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = postDetail?.nickname ?: "加载中...",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${postDetail?.ip_address ?: ""} · ${postDetail?.create_time_ago ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // 修复：将下拉菜单状态移到 PostDetailScreen 内部，并正确集成
                            Spacer(Modifier.weight(1f))
                            Box {
                                IconButton(
                                    onClick = { showMoreOptions = true }
                                ) {
                                    Icon(Icons.Default.MoreVert, "更多")
                                }
                                
                                // 下拉菜单直接与按钮关联
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
                                    DropdownMenuItem(
                                        text = { Text("打赏") },
                                        onClick = {
                                            showMoreOptions = false
                                            postDetail?.let {
                                                val destination = PaymentForPost(
                                                    postId = it.id,
                                                    postTitle = it.title,
                                                    previewContent = it.content?.take(30) ?: "",
                                                    authorName = it.nickname,
                                                    authorAvatar = it.usertx,
                                                    postTime = it.create_time_ago
                                                )
                                                navController.navigate(destination.createRoute())
                                            }
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
                                text = postDetail?.title ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        LinkifyText(
                            text = postDetail?.content?.replace("<br>", "\n") ?: "",
                            navController = navController
                        )

                        postDetail?.img_url?.forEach { imageUrl ->
                            Spacer(Modifier.height(16.dp))
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "帖子图片",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        navController.navigate(ImagePreview(imageUrl).createRoute())
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "浏览: ${postDetail?.view ?: 0}",
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
                    context = context
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
                        showShareDialog = false
                        Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// CommentDialog 和 CommentItem 函数保持不变...
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

                val requestFile = MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull())
                )

                val response = RetrofitClient.uploadInstance.uploadImage(requestFile)

                withContext(Dispatchers.Main) {
                    showProgressDialog = false
                    if (response.isSuccessful && (response.body()?.code == 0 || response.body()?.code == 1)) {
                        response.body()?.downurl?.let { url ->
                            onSuccess(url)
                            Toast.makeText(context, "图片上传成功", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "上传失败: ${response.body()?.msg}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showProgressDialog = false
                    Toast.makeText(context, "上传错误: ${e.message}", Toast.LENGTH_SHORT).show()
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
                containerColor = MaterialTheme.colorScheme.surfaceVariant, // 对话框也使用 surfaceVariant
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
                            onSubmit(commentText, if (includeImage) imageUrl else null)
                    },
                    enabled = commentText.isNotEmpty()
                ) {
                    Text("提交")
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
}

@Composable
fun CommentItem(
    //comment: RetrofitClient.models.Comment,
    comment: KtorClient.Comment,
    navController: NavController,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    clipboardManager: ClipboardManager,
    context: Context
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                        Toast.makeText(context, "评论已复制", Toast.LENGTH_SHORT).show()
                    },
                    onLongPress = {
                        showDeleteDialog = true
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant, // 评论卡片使用 surfaceVariant
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
                        .clickable { navController.navigate(ImagePreview(imageUrl).createRoute()) },
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

                val credentials = AuthManager.getCredentials(context)
                val currentUserId = credentials?.fourth
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
}