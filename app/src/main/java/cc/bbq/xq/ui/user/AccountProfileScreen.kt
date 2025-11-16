//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.user

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import cc.bbq.xq.util.FileUtil // 修复：使用正确的 import 路径
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

@OptIn(ExperimentalMaterial3Api::class)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        uploadAvatar(context, uri, snackbarHostState,
                            onProgress = { message ->
                                showProgressDialog = true
                                progressMessage = message
                            },
                            onComplete = {
                                showProgressDialog = false
                            },
                            onError = { error ->
                                showProgressDialog = false
                                //Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                 coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = error,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    saveChanges(context, nickname, qqNumber, snackbarHostState) {
                        coroutineScope.launch {
                            deviceNameDataStore.saveDeviceName(deviceName)
                        }
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

@Composable
suspend fun uploadAvatar(
    context: Context,
    uri: Uri,
    snackbarHostState: SnackbarHostState,
    onProgress: (String) -> Unit = {},
    onComplete: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val credentials = AuthManager.getCredentials(context)
    val scope = rememberCoroutineScope()

    try {
        onProgress("上传头像中...")

        val realPath = FileUtil.getRealPathFromURI(context, uri)
        if (realPath == null) {
            onError("无法获取图片路径")
            return
        }

        val file = File(realPath) // 修复：明确使用 String 构造函数
        val bytes = file.readBytes()

        val appid = 1
        val token = credentials?.third ?: ""

        val uploadResult = KtorClient.ApiServiceImpl.uploadAvatar(
            appid = appid,
            token = token,
            file = bytes,
            filename = file.name
        )

        if (uploadResult.isSuccess) {
            val response = uploadResult.getOrNull()
            if (response?.code == 1) {
                withContext(Dispatchers.Main) {
                    //Toast.makeText(context, "头像上传成功", Toast.LENGTH_SHORT).show()
                }
                onComplete()
            } else {
                onError("头像上传失败: ${response?.msg ?: "未知错误"}")
            }
        } else {
            onError("头像上传失败: ${uploadResult.exceptionOrNull()?.message ?: "未知错误"}")
        }
    } catch (e: Exception) {
        onError("上传错误: ${e.message}")
    }
}

@Composable
fun saveChanges(context: Context, nickname: String, qqNumber: String, snackbarHostState: SnackbarHostState,  onDeviceNameSaved: () -> Unit) {
    val credentials = AuthManager.getCredentials(context)
    val token = credentials?.third ?: ""
    val scope = rememberCoroutineScope()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            if (nickname.isNotEmpty()) {
                val nicknameResponse = KtorClient.ApiServiceImpl.modifyUserInfo(
                    appid = 1,
                    token = token,
                    nickname = nickname,
                    qq = null
                )
                if (nicknameResponse.isSuccess){
                    withContext(Dispatchers.Main) {
                        if (nicknameResponse.getOrNull()?.code == 1) {
                           // Toast.makeText(context, "昵称修改成功", Toast.LENGTH_SHORT).show()
                           scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.nickname_changed),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                           // Toast.makeText(context, "昵称修改失败: ${nicknameResponse.getOrNull()?.msg}", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.nickname_change_failed,nicknameResponse.getOrNull()?.msg),
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
                    token = token,
                    nickname = null,
                    qq = qqNumber
                )
                if (qqResponse.isSuccess){
                    withContext(Dispatchers.Main) {
                        if (qqResponse.getOrNull()?.code == 1) {
                            //Toast.makeText(context, "QQ号修改成功", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.qq_changed),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                            //Toast.makeText(context, "QQ号修改失败: ${qqResponse.getOrNull()?.msg}", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message =context.getString(R.string.qq_change_failed,qqResponse.getOrNull()?.msg),
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
               // Toast.makeText(context, "保存修改失败: ${e.message}", Toast.LENGTH_SHORT).show()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.save_change_failed, e.message),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
}