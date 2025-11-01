//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.community

import android.app.Application
import io.ktor.client.request.post // 添加这个导入
import io.ktor.client.request.setBody // 添加这个导入
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import cc.bbq.xq.data.db.PostDraftRepository
import cc.bbq.xq.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import io.ktor.client.plugins.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.http.content.*
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.HttpResponse
import cc.bbq.xq.data.PostDraftDataStore

class PostCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val draftRepository = PostDraftRepository()
    private val postDraftDataStore = PostDraftDataStore(application)

    private val _uiState = MutableStateFlow(PostCreateUiState())
    val uiState: StateFlow<PostCreateUiState> = _uiState.asStateFlow()

    // 新增：发帖状态
    private val _postStatus = MutableStateFlow<PostStatus>(PostStatus.Idle)
    val postStatus: StateFlow<PostStatus> = _postStatus.asStateFlow()

    init {
        // 修改：不再自动恢复草稿
        // observeAndAutoSave() // 移动到 restoreDraft 方法中
    }

    // --- Event Handlers ---
    fun onTitleChange(newTitle: String) { _uiState.update { it.copy(title = newTitle) } }
    fun onContentChange(newContent: String) { _uiState.update { it.copy(content = newContent) } }
    fun onSubsectionChange(newId: Int) { _uiState.update { it.copy(selectedSubsectionId = newId) } }
    fun onImageUrlsChange(urls: String) { _uiState.update { it.copy(imageUrls = urls) } }

    // 新增：恢复草稿方法
    fun restoreDraft() {
        viewModelScope.launch {
            draftRepository.draftFlow.first()?.let { draft ->
                _uiState.update { it.copy(
                    title = draft.title,
                    content = draft.content,
                    selectedImageUris = draft.imageUris,
                    imageUrls = draft.imageUrls,
                    selectedSubsectionId = draft.subsectionId,
                    imageUriToUrlMap = draft.imageUris.zip(draft.imageUrls.split(",").filter { s -> s.isNotEmpty() }).toMap()
                )}
            }
            observeAndAutoSave() // 只有在恢复草稿后才开始自动保存
        }
    }

    fun uploadImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(showProgressDialog = true, progressMessage = "上传图片中...") }
            
            val realPath = withContext(Dispatchers.IO) {
                FileUtil.getRealPathFromURI(getApplication(), uri)
            }

            if (realPath == null) {
                _uiState.update { it.copy(showProgressDialog = false) }
                Toast.makeText(getApplication(), "无法获取图片路径", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                val file = File(realPath)
                val bytes = file.readBytes() // 将文件读取为字节数组
                val uploadResult = uploadImageKtor(bytes, file.name)

                if (uploadResult.isSuccess) {
                    uploadResult.getOrNull()?.let {
                        _uiState.update { currentState ->
                            val newUris = currentState.selectedImageUris + uri
                            val newUrlMap = currentState.imageUriToUrlMap + (uri to it)
                            val newUrls = newUrlMap.values.joinToString(",")
                            currentState.copy(
                                selectedImageUris = newUris,
                                imageUrls = newUrls,
                                imageUriToUrlMap = newUrlMap
                            )
                        }
                        Toast.makeText(getApplication(), "图片上传成功", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(getApplication(), "上传失败: ${uploadResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "上传错误: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _uiState.update { it.copy(showProgressDialog = false) }
            }
        }
    }

    private suspend fun uploadImageKtor(fileBytes: ByteArray, fileName: String): Result<String> {
    return withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = KtorClient.uploadHttpClient.post("api.php") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", fileBytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg") // 根据实际文件类型设置
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            })
                        }
                    )
                )
            }

            val responseBody = response.bodyAsText()
            // 使用 Ktor 客户端的响应体获取图片 URL
            val imageUrl = responseBody.substringAfter("\"viewurl\":\"").substringBefore("\"").trim()
            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}



    fun removeImage(uri: Uri) {
        _uiState.update { currentState ->
            val newUris = currentState.selectedImageUris - uri
            val newUrlMap = currentState.imageUriToUrlMap - uri
            val newUrls = newUrlMap.values.joinToString(",")
            currentState.copy(
                selectedImageUris = newUris,
                imageUrls = newUrls,
                imageUriToUrlMap = newUrlMap
            )
        }
    }

    // 新增：发帖方法
    fun createPost(
        title: String,
        // 修复：移除未使用的 content 参数，使用 _uiState.value.content
        imageUrls: String,
        subsectionId: Int,
        bvNumber: String,
        tempDeviceName: String,
        mode: String,
        // 修复：移除未使用的 refundAppName 参数
        refundAppId: Long = 0L,
        refundVersionId: Long = 0L,
        refundPayMoney: Int = 0,
        selectedRefundReason: String = ""
    ) {
        viewModelScope.launch {
            _postStatus.value = PostStatus.Loading
            
            try {
                val credentials = AuthManager.getCredentials(getApplication())
                if (credentials == null) {
                    _postStatus.value = PostStatus.Error("请先登录")
                    return@launch
                }

                val token = credentials.third
                
                // 构建最终内容
                val finalContent = if (mode == "refund") {
                    val videoPart = if (bvNumber.isNotBlank()) "【视频：$bvNumber】" else ""
                    """
                    ${_uiState.value.content}


                    问题类型:$selectedRefundReason
                    系统定制商：${android.os.Build.BRAND.uppercase()}
                    设备型号：${android.os.Build.MODEL}
                    退还金额:$refundPayMoney
                    资源id:$refundAppId
                    资源版本:$refundVersionId 機型：$tempDeviceName｜$videoPart
                    """.trimIndent()
                } else {
                    val videoPart = if (bvNumber.isNotBlank()) "【视频：$bvNumber】" else ""
                    "${_uiState.value.content} 機型：$tempDeviceName｜$videoPart"
                }

                val finalSubsectionId = if (mode == "refund") 21 else subsectionId

                val createPostResult = KtorClient.ApiServiceImpl.createPost(
                    appid = 1,
                    token = token,  // 这里使用参数名 token，而不是 usertoken
                    title = title,
                    content = finalContent,
                    sectionId = finalSubsectionId,
                    imageUrls = if (imageUrls.isNotBlank()) imageUrls else null
                )

                if (createPostResult.isSuccess) {
                    _postStatus.value = PostStatus.Success
                    Toast.makeText(getApplication(), "发帖成功", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMsg = createPostResult.exceptionOrNull()?.message ?: "发帖失败"
                    _postStatus.value = PostStatus.Error(errorMsg)
                    Toast.makeText(getApplication(), "发帖失败: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val errorMsg = "网络错误: ${e.message}"
                _postStatus.value = PostStatus.Error(errorMsg)
                Toast.makeText(getApplication(), errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearDraft() {
        viewModelScope.launch {
            draftRepository.clearDraft()
        }
    }

    // 新增：重置发帖状态
    fun resetPostStatus() {
        _postStatus.value = PostStatus.Idle
    }

    // 修复：添加 FlowPreview 注解
    @OptIn(FlowPreview::class)
    private fun observeAndAutoSave() {
        postDraftDataStore.draftFlow.map { it.doNotSaveDraft }.distinctUntilChanged().onEach { doNotSave ->
            if (doNotSave) return@onEach // 如果用户选择不保存草稿，则直接返回
            
            _uiState
                .debounce(1000)
                .onEach { state ->
                    if (state.title.isNotBlank() || state.content.isNotBlank() || state.selectedImageUris.isNotEmpty()) {
                        draftRepository.saveDraft(
                            PostDraftRepository.DraftDto(
                                title = state.title,
                                content = state.content,
                                imageUris = state.selectedImageUris,
                                imageUrls = state.imageUrls,
                                subsectionId = state.selectedSubsectionId
                            )
                        )
                    }
                }
                .launchIn(viewModelScope)
        }.launchIn(viewModelScope)
    }
}

data class PostCreateUiState(
    val title: String = "",
    val content: String = "",
    val selectedSubsectionId: Int = 11,
    val selectedImageUris: List<Uri> = emptyList(),
    val imageUrls: String = "",
    val imageUriToUrlMap: Map<Uri, String> = emptyMap(),
    val showProgressDialog: Boolean = false,
    val progressMessage: String = ""
)

// 新增：发帖状态密封类
sealed class PostStatus {
    object Idle : PostStatus()
    object Loading : PostStatus()
    object Success : PostStatus()
    data class Error(val message: String) : PostStatus()
}