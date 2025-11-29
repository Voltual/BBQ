//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.gnu.org/licenses/>。
package cc.bbq.xq.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import cc.bbq.xq.AuthManager
import cc.bbq.xq.R
import cc.bbq.xq.data.DrawerMenuDataStore
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DrawerItem(
    val id: String,
    val label: String,
    val iconRes: Int,
    val route: String
)

@Composable
fun DrawerHeader(modifier: Modifier = Modifier, backgroundUri: String?) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (backgroundUri != null) {
            AsyncImage(
                model = backgroundUri,
                contentDescription = "Drawer Header Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun NavigationDrawerItems(
    navController: NavController,
    currentDestination: NavDestination?,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val context = LocalContext.current

    val allDrawerItems = remember {
        mutableListOf( // 修改为 mutableListOf
            DrawerItem("home", "首页", R.drawable.ic_menu_home, Home.route),
            DrawerItem("resources", "资源广场", R.drawable.ic_menu_apps, ResourcePlaza(isMyResource = false).createRoute()),
            DrawerItem("community", "交流社区", R.drawable.ic_menu_community, Community.route),
            DrawerItem("messages", "消息中心", R.drawable.ic_menu_message, MessageCenter.route),
            DrawerItem("ranking_list", "天梯竞赛", R.drawable.ic_menu_ranking, RankingList.route),
            DrawerItem("release_app", "发布应用", R.drawable.bg, CreateAppRelease.route),
            DrawerItem("bot_logs", "日志", R.drawable.work_log, LogViewer.route),
            DrawerItem("store_manager", "存储管理", R.drawable.appbackuprestore, StoreManager.route),
            // 新增：下载管理菜单项
            DrawerItem("download", "下载管理", R.drawable.dsdownload, Download.route),
            DrawerItem("update_settings", "更新设置", R.drawable.asusupdate, UpdateSettings.route),
            DrawerItem("settings", "主题设置", R.drawable.ic_menu_settings, ThemeCustomize.route),
            DrawerItem("logout", "退出登录", R.drawable.ic_menu_logout, "logout") // 特殊处理
        )
    }
    val allItemsMap = remember { allDrawerItems.associateBy { it.id } }

    var orderedItems by remember { mutableStateOf<List<DrawerItem>>(emptyList()) }
    var draggedItem by remember { mutableStateOf<DrawerItem?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var itemHeight by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val savedOrder = DrawerMenuDataStore.loadMenuOrder(context).first()
        orderedItems = if (savedOrder.isEmpty()) {
            allDrawerItems
        } else {
            val ordered = savedOrder.mapNotNull { allItemsMap[it] }
            val newItems = allDrawerItems.filter { it.id !in savedOrder }
            ordered + newItems
        }
    }

    val placeholderIndex by remember(draggedItem, dragOffsetY) {
        derivedStateOf {
            draggedItem?.let {
                val initialIndex = orderedItems.indexOf(it)
                val displacement = (dragOffsetY / itemHeight).toInt()
                (initialIndex + displacement).coerceIn(0, orderedItems.size - 1)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(orderedItems, key = { it.id }) { item ->
                val isBeingDragged = item.id == draggedItem?.id
                val index = orderedItems.indexOf(item)
                val showPlaceholder = placeholderIndex == index && placeholderIndex != orderedItems.indexOf(draggedItem)

                if (showPlaceholder) {
                    if (placeholderIndex!! > orderedItems.indexOf(draggedItem)) {
                        ItemContent(item, currentDestination, false, scope, drawerState, navController)
                        PlaceholderItem(modifier = Modifier.onSizeChanged { itemHeight = it.height })
                    } else {
                        PlaceholderItem(modifier = Modifier.onSizeChanged { itemHeight = it.height })
                        ItemContent(item, currentDestination, false, scope, drawerState, navController)
                    }
                } else {
                    ItemContent(
                        item = item,
                        currentDestination = currentDestination,
                        isDragged = isBeingDragged,
                        scope = scope,
                        drawerState = drawerState,
                        navController = navController,
                        modifier = Modifier
                            .onSizeChanged { itemHeight = it.height }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggedItem = item },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                    },
                                    onDragEnd = {
                                        placeholderIndex?.let { toIndex ->
                                            val fromIndex = orderedItems.indexOf(draggedItem!!)
                                            if (fromIndex != toIndex) {
                                                val newList = orderedItems.toMutableList().apply {
                                                    add(toIndex, removeAt(fromIndex))
                                                }
                                                orderedItems = newList
                                                scope.launch {
                                                    DrawerMenuDataStore.saveMenuOrder(context, newList.map { it.id })
                                                }
                                            }
                                        }
                                        draggedItem = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggedItem = null
                                        dragOffsetY = 0f
                                    }
                                )
                            }
                    )
                }
            }
        }

        draggedItem?.let { item ->
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = dragOffsetY
                        shadowElevation = 8f
                    }
                    .padding(horizontal = 12.dp)
            ) {
                ItemContent(item, currentDestination, false, scope, drawerState, navController)
            }
        }
    }
}

@Composable
private fun ItemContent(
    item: DrawerItem,
    currentDestination: NavDestination?,
    isDragged: Boolean,
    scope: CoroutineScope,
    drawerState: DrawerState,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    NavigationDrawerItem(
        label = { Text(item.label) },
        icon = {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        selected = currentDestination?.route == item.route,
        onClick = {
            scope.launch { drawerState.close() }
            when (item.id) {
                "logout" -> {
                    scope.launch {
                        AuthManager.clearCredentials(context)
                        navController.navigate(Home.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                else -> {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        // popUpTo(Home.route) // 可选：返回主页时清空堆栈
                    }
                }
            }
        },
        modifier = modifier
            .padding(vertical = 4.dp)
            .graphicsLayer { alpha = if (isDragged) 0f else 1f },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            unselectedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun PlaceholderItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {}
}