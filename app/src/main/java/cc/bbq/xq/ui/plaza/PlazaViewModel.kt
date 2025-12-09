// /app/src/main/java/cc/bbq/xq/ui/plaza/PlazaViewModel.kt
package cc.bbq.xq.ui.plaza

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.*
import cc.bbq.xq.AppStore
import cc.bbq.xq.AuthManager
import cc.bbq.xq.data.repository.IAppStoreRepository
import cc.bbq.xq.data.unified.UnifiedAppItem
import cc.bbq.xq.data.unified.UnifiedCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

// --- 统一的数据模型包装 ---
data class PlazaData(val popularApps: List<UnifiedAppItem>)

// --- Preference DataStore （仅保留 autoScrollMode） ---
private val Context.dataStore by preferencesDataStore(name = "plaza_preferences")

@KoinViewModel
class PlazaViewModel(
    private val app: Application,
    private val repositories: Map<AppStore, IAppStoreRepository>
) : AndroidViewModel(app) {

    // --- LiveData & State ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _appStore = MutableStateFlow(AppStore.XIAOQU_SPACE)
    val appStore: StateFlow<AppStore> = _appStore.asStateFlow()

    private val _categories = MutableStateFlow<List<UnifiedCategory>>(emptyList())
    val categories: StateFlow<List<UnifiedCategory>> = _categories.asStateFlow()

    private val _plazaData = MutableStateFlow(PlazaData(emptyList()))
    val plazaData: StateFlow<PlazaData> = _plazaData.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UnifiedAppItem>>(emptyList())
    val searchResults: StateFlow<List<UnifiedAppItem>> = _searchResults.asStateFlow()
    
    private var _currentStoreNameState: String = AppStore.XIAOQU_SPACE.name

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _autoScrollMode = MutableStateFlow<Boolean>(false)
    val autoScrollMode: StateFlow<Boolean> = _autoScrollMode.asStateFlow()

    // 新增：公开 currentCategoryId
    private val _currentCategoryId = MutableStateFlow<String?>(null)
    val currentCategoryId: StateFlow<String?> = _currentCategoryId.asStateFlow()

    // --- 内部状态管理 ---
    private var isSearchMode = false
    private var currentQuery = ""
    // private var currentCategoryId: String? = null // 移除 private 属性
    private var currentUserId: String? = null
    private var isMyResourceMode: Boolean = false
    private var currentMode: String = "public"

    // 新增：状态跟踪变量（参考旧版本 UserDetailViewModel 模式）
    private var _isInitialized = false
    private var _currentIsMyResourceMode: Boolean = false
    private var _currentUserIdState: String? = null
    private var _currentModeState: String = ""

    // 新增：保存状态 (只保留 currentPage 和 query)
    private var savedCurrentPage: Int = 1
    private var savedCurrentQuery: String = ""
    // 移除：private var savedCurrentCategoryId: String? = null

    private val currentRepository: IAppStoreRepository
        get() = repositories[_appStore.value] ?: throw IllegalStateException("No repository found for the selected app store")

    private val AUTO_SCROLL_MODE_KEY = booleanPreferencesKey("auto_scroll_mode")

    init {
        viewModelScope.launch {
            readAutoScrollMode().collect {
                _autoScrollMode.value = it
            }
        }
    }

    /**
     * 初始化方法：参考旧版本逻辑，只有参数真正变化时才重置并重新加载
     */
    fun initialize(isMyResource: Boolean, userId: String?, mode: String = "public", storeName: String = AppStore.XIAOQU_SPACE.name) {
        Log.d("PlazaViewModel", "initialize called: isMyResource=$isMyResource, userId=$userId, mode=$mode, store=$storeName")
        
        // 只有当模式、用户ID、模式或商店真正改变时才重新初始化
        val needsReinit = _currentIsMyResourceMode != isMyResource ||
                          _currentUserIdState != userId ||
                          _currentModeState != mode ||
                          _currentStoreNameState != storeName
        
        if (needsReinit) {
            Log.d("PlazaViewModel", "参数变化，重新初始化...")
            // 更新跟踪状态
            _currentIsMyResourceMode = isMyResource
            _currentUserIdState = userId
            _currentModeState = mode
            _currentStoreNameState = storeName
            _isInitialized = false
            
            // 更新内部状态
            this.isMyResourceMode = isMyResource
            this.currentUserId = userId
            this.currentMode = mode
            
            // 设置商店
            val targetStore = try {
                AppStore.valueOf(storeName)
            } catch (e: Exception) {
                AppStore.XIAOQU_SPACE
            }
            // 立即设置商店，确保后续加载使用正确的仓库
            if (_appStore.value != targetStore) {
                _appStore.value = targetStore
            }
            
            // 重置数据状态并加载
            resetStateAndLoadCategories()
        } else {
            Log.d("PlazaViewModel", "参数未变化，确保数据已加载...")
            loadDataIfNeeded()
        }
    }

    fun setAppStore(store: AppStore) {
        if (_appStore.value == store && _categories.value.isNotEmpty()) return
        _appStore.value = store
        resetStateAndLoadCategories()
    }

    fun loadCategory(categoryId: String?) {
        if (isSearchMode) {
            cancelSearch()
        }
        if (_currentCategoryId.value == categoryId) return

        // currentCategoryId = categoryId // 移除：不再直接修改实例变量
        _currentCategoryId.value = categoryId // 使用 LiveData 更新
        loadPage(1)
    }

    fun search(query: String) {
        if (query.isBlank()) return
        isSearchMode = true
        currentQuery = query
        savedCurrentQuery = query
        _plazaData.value = PlazaData(emptyList())
        _searchResults.value = emptyList()
        loadPage(1)
    }

    fun cancelSearch() {
        isSearchMode = false
        currentQuery = ""
        savedCurrentQuery = ""
        _searchResults.value = emptyList()
        loadPage(1)
    }

    fun nextPage() {
        val next = _currentPage.value + 1
        loadPage(next, append = autoScrollMode.value)
    }

    fun prevPage() {
        val prev = _currentPage.value - 1
        loadPage(prev)
    }

    fun goToPage(page: Int) {
        savedCurrentPage = page
        loadPage(page)
    }

    fun loadMore() {
        if (autoScrollMode.value && !_isLoading.value) {
            val current = _currentPage.value
            val total = _totalPages.value
            if (current < total) {
                loadPage(current + 1, append = true)
            }
        }
    }

    // --- 新增：参考旧版本的 loadDataIfNeeded ---
    private fun loadDataIfNeeded() {
        if (!_isInitialized) {
            resetStateAndLoadCategories() // 或者直接调用 loadPage(1) 如果状态已经准备好
            // 实际上，如果参数没变，状态应该也基本准备好，直接加载第一页即可
        }
    }

    // --- 修改：resetStateAndLoadCategories ---
    private fun resetStateAndLoadCategories() {
        Log.d("PlazaViewModel", "resetStateAndLoadCategories called")
        _isLoading.value = true
        // currentCategoryId = null // 移除：不再直接修改实例变量
        _currentCategoryId.value = null // 使用 LiveData 更新
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 根据模式设置应用商店（已在 initialize 中处理，此处可优化）
                when (currentMode) {
                    "my_upload", "my_favourite", "my_history" -> {
                        if (_appStore.value != AppStore.SIENE_SHOP) {
                            _appStore.value = AppStore.SIENE_SHOP
                        }
                    }
                }
                
                when (currentMode) {
                    "my_upload" ->  _currentCategoryId.value = "-3" //currentCategoryId = "-3"
                    "my_favourite" -> _currentCategoryId.value = "-4"//currentCategoryId = "-4"
                    "my_history" -> _currentCategoryId.value = "-5" //currentCategoryId = "-5"
                    else -> _currentCategoryId.value = null //currentCategoryId = null
                }
                
                if (currentMode in listOf("my_upload", "my_favourite", "my_history")) {
                    _categories.value = emptyList()
                    _isLoading.value = false
                    withContext(Dispatchers.Main) {
                        loadPage(1, append = false)
                    }
                } else {
                    val categoriesResult = currentRepository.getCategories()
                    if (categoriesResult.isSuccess) {
                        val categoryList = categoriesResult.getOrThrow()
                        _categories.value = categoryList
                        
                        if (_currentCategoryId.value == null) {
                            // currentCategoryId = categoryList.firstOrNull()?.id // 移除
                            _currentCategoryId.value = categoryList.firstOrNull()?.id // 使用 LiveData 更新
                        }
                        
                        _isLoading.value = false
                        withContext(Dispatchers.Main) {
                            loadPage(1, append = false)
                        }
                    } else {
                        handleFailure(categoriesResult.exceptionOrNull())
                        _categories.value = emptyList()
                        _isLoading.value = false
                        // 加载失败，保持 _isInitialized = false，允许重试
                    }
                }
            } catch (e: Exception) {
                handleFailure(e)
                _isLoading.value = false
                 // 加载失败，保持 _isInitialized = false，允许重试
            }
        }
    }

    // --- 修改：loadPage ---
    // --- 修改：loadPage ---
    private fun loadPage(page: Int, append: Boolean = false) {
        Log.d("PlazaViewModel", "loadPage called: page=$page, append=$append, _isInitialized=$_isInitialized, currentCategoryId=${_currentCategoryId.value}")
        if (_isLoading.value && !append) return
        
        val total = _totalPages.value
        if (page < 1 || (page > total && total > 0 && !append)) return

        _isLoading.value = true 
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                //val finalUserId = currentUserId ?: if (isMyResourceMode) {
                //    AuthManager.getCredentials(getApplication()).first()?.userId?.toString()
                //} else null
                // 移除上面这段，强制使用 currentUserId

                val finalUserId = currentUserId

                val result = if (isSearchMode) {
                    currentRepository.searchApps(currentQuery, page, finalUserId)
                } else {
                    currentRepository.getApps(_currentCategoryId.value, page, finalUserId) // 使用 LiveData 的值
                }

                if (result.isSuccess) {
                    val (items, totalPages) = result.getOrThrow()
                    _totalPages.value = if(totalPages > 0) totalPages else 1
                    _currentPage.value = page
                    savedCurrentPage = page

                    if (isSearchMode) {
                        val currentList = if (append) _searchResults.value else emptyList()
                        _searchResults.value = currentList + items
                    } else {
                        val currentList = if (append) _plazaData.value.popularApps else emptyList()
                        _plazaData.value = PlazaData(currentList + items)
                    }
                    
                    // 关键修改：只有在成功加载第一页数据后，才将 _isInitialized 设为 true
                    // 这表明初始数据加载已完成
                    if (!_isInitialized && page == 1 && !append) {
                         Log.d("PlazaViewModel", "Initial data load successful, setting _isInitialized = true")
                         _isInitialized = true
                    }
                } else {
                    handleFailure(result.exceptionOrNull())
                }
            } catch (e: Exception) {
                handleFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleFailure(exception: Throwable?) {
        val message = "操作失败: ${exception?.message ?: "未知错误"}"
        Log.e("PlazaViewModel", message, exception)
        _errorMessage.value = message
    }

    private fun readAutoScrollMode(): Flow<Boolean> {
        return app.applicationContext.dataStore.data
            .map { preferences ->
                preferences[AUTO_SCROLL_MODE_KEY] ?: false
            }
            .catch { emit(false) }
    }

    fun setAutoScrollMode(enabled: Boolean) {
        viewModelScope.launch {
            app.applicationContext.dataStore.edit { preferences ->
                preferences[AUTO_SCROLL_MODE_KEY] = enabled
            }
            _autoScrollMode.value = enabled
        }
    }
}