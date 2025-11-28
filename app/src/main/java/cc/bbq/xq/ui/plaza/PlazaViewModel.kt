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

    private val currentRepository: IAppStoreRepository
        get() = repositories[_appStore.value] ?: throw IllegalStateException("No repository found for the selected app store")

    private val AUTO_SCROLL_MODE_KEY = booleanPreferencesKey("auto_scroll_mode")

    init {
        viewModelScope.launch {
            _autoScrollMode.postValue(readAutoScrollMode())
            // 初始化时加载默认商店的数据
            setAppStore(AppStore.XIAOQU_SPACE)
        }
    }

    // --- 公共方法 ---

    fun setAppStore(store: AppStore) {
        if (_appStore.value == store && _categories.value?.isNotEmpty() == true) return
        _appStore.value = store
        resetStateAndLoadCategories()
    }

    fun initialize(isMyResource: Boolean, userId: String?) {
        if (this.isMyResourceMode == isMyResource && this.currentUserId == userId) return
        this.isMyResourceMode = isMyResource
        this.currentUserId = userId
        // 切换商店或模式时，重新加载分类和数据
        resetStateAndLoadCategories()
    }

    fun loadCategory(categoryId: String?) {
        // 如果正在搜索，选择分类意味着退出搜索模式
        if (isSearchMode) {
           cancelSearch()
        }
        if (currentCategoryId == categoryId) return

        currentCategoryId = categoryId
        loadPage(1)
    }

    fun search(query: String) {
        if (query.isBlank()) return
        isSearchMode = true
        currentQuery = query
        // 清空列表数据，保留分类和搜索词
        _plazaData.value = PlazaData(emptyList())
        _searchResults.value = emptyList() // 清空搜索结果以触发UI更新
        loadPage(1)
    }

    fun cancelSearch() {
        isSearchMode = false
        currentQuery = ""
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
    fun goToPage(page: Int) = loadPage(page)

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
        currentCategoryId = null // 重置分类选择
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val categoriesResult = currentRepository.getCategories()
                if (categoriesResult.isSuccess) {
                    val categoryList = categoriesResult.getOrThrow()
                    _categories.postValue(categoryList)
                    
                    // 默认选中第一个分类
                    currentCategoryId = categoryList.firstOrNull()?.id
                    
                    // --- 关键修复 ---
                    // 在调用 loadPage 之前，必须先将 isLoading 设为 false
                    // 否则 loadPage 会认为正在加载中而直接返回
                    _isLoading.postValue(false)
                    
                    // 切换到主线程确保状态同步后再加载页面
                    withContext(Dispatchers.Main) {
                        loadPage(1, append = false)
                    }
                } else {
                    handleFailure(categoriesResult.exceptionOrNull())
                    _categories.postValue(emptyList())
                    _isLoading.postValue(false)
                }
            } catch (e: Exception) {
                handleFailure(e)
                _isLoading.postValue(false)
            }
        }
    }

    private fun loadPage(page: Int, append: Boolean = false) {
        // 如果正在加载且不是追加模式（自动翻页），则阻止重复请求
        if (_isLoading.value == true && !append) return
        
        val total = _totalPages.value ?: 1
        if (page < 1 || (page > total && total > 0 && !append)) return

        _isLoading.value = true // 立即在主线程设置 Loading 状态
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val finalUserId = if (isMyResourceMode) {
                    currentUserId ?: AuthManager.getCredentials(getApplication()).first()?.userId.toString()
                } else null

                val result = if (isSearchMode) {
                    currentRepository.searchApps(currentQuery, page, finalUserId)
                } else {
                    currentRepository.getApps(currentCategoryId, page, finalUserId)
                }

                if (result.isSuccess) {
                    val (items, totalPages) = result.getOrThrow()
                    _totalPages.postValue(if(totalPages > 0) totalPages else 1)
                    _currentPage.postValue(page)

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

    // --- DataStore 操作 ---
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