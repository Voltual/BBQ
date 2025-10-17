// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.rank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RankingListState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val rankingList: List<RetrofitClient.models.RankingUser> = emptyList(),
    val page: Int = 1,
    val limit: Int = 15,
    val hasMore: Boolean = true
)

class RankingListViewModel : ViewModel() {

    private val _state = MutableStateFlow(RankingListState())
    val state: StateFlow<RankingListState> = _state

    init {
        loadRankingList()
    }

    fun loadRankingList() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = RetrofitClient.instance.getRankingList(
                    limit = _state.value.limit,
                    page = _state.value.page
                )
                if (response.isSuccessful) {
                    val rankingResponse = response.body()
                    if (rankingResponse?.code == 1) {
                        val rankingUserList = rankingResponse.data
                        _state.value = _state.value.copy(
                            isLoading = false,
                            rankingList = rankingUserList,
                            hasMore = rankingUserList.size == _state.value.limit
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = rankingResponse?.msg ?: "加载失败"
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "网络错误: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "加载异常: ${e.message}"
                )
            }
        }
    }

    fun refreshRankingList() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, error = null, page = 1)
            try {
                val response = RetrofitClient.instance.getRankingList(
                    limit = _state.value.limit,
                    page = 1
                )
                if (response.isSuccessful) {
                    val rankingResponse = response.body()
                    if (rankingResponse?.code == 1) {
                        val rankingUserList = rankingResponse.data
                        _state.value = _state.value.copy(
                            isRefreshing = false,
                            rankingList = rankingUserList,
                            hasMore = rankingUserList.size == _state.value.limit,
                            page = 1
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isRefreshing = false,
                            error = rankingResponse?.msg ?: "加载失败"
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        isRefreshing = false,
                        error = "网络错误: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    error = "加载异常: ${e.message}"
                )
            }
        }
    }

    fun loadNextRankingList() {
        if (!_state.value.hasMore || _state.value.isLoading) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val nextPage = _state.value.page + 1
                val response = RetrofitClient.instance.getRankingList(
                    limit = _state.value.limit,
                    page = nextPage
                )
                if (response.isSuccessful) {
                    val rankingResponse = response.body()
                    if (rankingResponse?.code == 1) {
                        val rankingUserList = rankingResponse.data
                        _state.value = _state.value.copy(
                            isLoading = false,
                            rankingList = _state.value.rankingList + rankingUserList,
                            hasMore = rankingUserList.size == _state.value.limit,
                            page = nextPage
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = rankingResponse?.msg ?: "加载失败"
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "网络错误: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "加载异常: ${e.message}"
                )
            }
        }
    }
}