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
    // 将内部状态也暴露为 public val (或 private set) 以便 initialize 方法可以读取
    var isMyResourceMode: Boolean = false
        private set
    var currentUserId: String? = null
        private set
    var currentMode: String = "public" // 新增模式变量
        private set

    private var isSearchMode = false
    // 新增：保存状态
    private var savedCurrentPage: Int = 1
    private var savedCurrentQuery: String = ""
    private var savedCurrentCategoryId: String? = null

    private val currentRepository: IAppStoreRepository
        get() = repositories[_appStore.value] ?: throw IllegalStateException("No repository found for the selected app store")

    private val AUTO_SCROLL_MODE_KEY = booleanPreferencesKey("auto_scroll_mode")

    init {
        viewModelScope.launch {
            _autoScrollMode.postValue(readAutoScrollMode())
            // 移除 setAppStore 的调用，让 initialize 方法来处理初始加载
        }
    }

    // --- 公共方法 ---

    fun setAppStore(store: AppStore) {
        if (_appStore.value == store && _categories.value?.isNotEmpty() == true) return
        _appStore.value = store
        resetStateAndLoadCategories()
    }

    /**
     * 幂等的初始化方法。只有当传入的参数与当前状态不同时才会触发重新加载。
     * @param isMyResource 是否是我的资源模式
     * @param userId 用户ID (查看他人资源时)
     * @param mode 模式 ("public", "my_upload", "my_favourite", "my_history")
     */
    fun initialize(isMyResource: Boolean, userId: String?, mode: String = "public") {
        Log.d("PlazaViewModel", "initialize called with: isMyResource=$isMyResource, userId=$userId, mode=$mode")
        Log.d("PlazaViewModel", "Current state: isMyResourceMode=$isMyResourceMode, currentUserId=$currentUserId, currentMode=$currentMode")

        // 1. 检查参数是否真正改变
        val hasModeChanged = this.currentMode != mode
        val hasUserIdChanged = this.currentUserId != userId
        val hasIsMyResourceModeChanged = this.isMyResourceMode != isMyResource

        // 如果没有任何参数改变，则直接返回，不执行任何操作
        if (!hasModeChanged && !hasUserIdChanged && !hasIsMyResourceModeChanged) {
            Log.d("PlazaViewModel", "initialize: No parameters changed, skipping initialization.")
            return
        }

        // 2. 更新内部状态
        this.isMyResourceMode = isMyResource
        this.currentUserId = userId
        this.currentMode = mode

        // 3. 根据新模式确定目标 AppStore
        val targetAppStore = when (mode) {
            "my_upload", "my_favourite", "my_history" -> AppStore.SIENE_SHOP
            else -> AppStore.XIAOQU_SPACE
        }

        // 4. 检查 AppStore 是否需要改变
        val hasAppStoreChanged = _appStore.value != targetAppStore

        // 5. 如果 AppStore 需要改变，则更新它 (这会触发 resetStateAndLoadCategories)
        if (hasAppStoreChanged) {
            Log.d("PlazaViewModel", "initialize: AppStore changed to $targetAppStore, calling setAppStore.")
            _appStore.value = targetAppStore
            // setAppStore 内部会调用 resetStateAndLoadCategories
            return // setAppStore 会处理后续逻辑，这里直接返回
        }

        // 6. 如果 AppStore 没变，但参数变了，则手动触发 resetStateAndLoadCategories
        // 这种情况发生在：AppStore 不变，但 mode/userId/isMyResourceMode 改变了
        Log.d("PlazaViewModel", "initialize: Parameters changed but AppStore stayed the same, calling resetStateAndLoadCategories.")
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
    // ... (其余方法保持不变，除了 currentMode, currentUserId, isMyResourceMode 的访问方式) ...
    // 注意：需要将 currentMode, currentUserId, isMyResourceMode 从局部变量改为类的属性

    // --- 移动内部状态到类属性 ---
    // 将这些从 initialize 或其他方法的局部变量提升为类的私有属性
    private var currentCategoryId: String? = null
    private var currentQuery: String = ""

    private fun resetStateAndLoadCategories() {
        _isLoading.value = true // 开始加载
        // currentCategoryId 在 initialize 中已经根据 mode 设置过了
        // 或者在 setAppStore 中被重置为 null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 根据当前已设置的 currentMode 设置特殊的分类ID
                // 这里不再需要根据 currentMode 设置 AppStore，因为 initialize 已经处理了
                when (this@PlazaViewModel.currentMode) { // 使用 this@PlazaViewModel 明确引用
                    "my_upload" -> currentCategoryId = "-3"
                    "my_favourite" -> currentCategoryId = "-4"
                    "my_history" -> currentCategoryId = "-5"
                    else -> currentCategoryId = null
                }

                // 对于特殊模式，直接加载数据而不加载分类
                if (this@PlazaViewModel.currentMode in listOf("my_upload", "my_favourite", "my_history")) {
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

                        // 默认选中第一个分类（如果未设置特殊分类）
                        if (currentCategoryId == null) {
                            currentCategoryId = categoryList.firstOrNull()?.id
                        }

                        _isLoading.postValue(false)
                        withContext(Dispatchers.Main) {
                            loadPage(1, append = false)
                        }
                    } else {
                        handleFailure(categoriesResult.exceptionOrNull())
                        _categories.postValue(emptyList())
                        _isLoading.postValue(false)
                    }
                }
            } catch (e: Exception) {
                handleFailure(e)
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
                // 关键逻辑恢复：
                // 1. 如果 currentUserId 不为空（查看他人资源），直接使用。
                // 2. 如果 currentUserId 为空，但 isMyResourceMode 为 true（查看我的资源），从 AuthManager 获取。
                // 3. 否则为 null（普通广场）。
                val finalUserId = currentUserId ?: if (isMyResourceMode) { // 访问类属性
                    AuthManager.getCredentials(getApplication()).first()?.userId?.toString()
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