//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.user

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import cc.bbq.xq.ui.theme.BBQSnackbarHost // 导入 BBQSnackbarHost
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import cc.bbq.xq.data.DeviceNameDataStore
import cc.bbq.xq.util.FileUtil
import coil.compose.rememberAsyncImagePainter
import com.github.dhaval2404.imagepicker.ImagePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.res.stringResource
import cc.bbq.xq.R

@Composable
fun AccountProfileScreen(modifier: Modifier = Modifier, snackbarHostState: SnackbarHostState) {
    var nickname by rememberSaveable { mutableStateOf("") }
    var qqNumber by rememberSaveable { mutableStateOf("") }
    var avatarUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showProgressDialog by rememberSaveable { mutableStateOf(false) }
    var progressMessage by rememberSaveable { mutableStateOf("") }
    var avatarUrl by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val deviceNameDataStore = remember { DeviceNameDataStore(context) }
    var deviceName by rememberSaveable { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        deviceName = deviceNameDataStore.deviceNameFlow.first()
    }

    Scaffold(
        snackbarHost = { BBQSnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("修改昵称") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = qqNumber,
                onValueChange = { qqNumber = it },
                label = { Text("修改QQ号") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("设备名称") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            AvatarUploadSection(
                avatarUri = avatarUri,
                avatarUrl = avatarUrl,
                onAvatarSelected = { uri ->
                    avatarUri = uri
                    coroutineScope.launch {
                        // 直接调用非Composable函数
                        uploadAvatar(
                            context = context,
                            uri = uri,
                            coroutineScope = coroutineScope,
                            snackbarHostState = snackbarHostState,
                            onProgress = { message ->
                                showProgressDialog = true
                                progressMessage = message
                            },
                            onComplete = {
                                showProgressDialog = false
                            },
                            onError = { error ->
                                showProgressDialog = false
                                snackbarHostState.showSnackbar(
                                    message = error,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        // 直接调用非Composable函数
                        saveChanges(
                            context = context,
                            nickname = nickname,
                            qqNumber = qqNumber,
                            coroutineScope = coroutineScope,
                            snackbarHostState = snackbarHostState,
                            onDeviceNameSaved = {
                                coroutineScope.launch {
                                    deviceNameDataStore.saveDeviceName(deviceName)
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存修改")
            }
        }

        if (showProgressDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("上传中") },
                text = { Text(progressMessage) },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun AvatarUploadSection(
    avatarUri: Uri?,
    avatarUrl: String,
    onAvatarSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null) {
                onAvatarSelected(uri)
            }
        }
    }

    val startImagePicker = {
        ImagePicker.with(context as Activity)
            .crop()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent ->
                launcher.launch(intent)
            }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (avatarUri != null) {
            Image(
                painter = rememberAsyncImagePainter(model = avatarUri),
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (avatarUrl.isNotEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(model = avatarUrl),
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = "选择头像",
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { startImagePicker() }) {
            Text("选择头像")
        }
    }
}

// 移除了 @Composable 注解，这是一个普通的挂起函数操作
suspend fun uploadAvatar(
    context: Context,
    uri: Uri,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onProgress: (String) -> Unit = {},
    onComplete: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val userCredentialsFlow = AuthManager.getCredentials(context)
    val userCredentials = userCredentialsFlow.first()
    val token = userCredentials?.token

    try {
        withContext(Dispatchers.Main) {
            onProgress("上传头像中...")
        }

        val realPath = FileUtil.getRealPathFromURI(context, uri)
        if (realPath == null) {
            withContext(Dispatchers.Main) {
                onError("无法获取图片路径")
            }
            return
        }

        val file = File(realPath)
        val bytes = file.readBytes()

        val appid = 1
        //val token = credentials?.third ?: ""

        val uploadResult = KtorClient.ApiServiceImpl.uploadAvatar(
            appid = appid,
            token = token!!,
            file = bytes,
            filename = file.name
        )

        if (uploadResult.isSuccess) {
            val response = uploadResult.getOrNull()
            if (response?.code == 1) {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } else {
                        withContext(Dispatchers.Main) {
                    onError("上传错误: ${e.message}")
                }
            }
        }
    }
}

// 移除了 @Composable 注解，这是一个普通的挂起函数操作
suspend fun saveChanges(
    context: Context,
    nickname: String,
    qqNumber: String,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onDeviceNameSaved: () -> Unit
) {
    val userCredentialsFlow = AuthManager.getCredentials(context)
    val userCredentials = userCredentialsFlow.first()
    val token = userCredentials?.token

    try {
        if (nickname.isNotEmpty()) {
            val nicknameResponse = KtorClient.ApiServiceImpl.modifyUserInfo(
                appid = 1,
                token = token!!,
                nickname = nickname,
                qq = null
            )
            if (nicknameResponse.isSuccess) {
                withContext(Dispatchers.Main) {
                    if (nicknameResponse.getOrNull()?.code == 1) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.nickname_changed),
                                duration = SnackbarDuration.Short
                            )
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(
                                    R.string.nickname_change_failed,
                                    nicknameResponse.getOrNull()?.msg ?: ""
                                ),
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            }
        }

        if (qqNumber.isNotEmpty()) {
            val qqResponse = KtorClient.ApiServiceImpl.modifyUserInfo(
                appid = 1,
                token = token!!,
                nickname = null,
                qq = qqNumber
            )
            if (qqResponse.isSuccess) {
                withContext(Dispatchers.Main) {
                    if (qqResponse.getOrNull()?.code == 1) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.qq_changed),
                                duration = SnackbarDuration.Short
                            )
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(
                                    R.string.qq_change_failed,
                                    qqResponse.getOrNull()?.msg ?: ""
                                ),
                                duration = SnackbarDuration.Short
                            ),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onDeviceNameSaved()
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.save_change_failed, e.message ?: ""),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
}