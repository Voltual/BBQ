//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.billing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AuthManager
import cc.bbq.xq.KtorClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BillingState(
    val billings: List<KtorClient.BillingItem> = emptyList(), // 使用 KtorClient.BillingItem
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()
    
    private val context = application.applicationContext

    fun loadBilling() {
        val token = AuthManager.getCredentials(context)?.third ?: ""
        
        _state.value = _state.value.copy(
            isLoading = true,
            error = null,
            currentPage = 1
        )
        
        viewModelScope.launch {
            try {
                val billingResult = KtorClient.ApiServiceImpl.getUserBilling(
                    token = token,
                    limit = 10,
                    page = 1
                )
                
                if (billingResult.isSuccess) {
                    billingResult.getOrNull()?.let { billingResponse ->
                        if (billingResponse.code == 1) {
                            billingResponse.data?.let { data ->
                                _state.value = BillingState(
                                    billings = data.list,
                                    currentPage = 1,
                                    totalPages = data.pagecount,
                                    isLoading = false
                                )
                            } ?: run {
                                _state.value = _state.value.copy(
                                    error = "加载账单失败: 数据为空",
                                    isLoading = false
                                )
                            }
                        } else {
                            _state.value = _state.value.copy(
                                error = billingResponse.msg ?: "加载账单失败",
                                isLoading = false
                            )
                        }
                    }
                } else {
                    _state.value = _state.value.copy(
                        error = billingResult.exceptionOrNull()?.message ?: "加载账单失败",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "网络错误: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun loadNextPage() {
        val nextPage = _state.value.currentPage + 1
        if (nextPage > _state.value.totalPages) return
        
        val token = AuthManager.getCredentials(context)?.third ?: ""
        
        _state.value = _state.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                val billingResult = KtorClient.ApiServiceImpl.getUserBilling(
                    token = token,
                    limit = 10,
                    page = nextPage
                )
                
                if (billingResult.isSuccess) {
                    billingResult.getOrNull()?.let { billingResponse ->
                        if (billingResponse.code == 1) {
                            billingResponse.data?.let { data ->
                                _state.value = _state.value.copy(
                                    billings = _state.value.billings + data.list,
                                    currentPage = nextPage,
                                    totalPages = data.pagecount,
                                    isLoading = false
                                )
                            } ?: run {
                                _state.value = _state.value.copy(
                                    error = "加载更多失败: 数据为空",
                                    isLoading = false
                                )
                            }
                        } else {
                            _state.value = _state.value.copy(
                                error = billingResponse.msg ?: "加载更多失败",
                                isLoading = false
                            )
                        }
                    }
                } else {
                    _state.value = _state.value.copy(
                        error = billingResult.exceptionOrNull()?.message ?: "加载更多失败",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "网络错误: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
}