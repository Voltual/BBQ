// /app/src/main/java/cc/bbq/xq/ui/plaza/ResourcePlazaScreen.kt
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

@Composable
fun ResourcePlazaScreen(
    isMyResourceMode: Boolean,
    navigateToAppDetail: (String, Long, String) -> Unit, // 统一使用三个参数：ID, VersionID, StoreName
    userId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: PlazaViewModel = koinViewModel()
) {
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
    navigateToAppDetail: (String, Long, String) -> Unit
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        AppStoreDropdownMenu(
            selectedStore = selectedAppStore,
            onStoreChange = { viewModel.setAppStore(it) },
            modifier = Modifier.fillMaxWidth()
        )

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

        CategoryTabs(
            categories = categories,
            onCategorySelected = { viewModel.loadCategory(it) },
            enabled = !isSearchMode
        )

        Box(modifier = Modifier.weight(1f)) {
            val showEmptyState = itemsToShow.isEmpty() && !isLoading && errorMessage == null
            when {
                showEmptyState -> {
                    Text(
                        text = if (isSearchMode) "未找到相关资源" else "暂无资源",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                     Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                else -> {
                    AppGrid(
                        apps = itemsToShow,
                        columns = if (isMyResourceMode) 4 else 3,
                        onItemClick = { app -> 
                            navigateToAppDetail(app.navigationId, app.navigationVersionId, app.store.name) 
                        },
                        gridState = gridState
                    )
                }
            }

            if (isLoading && itemsToShow.isEmpty()) {
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

@Composable
private fun CategoryTabs(
    categories: List<UnifiedCategory>,
    onCategorySelected: (String?) -> Unit,
    enabled: Boolean
) {
    var selectedTabIndex by remember(categories) { mutableIntStateOf(0) }

    if (categories.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
        }
        return
    }
    
    LaunchedEffect(categories) {
        if(categories.isNotEmpty()) {
            selectedTabIndex = 0
        }
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
                        selectedTabIndex = index
                        onCategorySelected(category.id)
                    }
                },
                text = { Text(category.name) },
                enabled = enabled
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
            .heightIn(min = 120.dp),
        shape = AppShapes.medium
    ) {
        Column(
            modifier = Modifier.padding(4.dp).fillMaxHeight(),
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
                    .padding(bottom = 8.dp)
            )
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                minLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}