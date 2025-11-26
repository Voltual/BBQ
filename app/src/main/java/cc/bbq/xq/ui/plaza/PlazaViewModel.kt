//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.plaza

import android.app.Application
import android.content.Context
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.*
import android.util.Log
import cc.bbq.xq.AppStore
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import cc.bbq.xq.SineShopClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch

// ==================== 内部类 - 数据模型和ViewModel ====================

// ==================== 数据模型 ====================
data class AppItem(
    val id: String,
    val name: String,
    val iconUrl: String,
    val versionId: Long
) {
    val uniqueId: String get() = "$id-$versionId-${System.nanoTime()}"
}

data class PlazaData(val popularApps: List<AppItem>)

// ==================== ViewModel ====================
private val Context.dataStore by preferencesDataStore(name = "plaza_preferences")

class PlazaViewModel(
    application: Application,
    initialMode: Boolean = false
) : AndroidViewModel(application) {
    private val repository = PlazaRepository(application.applicationContext)
    private val _plazaData = MutableLiveData(PlazaData(emptyList()))
    private val _searchResults = MutableLiveData(emptyList<AppItem>())
    private val _errorMessage = MutableLiveData("")
    private val _isLoading = MutableLiveData(false)
    private val _appStore = MutableLiveData(AppStore.XIAOQU_SPACE) // 默认商店
    private val _appTagList = MutableLiveData<List<SineShopClient.AppTag>>(emptyList()) // 弦应用商店标签

    // 模式管理
    private var currentMode = initialMode
    private var hasLoadedInitialData = false

    // 分页状态
    private var popularAppsPage = 1
    private var popularAppsTotalPages = 1
    private var searchPage = 1
    private var searchTotalPages = 1
    private var currentQuery = ""
    private var currentCategoryId: Int? = null
    private var currentSubCategoryId: Int? = null
    private var currentUserId: Long? = null

    // DataStore keys
    private val AUTO_SCROLL_MODE_KEY = booleanPreferencesKey("auto_scroll_mode")

    // 状态跟踪 - 类似 UserDetailViewModel
    private var _isInitialized = false
    private var _currentModeState: Boolean = initialMode
    private var _currentUserIdState: Long? = null

    // Public autoScrollMode LiveData
    private val _autoScrollMode = MutableLiveData<Boolean>()
    val autoScrollMode: LiveData<Boolean> = _autoScrollMode

    // 公开的LiveData
    val currentPage = MutableLiveData(1)
    val totalPages = MutableLiveData(1)
    val plazaData: LiveData<PlazaData> = _plazaData
    val searchResults: LiveData<List<AppItem>> = _searchResults
    val errorMessage: LiveData<String> = _errorMessage
    val isLoading: LiveData<Boolean> = _isLoading
    val appStore: LiveData<AppStore> = _appStore
    val appTagList: LiveData<List<SineShopClient.AppTag>> = _appTagList // 弦应用商店标签

    init {
        Log.d("PlazaViewModel", "初始模式: $currentMode")
        viewModelScope.launch {
            _autoScrollMode.postValue(getAutoScrollMode())
        }
    }

    fun setAppStore(store: AppStore) {
        _appStore.value = store
        resetState()
        loadData()
        if (store == AppStore.SIENE_SHOP) {
            loadSineShopAppTags()
        }
    }

    private suspend fun getAutoScrollMode(): Boolean {
        val dataStore = getApplication<Application>().applicationContext.dataStore
        return try {
            val preferences = dataStore.data.first()
            preferences[AUTO_SCROLL_MODE_KEY] ?: false
        } catch (e: Exception) {
            Log.e("PlazaViewModel", "Error reading auto scroll mode from DataStore", e)
            false
        }
    }

    fun setAutoScrollMode(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().applicationContext.dataStore.edit { preferences ->
                preferences[AUTO_SCROLL_MODE_KEY] = enabled
            }
            _autoScrollMode.postValue(enabled)
            Log.d("PlazaViewModel", "自动滚动模式设置为：$enabled")
        }
    }

    // 核心初始化方法 - 类似 UserDetailViewModel 的模式
    fun initializeData(isMyResourceMode: Boolean, userId: Long? = null) {
        // 只有当模式或用户ID真正改变时才重新加载
        if (this._currentModeState != isMyResourceMode || this._currentUserIdState != userId) {
            this._currentModeState = isMyResourceMode
            this._currentUserIdState = userId
            this._isInitialized = false
            resetState()
            loadDataIfNeeded()
        } else {
            // 相同的状态，确保数据已加载
            loadDataIfNeeded()
        }
    }

    // 分类数据加载方法
    fun loadDataByCategory(categoryId: Int? = null, subCategoryId: Int? = null, userId: Long? = null) {
        if (categoryId != currentCategoryId || subCategoryId != currentSubCategoryId) {
            currentCategoryId = categoryId
            currentSubCategoryId = subCategoryId
            currentUserId = userId
            loadData(categoryId, subCategoryId, userId)
        }
    }

    private fun resetState() {
        _plazaData.postValue(PlazaData(emptyList()))
        _searchResults.postValue(emptyList())
        _errorMessage.postValue("")
        popularAppsPage = 1
        currentPage.postValue(1)
    }

    private fun loadDataIfNeeded() {
        if (!_isInitialized && !(_isLoading.value == true)) {
            _isInitialized = true
            loadData(currentCategoryId, currentSubCategoryId, _currentUserIdState)
        }
    }

    fun resetForMode(newMode: Boolean) {
        if (newMode != currentMode) {
            Log.d("PlazaViewModel", "模式变化: $currentMode -> $newMode")
            currentMode = newMode
            _isInitialized = false
            popularAppsPage = 1
            currentUserId = null
            _plazaData.value = PlazaData(emptyList())
            _searchResults.value = emptyList()
        }
    }
    
    fun setMyResourceMode(isMyResource: Boolean) {
        if (currentMode != isMyResource) {
            currentMode = isMyResource
            _isInitialized = false
            popularAppsPage = 1
            currentUserId = null
            _plazaData.value = PlazaData(emptyList())
            _searchResults.value = emptyList()
        }
    }

    fun loadData(categoryId: Int? = null, subCategoryId: Int? = null, userId: Long? = null) {
    if (_isLoading.value == true) return
    _isLoading.value = true
    popularAppsPage = 1
    currentPage.value = 1
    currentCategoryId = categoryId
    currentSubCategoryId = subCategoryId
    currentUserId = userId

    viewModelScope.launch(Dispatchers.IO) {
        try {
            Log.d("PlazaViewModel", "加载数据: 模式=$currentMode, 页码=$popularAppsPage, categoryId=$categoryId, subCategoryId=$subCategoryId, userId=$userId")
            val result = when (_appStore.value) {
                AppStore.XIAOQU_SPACE -> repository.getAppsList(
                    limit = if (currentMode) 12 else 9,
                    page = popularAppsPage,
                    userId = userId,
                    categoryId = categoryId,
                    subCategoryId = subCategoryId
                )
                AppStore.SIENE_SHOP -> {
                    val tagId = categoryId ?: 0 // 默认使用第一个分类
                    val appListResult = SineShopClient.getAppsList(tag = tagId, page = popularAppsPage)
                    if (appListResult.isSuccess) {
                        val apps = appListResult.getOrThrow()
                        popularAppsTotalPages = 1 // 弦应用商店没有提供总页数，暂时设置为 1
                        this@PlazaViewModel.totalPages.postValue(popularAppsTotalPages)
                        Result.success(Pair(apps.map { convertToUiModel(it) }, popularAppsTotalPages))
                    } else {
                        Result.failure(Exception("加载弦应用商店应用列表失败: ${appListResult.exceptionOrNull()?.message}"))
                    }
                }
                else -> { // 添加 else 分支
                    Result.failure(Exception("不支持的应用商店类型"))
                }
            }
            
            if (result.isSuccess) {
                val (apps, totalPages) = result.getOrThrow()
                popularAppsTotalPages = totalPages
                if (popularAppsTotalPages <= 0) popularAppsTotalPages = 1
                this@PlazaViewModel.totalPages.postValue(popularAppsTotalPages)

                if (apps.isEmpty()) {
                    _errorMessage.postValue("加载失败或暂无资源")
                    _plazaData.postValue(PlazaData(emptyList()))
                } else {
                    _plazaData.postValue(PlazaData(apps.map { 
                        // 确保 convertToUiModel 返回的是 AppItem 类型
                        if (it is KtorClient.AppItem) {
                            convertToUiModel(it)
                        } else if (it is SineShopClient.AppTag) {
                            convertToUiModel(it)
                        } else {
                            // 如果类型不匹配，则返回一个默认的 AppItem 或者抛出异常
                            AppItem("", "未知应用", "", 0) // 或者抛出异常
                        }
                     }))
                }
            } else {
                _errorMessage.postValue("加载失败: ${result.exceptionOrNull()?.message}")
                _plazaData.postValue(PlazaData(emptyList()))
            }
        } catch (e: Exception) {
            _errorMessage.postValue("发生错误: ${e.localizedMessage}")
            _plazaData.postValue(PlazaData(emptyList()))
        } finally {
            _isLoading.postValue(false)
        }
    }
}

    private fun loadSineShopAppTags() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SineShopClient.getAppTagList()
                if (result.isSuccess) {
                    _appTagList.postValue(result.getOrThrow())
                } else {
                    _errorMessage.postValue("加载弦应用商店分类失败: ${result.exceptionOrNull()?.message}")
                    _appTagList.postValue(emptyList()) // 确保即使加载失败，也设置为空列表
                }
            } catch (e: Exception) {
                _errorMessage.postValue("加载弦应用商店分类失败: ${e.localizedMessage}")
            }
        }
    }

            fun nextPage(onComplete: (() -> Unit)? = null) {
    if (_isLoading.value == true || popularAppsPage >= popularAppsTotalPages) return
    _isLoading.value = true
    popularAppsPage++
    currentPage.postValue(popularAppsPage)

    viewModelScope.launch(Dispatchers.IO) {
        try {
            val finalUserId = if (currentUserId != null) currentUserId else if (currentMode) {
                val context = getApplication<Application>().applicationContext
                val userCredentialsFlow = AuthManager.getCredentials(context)
                 val userCredentials = userCredentialsFlow.first()
                 userCredentials?.userId
            } else null
            val result = when (_appStore.value) {
                AppStore.XIAOQU_SPACE -> repository.getAppsList(
                    limit = if (currentMode) 12 else 9,
                    page = popularAppsPage,
                    userId = finalUserId,
                    categoryId = this@PlazaViewModel.currentCategoryId, // 使用 this@PlazaViewModel 访问成员变量
                    subCategoryId = this@PlazaViewModel.currentSubCategoryId // 使用 this@PlazaViewModel 访问成员变量
                )
                AppStore.SIENE_SHOP -> {
                    val tagId = this@PlazaViewModel.currentCategoryId ?: 0 // 使用 this@PlazaViewModel 访问成员变量
                    val appListResult = SineShopClient.getAppsList(tag = tagId, page = popularAppsPage)
                    if (appListResult.isSuccess) {
                        val apps = appListResult.getOrThrow()
                        popularAppsTotalPages = 1 // 弦应用商店没有提供总页数，暂时设置为 1
                        this@PlazaViewModel.totalPages.postValue(popularAppsTotalPages)
                        // 明确指定泛型类型为 AppItem
                        val appItems: List<AppItem> = apps.map { appTag ->
                            convertToUiModel(appTag)
                        }
                        Result.success(Pair(appItems, popularAppsTotalPages))
                    } else {
                        Result.failure(Exception("加载弦应用商店应用列表失败: ${appListResult.exceptionOrNull()?.message}"))
                    }
                }
                else -> { // 添加 else 分支
                    Result.failure(Exception("不支持的应用商店类型"))
                }
            }
            
            if (result.isSuccess) {
                val (newApps, totalPages) = result.getOrThrow()
                popularAppsTotalPages = totalPages
                this@PlazaViewModel.totalPages.postValue(popularAppsTotalPages)

                if (newApps.isNotEmpty()) {
                    val currentApps = _plazaData.value?.popularApps ?: emptyList()
                    val updatedApps = currentApps + newApps
                    _plazaData.postValue(PlazaData(popularApps = updatedApps))
                }
            } else {
                popularAppsPage--
                currentPage.postValue(popularAppsPage)
                _errorMessage.postValue("加载更多失败: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            popularAppsPage--
            currentPage.postValue(popularAppsPage)
            _errorMessage.postValue("加载更多失败: ${e.localizedMessage}")
        } finally {
            _isLoading.postValue(false)
            onComplete?.invoke()
        }
    }
}

    fun prevPage() {
        if (_isLoading.value == true || popularAppsPage <= 1) return
        _isLoading.value = true
        popularAppsPage--
        currentPage.postValue(popularAppsPage)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val finalUserId = if (currentUserId != null) currentUserId else if (currentMode) {
                     val context = getApplication<Application>().applicationContext
                    val userCredentialsFlow = AuthManager.getCredentials(context)
                     val userCredentials = userCredentialsFlow.first()
                     userCredentials?.userId
                } else null
                val result = when (_appStore.value) {
                    AppStore.XIAOQU_SPACE -> repository.getAppsList(
                        limit = if (currentMode) 12 else 9,
                        page = popularAppsPage,
                        userId = finalUserId,
                        categoryId = currentCategoryId,
                        subCategoryId = currentSubCategoryId
                    )
                    AppStore.SIENE_SHOP -> TODO("Implement SineShop App List Loading")
                    else -> { // 添加 else 分支
                        Result.failure(Exception("不支持的应用商店类型"))
                    }
                }
                
                if (result.isSuccess) {
                    val (newApps, totalPages) = result.getOrThrow()
                    popularAppsTotalPages = totalPages
                    this@PlazaViewModel.totalPages.postValue(totalPages)
                    _plazaData.postValue(PlazaData(popularApps = newApps.map { convertToUiModel(it) }))
                } else {
                    popularAppsPage++
                    currentPage.postValue(popularAppsPage)
                    _errorMessage.postValue("加载上一页失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                popularAppsPage++
                currentPage.postValue(popularAppsPage)
                _errorMessage.postValue("加载上一页失败: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun searchResources(query: String, isMyResource: Boolean = false) {
        if (_isLoading.value == true) return
        _isLoading.postValue(true)
        searchPage = 1
        currentQuery = query
        currentPage.postValue(1)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 修复：在"我的资源"或"TA的资源"模式下，需要传递 userId
                val finalUserId = when {
                    // 如果是"我的资源"模式，使用当前登录用户的ID
                    isMyResource && currentUserId == null ->{
                       val context = getApplication<Application>().applicationContext
                        val userCredentialsFlow = AuthManager.getCredentials(context)
                     val userCredentials = userCredentialsFlow.first()
                      userCredentials?.userId
                    }
                    // 如果是"TA的资源"模式，使用指定的 userId
                    currentUserId != null -> currentUserId
                    // 普通资源广场模式，不传递 userId
                    else -> null
                }
                
                val result = when (_appStore.value) {
                    AppStore.XIAOQU_SPACE -> repository.searchApps(query = query, page = searchPage, userId = finalUserId)
                    AppStore.SIENE_SHOP -> TODO("Implement SineShop App Search")
                    else -> { // 添加 else 分支
                        Result.failure(Exception("不支持的应用商店类型"))
                    }
                }
                
                if (result.isSuccess) {
                    val (results, totalPages) = result.getOrThrow()
                    searchTotalPages = totalPages
                    this@PlazaViewModel.totalPages.postValue(totalPages)

                    if (results.isEmpty()) {
                        _errorMessage.postValue("未找到相关资源")
                        _searchResults.postValue(emptyList())
                    } else {
                        _searchResults.postValue(results.map { convertToUiModel(it) })
                    }
                } else {
                    _errorMessage.postValue("搜索失败: ${result.exceptionOrNull()?.message}")
                    _searchResults.postValue(emptyList())
                }
            } catch (e: Exception) {
                _errorMessage.postValue("搜索失败: ${e.localizedMessage}")
                _searchResults.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun searchNextPage(query: String, onComplete: (() -> Unit)? = null) {
        if (_isLoading.value == true || searchPage >= searchTotalPages) return
        _isLoading.postValue(true)
        searchPage++
        currentPage.postValue(searchPage)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 修复：在分页搜索时也传递正确的 userId
                val finalUserId = when {
                    currentMode && currentUserId == null -> {
                          val context = getApplication<Application>().applicationContext
                        val userCredentialsFlow = AuthManager.getCredentials(context)
                         val userCredentials = userCredentialsFlow.first()
                          userCredentials?.userId
                    }
                    currentUserId != null -> currentUserId
                    else -> null
                }
                
                val result = when (_appStore.value) {
                    AppStore.XIAOQU_SPACE -> repository.searchApps(query = query, page = searchPage, userId = finalUserId)
                    AppStore.SIENE_SHOP -> TODO("Implement SineShop App Search Next Page")
                    else -> { // 添加 else 分支
                        Result.failure(Exception("不支持的应用商店类型"))
                    }
                }
                
                if (result.isSuccess) {
                    val (newResults, totalPages) = result.getOrThrow()
                    searchTotalPages = totalPages
                    this@PlazaViewModel.totalPages.postValue(totalPages)

                    if (newResults.isNotEmpty()) {
                        val currentResults = _searchResults.value ?: emptyList()
                        val updatedResults = currentResults + newResults.map { convertToUiModel(it) }
                        _searchResults.postValue(updatedResults)
                    }
                } else {
                    searchPage--
                    currentPage.postValue(searchPage)
                    _errorMessage.postValue("加载更多失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                searchPage--
                currentPage.postValue(searchPage)
                _errorMessage.postValue("加载更多失败: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
                onComplete?.invoke()
            }
        }
    }

    fun searchPrevPage(query: String) {
        if (_isLoading.value == true || searchPage <= 1) return
        _isLoading.postValue(true)
        searchPage--
        currentPage.postValue(searchPage)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 修复：在上一页搜索时也传递正确的 userId
                val finalUserId = when {
                    currentMode && currentUserId == null ->{
                         val context = getApplication<Application>().applicationContext
                        val userCredentialsFlow = AuthManager.getCredentials(context)
                          val userCredentials = userCredentialsFlow.first()
                          userCredentials?.userId
                    }
                    currentUserId != null -> currentUserId
                    else -> null
                }
                
                val result = when (_appStore.value) {
                    AppStore.XIAOQU_SPACE -> repository.searchApps(query = query, page = searchPage, userId = finalUserId)
                    AppStore.SIENE_SHOP -> TODO("Implement SineShop App Search Prev Page")
                    else -> { // 添加 else 分支
                        Result.failure(Exception("不支持的应用商店类型"))
                    }
                }
                
                if (result.isSuccess) {
                    val (newResults, totalPages) = result.getOrThrow()
                    searchTotalPages = totalPages
                    this@PlazaViewModel.totalPages.postValue(totalPages)
                    _searchResults.postValue(newResults.map { convertToUiModel(it) })
                } else {
                    searchPage++
                    currentPage.postValue(searchPage)
                    _errorMessage.postValue("加载上一页失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                searchPage++
                currentPage.postValue(searchPage)
                _errorMessage.postValue("加载上一页失败: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun cancelSearch() {
        _searchResults.postValue(emptyList())
    }

    private fun convertToUiModel(apiItem: KtorClient.AppItem): AppItem = AppItem(
        id = apiItem.id.toString(),
        name = apiItem.appname,
        iconUrl = apiItem.app_icon,
        versionId = apiItem.apps_version_id
    )

    private fun convertToUiModel(apiItem: SineShopClient.AppTag): AppItem = AppItem(
        id = apiItem.id.toString(),
        name = apiItem.name,
        iconUrl = apiItem.icon ?: "",
        versionId = 0 // 弦应用商店没有版本ID，暂时设置为0
    )

    fun goToPage(page: Int) {
    if (page < 1 || page > popularAppsTotalPages) {
        _errorMessage.postValue("页码超出范围")
        return
    }
    if (page == popularAppsPage && _plazaData.value?.popularApps?.isNotEmpty() == true) return

    _isLoading.value = true
    popularAppsPage = page
    currentPage.postValue(page)

    viewModelScope.launch(Dispatchers.IO) {
        try {
            val finalUserId = if (currentUserId != null) currentUserId else if (currentMode) {
                val context = getApplication<Application>().applicationContext
                val userCredentialsFlow = AuthManager.getCredentials(context)
                 val userCredentials = userCredentialsFlow.first()
                 userCredentials?.userId
            } else null
            val result = when (_appStore.value) {
                AppStore.XIAOQU_SPACE -> repository.getAppsList(
                    limit = if (currentMode) 12 else 9,
                    page = page,
                    userId = finalUserId,
                    categoryId = currentCategoryId,
                    subCategoryId = this@PlazaViewModel.currentSubCategoryId // 使用 this@PlazaViewModel 访问成员变量
                )
                AppStore.SIENE_SHOP -> TODO("Implement SineShop App List Loading")
                else -> { // 添加 else 分支
                    Result.failure(Exception("不支持的应用商店类型"))
                }
            }
            
            if (result.isSuccess) {
                val (newApps, totalPages) = result.getOrThrow()
                popularAppsTotalPages = totalPages
                this@PlazaViewModel.totalPages.postValue(totalPages)
                _plazaData.postValue(PlazaData(popularApps = newApps.map { convertToUiModel(it) }))
            } else {
                _errorMessage.postValue("加载失败: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            _errorMessage.postValue("加载失败: ${e.message}")
        } finally {
            _isLoading.postValue(false)
        }
    }
}

    fun searchGoToPage(page: Int) {
        if (page < 1 || page > searchTotalPages) {
            _errorMessage.postValue("页码超出范围")
            return
        }
        if (page == searchPage && _searchResults.value?.isNotEmpty() == true) return

        _isLoading.value = true
        searchPage = page
        currentPage.postValue(page)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 修复：在跳页搜索时也传递正确的 userId
                val finalUserId = when {
                    currentMode && currentUserId == null ->{
                         val context = getApplication<Application>().applicationContext
                        val userCredentialsFlow = AuthManager.getCredentials(context)
                         val userCredentials = userCredentialsFlow.first()
                         userCredentials?.userId
                    }
                    currentUserId != null -> currentUserId
                    else -> null
                }
                
                val result = when (_appStore.value) {
                    AppStore.XIAOQU_SPACE -> repository.searchApps(currentQuery, page = page, userId = finalUserId)
                    AppStore.SIENE_SHOP -> TODO("Implement SineShop App Search GoToPage")
                    else -> { // 添加 else 分支
                        Result.failure(Exception("不支持的应用商店类型"))
                    }
                }
                
                if (result.isSuccess) {
                    val (newResults, totalPages) = result.getOrThrow()
                    searchTotalPages = totalPages
                    this@PlazaViewModel.totalPages.postValue(totalPages)
                    _searchResults.postValue(newResults.map { convertToUiModel(it) })
                } else {
                    _errorMessage.postValue("加载失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("加载失败: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loadMore(isSearchMode: Boolean) {
        if (autoScrollMode.value == true && _isLoading.value != true) {
            if (isSearchMode) {
                if (searchPage < searchTotalPages) {
                    searchNextPage(currentQuery)
                }
            } else {
                if (popularAppsPage < popularAppsTotalPages) {
                    nextPage()
                }
            }
        }
    }
}

class PlazaRepository(private val context: Context) {
    private val api = KtorClient.ApiServiceImpl

    suspend fun getAppsList(
        limit: Int, 
        page: Int = 1, 
        categoryId: Int? = null, 
        subCategoryId: Int? = null, 
        userId: Long? = null
    ): Result<Pair<List<KtorClient.AppItem>, Int>> {
        return try {
            val result = api.getAppsList(
                limit = limit,
                page = page,
                sortOrder = "desc",
                categoryId = categoryId,
                subCategoryId = subCategoryId,
                appName = null,
                userId = userId
            )
            
            when {
                result.isSuccess -> {
                    val response = result.getOrThrow()
                    if (response.code == 1) {
                        val apps = response.data.list
                        val totalPages = response.data.pagecount.takeIf { it > 0 } ?: 1
                        Result.success(Pair(apps, totalPages))
                    } else {
                        Result.failure(Exception("API错误: ${response.msg}"))
                    }
                }
                else -> result.map { 
                    if (it.code == 1) {
                        val apps = it.data.list
                        val totalPages = it.data.pagecount.takeIf { it > 0 } ?: 1
                        Pair(apps, totalPages)
                    } else {
                        throw Exception("API错误: ${it.msg}")
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchApps(
    query: String, 
    page: Int = 1, 
    limit: Int = 20, 
    userId: Long? = null
): Result<Pair<List<KtorClient.AppItem>, Int>> {
    return try {
        val result = api.getAppsList(
            limit = limit,
            page = page,
            appName = query,
            sortOrder = "desc",
            userId = userId
            // 移除了 categoryId 和 subCategoryId 参数
        )
        
        when {
            result.isSuccess -> {
                val response = result.getOrThrow()
                if (response.code == 1) {
                    val apps = response.data.list
                    val totalPages = response.data.pagecount.takeIf { it > 0 } ?: 1
                    Result.success(Pair(apps, totalPages))
                } else {
                    Result.failure(Exception("搜索失败: ${response.msg}"))
                }
            }
            else -> result.map { 
                if (it.code == 1) {
                    val apps = it.data.list
                    val totalPages = it.data.pagecount.takeIf { it > 0 } ?: 1
                    Pair(apps, totalPages)
                } else {
                    throw Exception("搜索失败: ${it.msg}")
                }
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}