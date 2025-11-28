//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
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
import cc.bbq.xq.KtorClient
import cc.bbq.xq.data.repository.IAppStoreRepository
import cc.bbq.xq.data.repository.SineShopRepository
import cc.bbq.xq.data.repository.XiaoQuRepository
import cc.bbq.xq.data.unified.UnifiedAppItem
import cc.bbq.xq.data.unified.UnifiedCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// --- 统一的数据模型包装 ---
data class PlazaData(val popularApps: List<UnifiedAppItem>)

// --- Preference DataStore ---
private val Context.dataStore by preferencesDataStore(name = "plaza_preferences")

// --- ViewModel 工厂 ---
class PlazaViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlazaViewModel::class.java)) {
            // 在工厂中创建并注入 Repository 实例
            val repositories = mapOf(
                AppStore.XIAOQU_SPACE to XiaoQuRepository(KtorClient.ApiServiceImpl),
                AppStore.SIENE_SHOP to SineShopRepository()
            )
            return PlazaViewModel(application, repositories) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class PlazaViewModel(
    application: Application,
    private val repositories: Map<AppStore, IAppStoreRepository>
) : AndroidViewModel(application) {

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
        resetStateAndLoadCategories()
    }

    fun loadCategory(categoryId: String?) {
        if (currentCategoryId == categoryId) return
        isSearchMode = false
        _searchResults.value = emptyList()
        currentCategoryId = categoryId
        loadPage(1)
    }

    fun search(query: String) {
        if (query.isBlank()) return
        isSearchMode = true
        currentQuery = query
        loadPage(1)
    }

    fun cancelSearch() {
        isSearchMode = false
        currentQuery = ""
        _searchResults.value = emptyList()
        // 重新加载当前分类的第一页
        loadPage(1)
    }

    fun nextPage() = loadPage( (_currentPage.value ?: 1) + 1 )
    fun prevPage() = loadPage( (_currentPage.value ?: 1) - 1 )
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
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val categoriesResult = currentRepository.getCategories()
                if (categoriesResult.isSuccess) {
                    val categoryList = categoriesResult.getOrThrow()
                    _categories.postValue(categoryList)
                    // 加载完分类后，自动加载第一个分类的数据
                    currentCategoryId = categoryList.firstOrNull()?.id
                    loadPage(1)
                } else {
                    handleFailure(categoriesResult.exceptionOrNull())
                }
            } catch (e: Exception) {
                handleFailure(e)
            } finally {
                // loadPage会处理isLoading状态
            }
        }
    }

    private fun loadPage(page: Int, append: Boolean = false) {
        if (_isLoading.value == true) return
        val total = _totalPages.value ?: 1
        if (page < 1 || (page > total && !append)) { // 在追加模式下，即使页码大于当前总页数也可能尝试加载
            return
        }

        _isLoading.value = true
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
                    _totalPages.postValue(totalPages)
                    _currentPage.postValue(page)

                    if (isSearchMode) {
                        val currentList = if (append) _searchResults.value ?: emptyList() else emptyList()
                        _searchResults.postValue(currentList + items)
                    } else {
                        val currentList = if (append) _plazaData.value?.popularApps ?: emptyList() else emptyList()
                        _plazaData.postValue(PlazaData(currentList + items))
                    }
                    _errorMessage.postValue(null)
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
            getApplication<Application>().applicationContext.dataStore.data.first()[AUTO_SCROLL_MODE_KEY] ?: false
        } catch (e: Exception) { false }
    }

    fun setAutoScrollMode(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().applicationContext.dataStore.edit { preferences ->
                preferences[AUTO_SCROLL_MODE_KEY] = enabled
            }
            _autoScrollMode.postValue(enabled)
        }
    }
}