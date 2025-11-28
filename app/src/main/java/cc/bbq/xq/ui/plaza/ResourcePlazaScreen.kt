//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.plaza

import androidx.compose.animation.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.bbq.xq.AppStore
import cc.bbq.xq.data.unified.UnifiedAppItem
import cc.bbq.xq.data.unified.UnifiedCategory
import cc.bbq.xq.ui.compose.PageJumpDialog
import cc.bbq.xq.ui.compose.PaginationControls
import cc.bbq.xq.ui.theme.AppShapes
import cc.bbq.xq.ui.theme.AppStoreDropdownMenu
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.launch

@Composable
fun ResourcePlazaScreen(
    isMyResourceMode: Boolean,
    navigateToAppDetail: (String, Long) -> Unit,
    userId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: PlazaViewModel = viewModel(
        factory = PlazaViewModelFactory(context.applicationContext as android.app.Application)
    )

    // 当参数变化时，初始化ViewModel
    LaunchedEffect(isMyResourceMode, userId) {
        viewModel.initialize(isMyResourceMode, userId)
    }

    ResourcePlazaContent(
        modifier = modifier,
        viewModel = viewModel,
        isMyResourceMode = isMyResourceMode,
        navigateToAppDetail = navigateToAppDetail
    )
}

@Composable
fun ResourcePlazaContent(
    modifier: Modifier = Modifier,
    viewModel: PlazaViewModel,
    isMyResourceMode: Boolean,
    navigateToAppDetail: (String, Long) -> Unit
) {
    val selectedAppStore by viewModel.appStore.observeAsState(AppStore.XIAOQU_SPACE)
    val categories by viewModel.categories.observeAsState(emptyList())
    val plazaState by viewModel.plazaData.observeAsState(PlazaData(emptyList()))
    val searchState by viewModel.searchResults.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val currentPage by viewModel.currentPage.observeAsState(1)
    val totalPages by viewModel.totalPages.observeAsState(1)
    val autoScrollMode by viewModel.autoScrollMode.observeAsState(false)

    val isSearchMode by remember(searchState) { derivedStateOf { searchState.isNotEmpty() } }
    var searchQuery by remember { mutableStateOf("") }
    var showPageDialog by remember { mutableStateOf(false) }
    val dialogShape = remember { RoundedCornerShape(4.dp) }
    val gridState = rememberLazyGridState()
    val focusRequester = remember { FocusRequester() }
    
    val itemsToShow = if (isSearchMode) searchState else plazaState.popularApps
    
    // --- 自动翻页逻辑 ---
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            if (!autoScrollMode || isLoading || layoutInfo.visibleItemsInfo.isEmpty()) {
                false
            } else {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.last()
                val totalItemsCount = layoutInfo.totalItemsCount
                val hasMorePages = currentPage < totalPages
                totalItemsCount > 0 && hasMorePages && lastVisibleItem.index >= totalItemsCount - 3
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }
    // --- 自动翻页逻辑结束 ---

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // 应用商店切换菜单
        AppStoreDropdownMenu(
            selectedStore = selectedAppStore,
            onStoreChange = { viewModel.setAppStore(it) },
            modifier = Modifier.fillMaxWidth()
        )
        
        // 搜索框
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search(searchQuery) }),
                singleLine = true
            )
        }

        // 分类标签或搜索结果标题
        if (isSearchMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Text(
                    text = "搜索结果",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { 
                    searchQuery = ""
                    viewModel.cancelSearch() 
                }) {
                    Text("清除")
                }
            }
        } else {
            CategoryTabs(categories = categories, onCategorySelected = { viewModel.loadCategory(it) })
        }

        // 内容区域：网格或空状态
        Box(modifier = Modifier.weight(1f)) {
            if (itemsToShow.isEmpty() && !isLoading) {
                Text(
                    text = "暂无资源",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                AppGrid(
                    apps = itemsToShow,
                    columns = if (isMyResourceMode) 4 else 3,
                    onItemClick = { app -> navigateToAppDetail(app.navigationId, app.navigationVersionId) },
                    gridState = gridState
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        // 分页控制器
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

@Composable
private fun CategoryTabs(
    categories: List<UnifiedCategory>,
    onCategorySelected: (String?) -> Unit
) {
    var selectedTabIndex by remember(categories) { mutableIntStateOf(0) }

    if (categories.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
             Text("加载分类中...", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier.fillMaxWidth()
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = {
                    selectedTabIndex = index
                    onCategorySelected(category.id)
                },
                text = { Text(category.name) }
            )
        }
    }
}

@Composable
fun AppGrid(
    apps: List<UnifiedAppItem>,
    columns: Int,
    onItemClick: (UnifiedAppItem) -> Unit,
    gridState: LazyGridState
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        state = gridState
    ) {
        items(apps, key = { it.uniqueId }) { app ->
            AppGridItem(app, onClick = { onItemClick(app) })
        }
    }
}

@Composable
fun AppGridItem(
    app: UnifiedAppItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = AppShapes.medium
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(app.iconUrl)
                    .build(),
                contentDescription = app.name,
                modifier = Modifier
                    .size(56.dp)
                    .padding(8.dp)
            )
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}