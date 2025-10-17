// cc/bbq/xq/bot/ui/log/LogViewModel.kt

package cc.bbq.xq.ui.log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.BBQApplication
import cc.bbq.xq.data.db.LogEntry
import cc.bbq.xq.data.db.LogRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val logRepository = LogRepository()

    val logs: StateFlow<List<LogEntry>> = logRepository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- 新增：选择模式相关的状态 ---
    private val _selectedItems = MutableStateFlow<Set<Int>>(emptySet())
    val selectedItems: StateFlow<Set<Int>> = _selectedItems.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // 核心修正 #1: 让事件携带一个 Pair<String, Int>，分别代表要复制的文本和数量
    private val _copyEvent = MutableSharedFlow<Pair<String, Int>>()
    val copyEvent: SharedFlow<Pair<String, Int>> = _copyEvent.asSharedFlow()

    fun clearAllLogs() {
        viewModelScope.launch {
            logRepository.clearAllLogs()
            // 清空日志后，确保退出选择模式
            clearSelection()
        }
    }

    // --- 新增：选择操作的函数 ---

    fun toggleSelection(logId: Int) {
        val currentSelected = _selectedItems.value
        _selectedItems.value = if (currentSelected.contains(logId)) {
            currentSelected - logId
        } else {
            currentSelected + logId
        }
        // 如果选择集为空，自动退出选择模式
        if (_selectedItems.value.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun startSelectionMode(initialLogId: Int) {
        _isSelectionMode.value = true
        toggleSelection(initialLogId)
    }

    // 新增：无参数的 startSelectionMode 重载函数
    fun startSelectionMode() {
        _isSelectionMode.value = true
        // 不预先选择任何项目
    }

    fun selectAll() {
        _selectedItems.value = logs.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectionMode.value = false
    }

    fun invertSelection() {
        val allIds = logs.value.map { it.id }.toSet()
        _selectedItems.value = allIds - _selectedItems.value
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val idsToDelete = _selectedItems.value.toList()
            // 核心修正：调用 Repository 的公共方法，而不是直接访问 DAO
            logRepository.deleteLogsByIds(idsToDelete)
            clearSelection()
        }
    }

    fun copySelectedLogs() {
        viewModelScope.launch {
            val selectedIds = _selectedItems.value
            if (selectedIds.isEmpty()) return@launch

            val logsToCopy = logs.value.filter { it.id in selectedIds }

            val formattedLogs = logsToCopy.joinToString(separator = "\n\n=======\n\n") { log ->
                """
                [${log.formattedTime()}] [${log.type}] - ${log.status}
                
                [Request]
                ${log.requestBody}
                
                [Response]
                ${log.responseBody}
                """.trimIndent()
            }
            // 核心修正 #2: 发出带有正确数量的事件
            _copyEvent.emit(formattedLogs to logsToCopy.size)
        }
    }
}