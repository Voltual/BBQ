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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- 统一的数据模型包装 ---
data class PlazaData(val popularApps: List<UnifiedAppItem>)

// --- Preference DataStore ---
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

    // --- 内部状态管理 ---
    private var isSearchMode = false
    private var currentQuery = ""
    private var currentCategoryId: String? = null
    private var currentUserId: String? = null
    private var isMyResourceMode: Boolean = false
    private var currentMode: String = "public" // 新增模式变量

    // 新增：保存状态
    private var savedCurrentPage: Int = 1
    private var savedCurrentQuery: String = ""
    private var savedCurrentCategoryId: String? = null

    private val currentRepository: IAppStoreRepository
        get() = repositories[_appStore.value] ?: throw IllegalStateException("No repository found for the selected app store")

    private val AUTO_SCROLL_MODE_KEY = booleanPreferencesKey("auto_scroll_mode")

    // --- 状态跟踪 ---
    private var _isInitialized = false
    private var _currentModeState: String? = null // 使用 String? 类型
    private var _currentUserIdState: String? = null // 使用 String? 类型
    private var _currentAppStoreState: AppStore? = null // 新增 AppStore 状态

    init {
        viewModelScope.launch {
            _autoScrollMode.postValue(readAutoScrollMode())
        }
    }

    // --- 公共方法 ---

    fun setAppStore(store: AppStore) {
        if (_appStore.value == store) return // 修正：如果 AppStore 没有变化，直接返回
        _appStore.value = store
        resetStateAndLoadCategories()
    }

    fun initialize(isMyResource: Boolean, userId: String?, mode: String = "public") {

        // 更新状态
        this.isMyResourceMode = isMyResource
        this.currentUserId = userId
        this.currentMode = mode

        // 根据模式设置应用商店
        val targetAppStore = when (mode) {
            "my_upload", "my_favourite", "my_history" -> AppStore.SIENE_SHOP
            else -> AppStore.XIAOQU_SPACE
        }
        
        if (_appStore.value != targetAppStore) {
            _appStore.value = targetAppStore
        }

        // 根据模式设置特殊的分类ID
        currentCategoryId = when (mode) {
            "my_upload" -> "-3"
            "my_favourite" -> "-4"
            "my_history" -> "-5"
            else -> null
        }

        // 重置状态并加载数据
        resetStateAndLoadCategories()
    }

    fun loadCategory(categoryId: String?) {
        // 如果正在搜索，选择分类意味着退出搜索模式
        if (isSearchMode) {
           cancelSearch()
        }
        if (currentCategoryId == categoryId) return

        currentCategoryId = categoryId
        savedCurrentCategoryId = categoryId // 保存状态
        loadPage(1)
    }

    fun search(query: String) {
        if (query.isBlank()) return
        isSearchMode = true
        currentQuery = query
        savedCurrentQuery = query // 保存状态
        // 清空列表数据，保留分类和搜索词
        _plazaData.value = PlazaData(emptyList())
        _searchResults.value = emptyList() // 清空搜索结果以触发UI更新
        loadPage(1)
    }

    fun cancelSearch() {
        isSearchMode = false
        currentQuery = ""
        savedCurrentQuery = "" // 保存状态
        _searchResults.value = emptyList()
        // 重新加载当前分类的第一页
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
        savedCurrentPage = page // 保存状态
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

    // --- 私有辅助方法 ---

    private fun resetStateAndLoadCategories() {
        _isLoading.value = true // 开始加载分类
        _categories.value = emptyList() // 清空分类数据
        _plazaData.value = PlazaData(emptyList()) // 清空列表数据
        _searchResults.value = emptyList() // 清空搜索结果
        _currentPage.value = 1 // 重置当前页码
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 根据模式设置特殊的分类ID
                val specialCategoryId = when (currentMode) {
                    "my_upload" -> "-3"
                    "my_favourite" -> "-4"
                    "my_history" -> "-5"
                    else -> null
                }
                
                // 对于特殊模式，直接加载数据而不加载分类
                if (specialCategoryId != null) {
                    currentCategoryId = specialCategoryId
                    _categories.postValue(emptyList())
                    loadPage(1, append = false)
                } else {
                    val categoriesResult = currentRepository.getCategories()
                    if (categoriesResult.isSuccess) {
                        val categoryList = categoriesResult.getOrThrow()
                        _categories.postValue(categoryList)
                        
                        // 默认选中第一个分类
                        currentCategoryId = categoryList.firstOrNull()?.id
                        
                        loadPage(1, append = false)
                    } else {
                        handleFailure(categoriesResult.exceptionOrNull())
                        _categories.postValue(emptyList())
                    }
                }
            } catch (e: Exception) {
                handleFailure(e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun loadPage(page: Int, append: Boolean = false) {
        if (_isLoading.value == true && !append) return
        
        val total = _totalPages.value ?: 1
        if (page < 1 || (page > total && total > 0 && !append)) return

        _isLoading.value = true 
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val finalUserId = currentUserId

                val result = if (isSearchMode) {
                    currentRepository.searchApps(currentQuery, page, finalUserId)
                } else {
                    currentRepository.getApps(currentCategoryId, page, finalUserId)
                }

                if (result.isSuccess) {
                    val (items, totalPages) = result.getOrThrow()
                    _totalPages.postValue(if(totalPages > 0) totalPages else 1)
                    _currentPage.postValue(page)
                    savedCurrentPage = page // 保存状态

                    if (isSearchMode) {
                        val currentList = if (append) _searchResults.value ?: emptyList() else emptyList()
                        _searchResults.postValue(currentList + items)
                    } else {
                        val currentList = if (append) _plazaData.value?.popularApps ?: emptyList() else emptyList()
                        _plazaData.postValue(PlazaData(currentList + items))
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