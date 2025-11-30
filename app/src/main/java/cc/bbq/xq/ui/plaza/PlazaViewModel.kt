// /app/src/main/java/cc/bbq/xq/ui/plaza/PlazaViewModel.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- 统一的数据模型包装 ---
data class PlazaData(val popularApps: List<UnifiedAppItem>)

// --- Preference DataStore （仅保留 autoScrollMode） ---
private val Context.dataStore by preferencesDataStore(name = "plaza_preferences")

class PlazaViewModel(
    private val app: Application,
    private val repositories: Map<AppStore, IAppStoreRepository>
) : AndroidViewModel(app) {

    // --- LiveData & State ---
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _appStore = MutableLiveData(AppStore.XIAOQU_SPACE)
    val appStore: LiveData<AppStore> = _appStore

    private val _categories = MutableLiveData<List<UnifiedCategory>>(emptyList())
    val categories: LiveData<List<UnifiedCategory>> = _categories

    private val _plazaData = MutableLiveData(PlazaData(emptyList()))
    val plazaData: LiveData<PlazaData> = _plazaData

    private val _searchResults = MutableLiveData<List<UnifiedAppItem>>(emptyList())
    val searchResults: LiveData<List<UnifiedAppItem>> = _searchResults

    private val _currentPage = MutableLiveData(1)
    val currentPage: LiveData<Int> = _currentPage

    private val _totalPages = MutableLiveData(1)
    val totalPages: LiveData<Int> = _totalPages

    private val _autoScrollMode = MutableLiveData<Boolean>()
    val autoScrollMode: LiveData<Boolean> = _autoScrollMode

    // 新增：公开 currentCategoryId
    private val _currentCategoryId = MutableLiveData<String?>(null)
    val currentCategoryId: LiveData<String?> = _currentCategoryId

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
        get() = repositories[_appStore.value ?: AppStore.XIAOQU_SPACE] ?: throw IllegalStateException("No repository found for the selected app store")

    private val AUTO_SCROLL_MODE_KEY = booleanPreferencesKey("auto_scroll_mode")

    init {
        viewModelScope.launch {
            _autoScrollMode.postValue(readAutoScrollMode())
        }
    }

    /**
     * 初始化方法：参考旧版本逻辑，只有参数真正变化时才重置并重新加载
     */
    fun initialize(isMyResource: Boolean, userId: String?, mode: String = "public") {
        Log.d("PlazaViewModel", "initialize called: isMyResource=$isMyResource, userId=$userId, mode=$mode")
        Log.d("PlazaViewModel", "Current tracked state: _isInitialized=$_isInitialized, _currentIsMyResourceMode=$_currentIsMyResourceMode, _currentUserIdState=$_currentUserIdState, _currentModeState=$_currentModeState")

        // 只有当模式、用户ID或模式真正改变时才重新初始化
        val needsReinit = _currentIsMyResourceMode != isMyResource ||
                          _currentUserIdState != userId ||
                          _currentModeState != mode
        
        if (needsReinit) {
            Log.d("PlazaViewModel", "参数变化，重新初始化...")
            // 更新跟踪状态
            _currentIsMyResourceMode = isMyResource
            _currentUserIdState = userId
            _currentModeState = mode
            _isInitialized = false  // 重置初始化标志
            
            // 更新内部状态
            this.isMyResourceMode = isMyResource
            this.currentUserId = userId
            this.currentMode = mode
            
            // 重置数据状态并加载
            resetStateAndLoadCategories()
        } else {
            Log.d("PlazaViewModel", "参数未变化，确保数据已加载... _isInitialized=$_isInitialized")
            // 参数未变化，确保数据已加载（参考旧版本）
            loadDataIfNeeded()
        }
    }

    fun setAppStore(store: AppStore) {
        if (_appStore.value == store && _categories.value?.isNotEmpty() == true) return
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
        val next = (_currentPage.value ?: 1) + 1
        loadPage(next, append = autoScrollMode.value == true)
    }

    fun prevPage() {
        val prev = (_currentPage.value ?: 1) - 1
        loadPage(prev)
    }

    fun goToPage(page: Int) {
        savedCurrentPage = page
        loadPage(page)
    }

    fun loadMore() {
        if (autoScrollMode.value == true && _isLoading.value != true) {
            val current = _currentPage.value ?: 1
            val total = _totalPages.value ?: 1
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
                            _appStore.postValue(AppStore.SIENE_SHOP)
                        }
                    }
                }
                
                when (currentMode) {
                    "my_upload" ->  _currentCategoryId.postValue("-3") //currentCategoryId = "-3"
                    "my_favourite" -> _currentCategoryId.postValue("-4")//currentCategoryId = "-4"
                    "my_history" -> _currentCategoryId.postValue("-5") //currentCategoryId = "-5"
                    else -> _currentCategoryId.postValue(null) //currentCategoryId = null
                }
                
                if (currentMode in listOf("my_upload", "my_favourite", "my_history")) {
                    _categories.postValue(emptyList())
                    _isLoading.postValue(false)
                    withContext(Dispatchers.Main) {
                        loadPage(1, append = false)
                    }
                } else {
                    val categoriesResult = currentRepository.getCategories()
                    if (categoriesResult.isSuccess) {
                        val categoryList = categoriesResult.getOrThrow()
                        _categories.postValue(categoryList)
                        
                        if (_currentCategoryId.value == null) {
                            // currentCategoryId = categoryList.firstOrNull()?.id // 移除
                            _currentCategoryId.postValue(categoryList.firstOrNull()?.id) // 使用 LiveData 更新
                        }
                        
                        _isLoading.postValue(false)
                        withContext(Dispatchers.Main) {
                            loadPage(1, append = false)
                        }
                    } else {
                        handleFailure(categoriesResult.exceptionOrNull())
                        _categories.postValue(emptyList())
                        _isLoading.postValue(false)
                        // 加载失败，保持 _isInitialized = false，允许重试
                    }
                }
            } catch (e: Exception) {
                handleFailure(e)
                _isLoading.postValue(false)
                 // 加载失败，保持 _isInitialized = false，允许重试
            }
        }
    }

    // --- 修改：loadPage ---
    private fun loadPage(page: Int, append: Boolean = false) {
        Log.d("PlazaViewModel", "loadPage called: page=$page, append=$append, _isInitialized=$_isInitialized, currentCategoryId=${_currentCategoryId.value}")
        if (_isLoading.value == true && !append) return
        
        val total = _totalPages.value ?: 1
        if (page < 1 || (page > total && total > 0 && !append)) return

        _isLoading.value = true 
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val finalUserId = currentUserId ?: if (isMyResourceMode) {
                    AuthManager.getCredentials(getApplication()).first()?.userId?.toString()
                } else null

                val result = if (isSearchMode) {
                    currentRepository.searchApps(currentQuery, page, finalUserId)
                } else {
                    currentRepository.getApps(_currentCategoryId.value, page, finalUserId) // 使用 LiveData 的值
                }

                if (result.isSuccess) {
                    val (items, totalPages) = result.getOrThrow()
                    _totalPages.postValue(if(totalPages > 0) totalPages else 1)
                    _currentPage.postValue(page)
                    savedCurrentPage = page

                    if (isSearchMode) {
                        val currentList = if (append) _searchResults.value ?: emptyList() else emptyList()
                        _searchResults.postValue(currentList + items)
                    } else {
                        val currentList = if (append) _plazaData.value?.popularApps ?: emptyList() else emptyList()
                        _plazaData.postValue(PlazaData(currentList + items))
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
                _isLoading.postValue(false)
            }
        }
    }

    private fun handleFailure(exception: Throwable?) {
        val message = "操作失败: ${exception?.message ?: "未知错误"}"
        Log.e("PlazaViewModel", message, exception)
        _errorMessage.postValue(message)
    }

    private suspend fun readAutoScrollMode(): Boolean {
        return try {
            app.applicationContext.dataStore.data.first()[AUTO_SCROLL_MODE_KEY] ?: false
        } catch (e: Exception) { false }
    }

    fun setAutoScrollMode(enabled: Boolean) {
        viewModelScope.launch {
            app.applicationContext.dataStore.edit { preferences ->
                preferences[AUTO_SCROLL_MODE_KEY] = enabled
            }
            _autoScrollMode.postValue(enabled)
        }
    }
}