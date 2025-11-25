//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.plaza

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cc.bbq.xq.ui.compose.PageJumpDialog
import cc.bbq.xq.ui.compose.PaginationControls
import cc.bbq.xq.ui.theme.AppShapes
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import cc.bbq.xq.AppStore
import cc.bbq.xq.ui.theme.AppStoreDropdownMenu
import cc.bbq.xq.KtorClient
import kotlinx.coroutines.launch

@Composable
fun ResourcePlazaScreen(
    viewModel: PlazaViewModel,
    isMyResourceMode: Boolean,
    navigateToAppDetail: (String, Long) -> Unit,
//    navController: NavController,
    userId: Long? = null,
    modifier: Modifier = Modifier
) {
    ResourcePlazaContent(
        modifier = modifier,
        viewModel = viewModel,
        isMyResourceMode = isMyResourceMode,
        userId = userId,
        navigateToAppDetail = navigateToAppDetail
    )
}

@Composable
fun ResourcePlazaContent(
    modifier: Modifier = Modifier,
    viewModel: PlazaViewModel,
    isMyResourceMode: Boolean,
    userId: Long? = null,
    navigateToAppDetail: (String, Long) -> Unit
) {
    // 修复：使用简单的 rememberSaveable 而不需要自定义 Saver
    var selectedCategoryIndex by rememberSaveable { mutableStateOf(0) }
    val selectedAppStore by viewModel.appStore.observeAsState(AppStore.XIAOQU_SPACE)

    val plazaState by viewModel.plazaData.observeAsState(PlazaData(emptyList()))
    val searchState by viewModel.searchResults.observeAsState(emptyList<AppItem>())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val currentPage by viewModel.currentPage.observeAsState(1)
    val totalPages by viewModel.totalPages.observeAsState(1)
    val autoScrollMode by viewModel.autoScrollMode.observeAsState(false)
    val isSearchMode by remember { derivedStateOf { searchState.isNotEmpty() } }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var showPageDialog by remember { mutableStateOf(false) }
    var showPagination by rememberSaveable { mutableStateOf(true) }
    val dialogShape = remember { RoundedCornerShape(4.dp) }
    val gridState = rememberLazyGridState()
    val appTagList by viewModel.appTagList.observeAsState(emptyList()) // 弦应用商店标签

    val categories = remember(selectedAppStore) {
        if (selectedAppStore == AppStore.XIAOQU_SPACE) {
            listOf(
                AppCategory(null, null, "最新分享"),
                AppCategory(45, 47, "影音阅读"),
                AppCategory(45, 55, "音乐听歌"),
                AppCategory(45, 61, "休闲娱乐"),
                AppCategory(45, 58, "文件管理"),
                AppCategory(45, 59, "图像摄影"),
                AppCategory(45, 53, "输入方式"),
                AppCategory(45, 54, "生活出行"),
                AppCategory(45, 50, "社交通讯"),
                AppCategory(45, 56, "上网浏览"),
                AppCategory(45, 60, "其他类型"),
                AppCategory(45, 62, "跑酷竞技")
            )
        } else {
            // 将 SineShopClient.AppTag 转换为 AppCategory
           appTagList.map { AppCategory(it.id, null, it.name) }
        }
    }
    val selectedCategory = categories.getOrNull(selectedCategoryIndex) ?: AppCategory(null, null, "暂无分类")

    // 修复：使用 derivedStateOf 跟踪分类变化，避免不必要的重组
    val currentCategory by remember(selectedCategory) {
        derivedStateOf { selectedCategory }
    }

    // 修复：简化的初始化，只在模式或用户ID变化时重新初始化
    LaunchedEffect(isMyResourceMode, userId, selectedAppStore) {
        viewModel.initializeData(isMyResourceMode, userId)
    }

    // 修复：分类变化时正确调用 ViewModel 方法
    LaunchedEffect(currentCategory, selectedAppStore) {
        if (!isSearchMode) { // 只有在非搜索模式下才响应分类变化
            viewModel.loadDataByCategory(
                categoryId = currentCategory.categoryId, 
                subCategoryId = currentCategory.subCategoryId, 
                userId = userId
            )
        }
    }

    // 修复：搜索模式变化时重置分类选择
    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            // 搜索模式下保持当前分类选择，但不加载分类数据
        } else {
            // 退出搜索模式时重新加载当前分类数据
            viewModel.loadDataByCategory(
                categoryId = currentCategory.categoryId,
                subCategoryId = currentCategory.subCategoryId,
                userId = userId
            )
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
                label = { Text("搜索资源...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (searchQuery.isNotBlank()) {
                        viewModel.searchResources(searchQuery, isMyResourceMode)
                    }
                }),
                singleLine = true
            )
        }

        if (isSearchMode) {
            Text(
                text = if (isMyResourceMode) "搜索结果（我的资源）" else "搜索结果",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            // 修复：使用 PrimaryScrollableTabRow 替代弃用的 ScrollableTabRow
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { 
                            selectedCategoryIndex = index
                            // 立即更新分类，不等待 LaunchedEffect
                            viewModel.loadDataByCategory(
                                categoryId = category.categoryId,
                                subCategoryId = category.subCategoryId,
                                userId = userId
                            )
                        },
                        text = { Text(category.categoryName) }
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if ((isSearchMode && searchState.isEmpty()) || (!isSearchMode && plazaState.popularApps.isEmpty())) {
                Text(
                    text = "暂无资源",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                AppGrid(
                    apps = if (isSearchMode) searchState else plazaState.popularApps,
                    columns = if (isMyResourceMode) 4 else 3,
                    onItemClick = { app -> navigateToAppDetail(app.id, app.versionId) },
                    gridState = gridState
                )
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            var lastVisibleItemIndex by remember { mutableStateOf(gridState.firstVisibleItemIndex) }
            LaunchedEffect(gridState.firstVisibleItemIndex) {
                if (gridState.firstVisibleItemIndex > lastVisibleItemIndex) {
                    showPagination = false
                } else if (gridState.firstVisibleItemIndex < lastVisibleItemIndex) {
                    showPagination = true
                }
                lastVisibleItemIndex = gridState.firstVisibleItemIndex
            }

            val shouldLoadMore by remember {
                derivedStateOf {
                    if (!autoScrollMode || isLoading || currentPage >= totalPages) {
                        false
                    } else {
                        val layoutInfo = gridState.layoutInfo
                        val visibleItemsInfo = layoutInfo.visibleItemsInfo
                        if (visibleItemsInfo.isEmpty()) return@derivedStateOf false
                        val lastVisibleItem = visibleItemsInfo.last()
                        val totalItemsCount = layoutInfo.totalItemsCount
                        lastVisibleItem.index >= totalItemsCount - 4
                    }
                }
            }

            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore) {
                    viewModel.loadMore(isSearchMode)
                }
            }
        }

        AnimatedVisibility(
            visible = showPagination,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            PaginationControls(
                currentPage = currentPage,
                totalPages = totalPages,
                onPrevClick = { if (isSearchMode) viewModel.searchPrevPage() else viewModel.prevPage() },
                onNextClick = { if (isSearchMode) viewModel.searchNextPage() else viewModel.nextPage() },
                onPageClick = { showPageDialog = true },
                isPrevEnabled = currentPage > 1 && !isLoading,
                isNextEnabled = currentPage < totalPages && !isLoading,
                extraControls = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "自动翻页",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Switch(
                            checked = autoScrollMode,
                            onCheckedChange = { viewModel.setAutoScrollMode(it) }
                        )
                    }
                }
            )
        }
    }

    if (showPageDialog) {
        PageJumpDialog(
            currentPage = currentPage,
            totalPages = totalPages,
            shape = dialogShape,
            onDismiss = { showPageDialog = false },
            onConfirm = { page ->
                if (isSearchMode) viewModel.searchGoToPage(page) else viewModel.goToPage(page)
                showPageDialog = false
            }
        )
    }
}

@Composable
fun AppGrid(
    apps: List<AppItem>,
    columns: Int,
    onItemClick: (AppItem) -> Unit,
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
            AppGridItem(app, onItemClick)
        }
    }
}

@Composable
fun AppGridItem(
    app: AppItem,
    onClick: (AppItem) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val selectedAppStore by remember { mutableStateOf(AppStore.XIAOQU_SPACE) } // 假设默认是小趣空间

    Card(
        onClick = {
            if (selectedAppStore == AppStore.XIAOQU_SPACE) {
                onClick(app)
            } else {
                // TODO: Implement SineShop App Detail Navigation
                scope.launch {
                    snackbarHostState.showSnackbar("弦应用商店应用详情功能暂未实现")
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant, // 使用 surfaceVariant 背景色
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(app.iconUrl)
//                    .crossfade(true)
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
