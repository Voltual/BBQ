// /app/src/main/java/cc/bbq/xq/ui/plaza/ResourcePlazaScreen.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.plaza

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cc.bbq.xq.AppStore
import cc.bbq.xq.data.unified.UnifiedAppItem
import cc.bbq.xq.data.unified.UnifiedCategory
import cc.bbq.xq.ui.compose.PageJumpDialog
import cc.bbq.xq.ui.compose.PaginationControls
import cc.bbq.xq.ui.theme.AppShapes
import cc.bbq.xq.ui.theme.AppStoreDropdownMenu
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import cc.bbq.xq.ui.theme.AppGrid
import cc.bbq.xq.ui.theme.AppGridItem

@Composable
fun ResourcePlazaScreen(
    isMyResourceMode: Boolean,
    mode: String = "public", // 新增模式参数
    navigateToAppDetail: (String, Long, String) -> Unit,
    userId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: PlazaViewModel = koinViewModel()
) {
    // 移除 LaunchedEffect，因为 ViewModel 已经负责状态恢复
    // LaunchedEffect(isMyResourceMode, userId, mode) {
    //     viewModel.initialize(isMyResourceMode, userId, mode)
    // }

    ResourcePlazaContent(
        modifier = modifier,
        viewModel = viewModel,
        isMyResourceMode = isMyResourceMode,
        navigateToAppDetail = navigateToAppDetail,
        userId = userId, // 传递 userId
        mode = mode // 传递 mode
    )
}

@Composable
fun ResourcePlazaContent(
    modifier: Modifier = Modifier,
    viewModel: PlazaViewModel,
    isMyResourceMode: Boolean,
    navigateToAppDetail: (String, Long, String) -> Unit,
    userId: String?, // 新增 userId 参数
    mode: String // 新增 mode 参数
) {
    val selectedAppStore by viewModel.appStore.observeAsState(AppStore.XIAOQU_SPACE)
    val categories by viewModel.categories.observeAsState(emptyList())
    val plazaState by viewModel.plazaData.observeAsState(PlazaData(emptyList()))
    val searchState by viewModel.searchResults.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val currentPage by viewModel.currentPage.observeAsState(1)
    val totalPages by viewModel.totalPages.observeAsState(1)
    val autoScrollMode by viewModel.autoScrollMode.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    // 新增：从 ViewModel 获取 currentCategoryId
    val currentCategoryId by viewModel.currentCategoryId.observeAsState()

    val isSearchMode by remember(searchState) { derivedStateOf { searchState.isNotEmpty() } }
    var searchQuery by remember { mutableStateOf("") }
    var showPageDialog by remember { mutableStateOf(false) }
    val dialogShape = remember { RoundedCornerShape(4.dp) }
    val gridState = rememberLazyGridState()
    val focusRequester = remember { FocusRequester() }

    val itemsToShow = if (isSearchMode) searchState else plazaState.popularApps

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            if (!autoScrollMode || isLoading || layoutInfo.visibleItemsInfo.isEmpty()) {
                false
            } else {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.last()
                val totalItemsCount = layoutInfo.totalItemsCount
                val hasMorePages = currentPage < totalPages
                totalItemsCount > 0 && hasMorePages && lastVisibleItem.index >= totalItemsCount - 5
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    // 新增：初始化 ViewModel（确保只执行一次）
    LaunchedEffect(isMyResourceMode, userId, mode) {
        viewModel.initialize(isMyResourceMode, userId, mode)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // 修正：仅在非“我的资源”模式下显示商店切换菜单
        if (!isMyResourceMode) {
            AppStoreDropdownMenu(
                selectedStore = selectedAppStore,
                onStoreChange = { viewModel.setAppStore(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 修正：仅在非“我的资源”模式下显示搜索框
        if (!isMyResourceMode) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .padding(top = 8.dp, bottom = 12.dp),
                shape = AppShapes.medium,
                label = { Text("搜索...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                     if (searchQuery.isNotEmpty()) {
                         IconButton(onClick = { 
                             searchQuery = ""
                             viewModel.cancelSearch()
                         }) {
                             Icon(Icons.Default.Clear, contentDescription = "Clear search")
                         }
                     }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search(searchQuery) }),
                singleLine = true
            )
        }

        // 分类标签
        if (isSearchMode) {
             Text(
                text = "搜索结果",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            CategoryTabs(
                categories = categories,
                // 传递 currentCategoryId
                selectedCategoryId = currentCategoryId,
                onCategorySelected = { viewModel.loadCategory(it) },
                enabled = true
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (itemsToShow.isNotEmpty()) {
                AppGrid(
                    apps = itemsToShow,
                    columns = if (isMyResourceMode) 4 else 3,
                    onItemClick = { app -> 
                        navigateToAppDetail(app.navigationId, app.navigationVersionId, app.store.name) 
                    },
                    gridState = gridState
                )
            } else if (!isLoading) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                } else {
                    Text(
                        text = if (isSearchMode) "未找到相关资源" else "暂无资源",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            if (isLoading) {
                if (itemsToShow.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    )
                }
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        PaginationControls(
            currentPage = currentPage,
            totalPages = totalPages,
            onPrevClick = { viewModel.prevPage() },
            onNextClick = { viewModel.nextPage() },
            onPageClick = { showPageDialog = true },
            isPrevEnabled = currentPage > 1 && !isLoading,
            isNextEnabled = currentPage < totalPages && !isLoading,
            extraControls = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("自动翻页", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = autoScrollMode,
                        onCheckedChange = { viewModel.setAutoScrollMode(it) }
                    )
                }
            }
        )
    }

    if (showPageDialog) {
        PageJumpDialog(
            currentPage = currentPage,
            totalPages = totalPages,
            shape = dialogShape,
            onDismiss = { showPageDialog = false },
            onConfirm = { page ->
                viewModel.goToPage(page)
                showPageDialog = false
            }
        )
    }
}

// 修改：CategoryTabs 接收 selectedCategoryId
@Composable
private fun CategoryTabs(
    categories: List<UnifiedCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    enabled: Boolean
) {
    // 使用 rememberUpdatedState 来捕获最新的 lambda
    val onCategorySelectedState = rememberUpdatedState(onCategorySelected)

    // 根据 currentCategoryId 计算 selectedTabIndex
    val selectedTabIndex = remember(categories, selectedCategoryId) {
        categories.indexOfFirst { it.id == selectedCategoryId }.takeIf { it != -1 } ?: 0
    }

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier.fillMaxWidth()
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = {
                    if (enabled) {
                        // 使用 rememberUpdatedState 捕获的最新 lambda
                        onCategorySelectedState.value(category.id)
                    }
                },
                text = { Text(category.name) },
                enabled = enabled
            )
        }
    }
}