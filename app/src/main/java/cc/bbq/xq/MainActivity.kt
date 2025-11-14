//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq

import android.app.ActivityOptions
import android.content.Context
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cc.bbq.xq.ui.theme.BBQTheme
import cc.bbq.xq.ui.theme.ThemeColorStore
import cc.bbq.xq.ui.theme.ThemeCustomizeScreen
import cc.bbq.xq.ui.theme.ThemeManager
import cc.bbq.xq.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import cc.bbq.xq.ui.CrashLogActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import cc.bbq.xq.data.db.LogEntry
import java.io.IOException // 导入 IOException
import cc.bbq.xq.data.UpdateInfo
import kotlinx.serialization.json.Json
import io.ktor.client.call.body
import cc.bbq.xq.ui.compose.UpdateDialog
import kotlinx.serialization.decodeFromString

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)               

        applyDpiAndFontScale(this)

        setContent {
            BBQTheme(appDarkTheme = ThemeManager.isAppDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainComposeApp()
                    // 检查更新
                    CheckForUpdates()
                }
            }
        }

        lifecycleScope.launch {
            delay(10000)
            val credentials = AuthManager.getCredentials(this@MainActivity)
            if (credentials != null) {
                startHeartbeatService(this@MainActivity, credentials.third)
            }
        }
    }
    
    init {
        // 设置崩溃处理
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val crashReport = getCrashReport(throwable)
            // 保存崩溃日志到数据库
            CoroutineScope(Dispatchers.IO).launch {
                val logEntry = LogEntry(
                    type = "CRASH",
                    requestBody = "MainActivity 崩溃",
                    responseBody = crashReport,
                    status = "FAILURE"
                )
                BBQApplication.instance.database.logDao().insert(logEntry)
            }.invokeOnCompletion {
                CrashLogActivity.start(BBQApplication.instance, crashReport)
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
    
    private fun getCrashReport(throwable: Throwable): String {
        val stackTrace = throwable.stackTraceToString()
        val deviceInfo = """
            设备型号: ${android.os.Build.MODEL}
            Android 版本: ${android.os.Build.VERSION.RELEASE}
            App 版本: ${BuildConfig.VERSION_NAME}
        """.trimIndent()

        return """
            崩溃信息: ${throwable.message}
            
            设备信息:
            $deviceInfo
            
            堆栈跟踪:
            $stackTrace
        """.trimIndent()
    }

    @Suppress("DEPRECATION")
    private fun applyDpiAndFontScale(context: Context) {
        val dpi = ThemeColorStore.loadDpi(context)
        val fontScale = ThemeColorStore.loadFontSize(context)
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        val metrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)
        val newDensityDpi = (dpi * DisplayMetrics.DENSITY_DEFAULT).toInt()
        configuration.densityDpi = newDensityDpi
        configuration.fontScale = fontScale
        metrics.densityDpi = newDensityDpi
        resources.updateConfiguration(configuration, metrics)
    }
}

@Composable
fun CheckForUpdates() {
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 后台线程执行网络请求
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = KtorClient.httpClient.get(URLBuilder("https://gitee.com/api/v5/repos/Voltula/bbq/releases/latest").build())
                if (response.status.isSuccess()) {
                    val responseBody = response.body<String>()
                    val update = Json.decodeFromString<UpdateInfo>(responseBody)

                    // 版本号比较
                    val currentVersion = BuildConfig.VERSION_NAME
                    val newVersion = update.tag_name.replace(Regex("[^\\d.]"), "") // 提取数字部分
                    if (newVersion > currentVersion) {
                        // 在主线程更新UI
                        withContext(Dispatchers.Main) {
                            updateInfo = update
                            showDialog = true
                        }
                    } else {
                        withContext(Dispatchers.Main){
                            Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main){
                        Toast.makeText(context, "检查更新失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main){
                    Toast.makeText(context, "检查更新出错: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 显示更新对话框
    if (showDialog && updateInfo != null) {
        UpdateDialog(updateInfo = updateInfo!!) {
            showDialog = false
        }
    }
}

// 移到此处，成为包级函数
fun restartMainActivity(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptions.makeCustomAnimation(
            context,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        context.startActivity(it, options.toBundle())
    }
}

fun startHeartbeatService(context: Context, token: String) {
    Intent(context, HeartbeatService::class.java).apply {
        putExtra("TOKEN", token)
        context.startService(this)
    }
}

private fun tryAutoLogin(
    username: String,
    password: String,
    context: Context,
    navController: NavController
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val deviceId = AuthManager.getDeviceId(context)
            val result = KtorClient.ApiServiceImpl.login(
                username = username,
                password = password,
                device = deviceId
            )

            withContext(Dispatchers.Main) {
                when {
                    result.isSuccess -> {
                        val loginResponse = result.getOrNull()
                        if (loginResponse != null && loginResponse.code == 1) {
                            val loginData = loginResponse.data
                            if (loginData != null) {
                                AuthManager.saveCredentials(
                                    context,
                                    username,
                                    password,
                                    loginData.usertoken,
                                    loginData.id
                                )
                                // 登录成功，不需要额外导航，因为已经在主页面
                            } else {
                                AuthManager.clearCredentials(context)
                                Toast.makeText(context, "登录数据为空", Toast.LENGTH_SHORT).show()
                                navController.navigate(Login.route)
                            }
                        } else {
                            AuthManager.clearCredentials(context)
                            val errorMsg = loginResponse?.msg ?: "登录失败"
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                            navController.navigate(Login.route)
                        }
                    }
                    result.isFailure -> {
                        AuthManager.clearCredentials(context)
                        val exception = result.exceptionOrNull()
                        val errorMsg = when (exception) {
                            is IOException -> {
                                when {
                                    exception.message?.contains("429") == true -> "请求太频繁"
                                    exception.message?.contains("500") == true -> "服务器错误"
                                    else -> "网络错误: ${exception.message}"
                                }
                            }
                            else -> "登录异常: ${exception?.message ?: "未知错误"}"
                        }
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        navController.navigate(Login.route)
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                AuthManager.clearCredentials(context)
                Toast.makeText(context, "登录异常: ${e.message}", Toast.LENGTH_SHORT).show()
                navController.navigate(Login.route)
            }
        }
    }
}

@Composable
fun getTitleForDestination(backStackEntry: NavBackStackEntry?): String {
    val route = backStackEntry?.destination?.route
    val isMyResource = backStackEntry?.arguments?.getBoolean(AppDestination.ARG_IS_MY_RESOURCE) ?: false
    val userId = backStackEntry?.arguments?.getLong(AppDestination.ARG_USER_ID, -1L) ?: -1L

    val routeBase = route?.substringBefore("?")?.substringBefore("/")

    return when (routeBase) {
        Home.route -> "首页"
        Login.route -> "登录"
        "plaza" -> {
            if (userId != -1L) "Ta的资源"
            else if (isMyResource) "我的资源"
            else "资源广场"
        }
        RankingList.route -> "天梯竞赛"
        MessageCenter.route -> "消息中心"
        BrowseHistory.route -> "浏览历史"
        Billing.route -> "账单"
        ThemeCustomize.route -> "主题设置"
        Search.route -> "搜索"
        "post_detail" -> "帖子详情"
        "user_detail" -> "用户详情"
        "my_posts" -> "我的帖子"
        CreatePost.route -> "创建新帖"
        "create_refund_post" -> "申请退币"
        CreateAppRelease.route -> "发布应用"
        "app_release_update" -> "更新应用"
        LogViewer.route -> "日志"
        AccountProfile.route -> "账号资料"
        FollowList.route -> "我的关注"
        FanList.route -> "我的粉丝"
        MyLikes.route -> "我喜欢的"
        HotPosts.route -> "热点"
        FollowingPosts.route -> "关注的人"
        Community.route -> "社区"
        "payment_app" -> "应用购买"
        "payment_post" -> "帖子打赏"
        PaymentCenterAdvanced.route -> "高级支付"
        "player" -> "视频播放"
        About.route -> "关于"
        "image_preview" -> "图片预览"
        StoreManager.route -> "存储管理"
        "app_detail" -> "应用详情"
        else -> "BBQ"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainComposeApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val context = LocalContext.current

    // 判断是否显示返回按钮：不在首页且不在登录页时显示
    val showBackButton = remember(currentBackStackEntry) {
        val route = currentBackStackEntry?.destination?.route
        route != Home.route && route != Login.route
    }

    // 新增：判断是否为社区相关屏幕
    val isCommunityScreen = remember(currentBackStackEntry) {
        val route = currentBackStackEntry?.destination?.route
        val routeBase = route?.substringBefore("?")?.substringBefore("/")

        routeBase == Community.route ||
                routeBase == MyLikes.route ||
                routeBase == HotPosts.route ||
                routeBase == FollowingPosts.route ||
                routeBase == "my_posts" // 新增：我的帖子也属于社区屏幕
    }

    // 新增：判断是否为播放器屏幕
    val isPlayerScreen = remember(currentBackStackEntry) {
        val route = currentBackStackEntry?.destination?.route
        val routeBase = route?.substringBefore("?")?.substringBefore("/")
        routeBase == "player" // 播放器路由
    }

    // 在 NavHost 外部设置回调，因为它与 MainActivity 的生命周期相关
    var restartAppCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        restartAppCallback = { restartMainActivity(context) }
    }

    val useDarkTheme = ThemeManager.isAppDarkTheme

    val lightBgUri by ThemeColorStore.getDrawerHeaderLightBackgroundUriFlow(context).collectAsState(initial = null)
    val darkBgUri by ThemeColorStore.getDrawerHeaderDarkBackgroundUriFlow(context).collectAsState(initial = null)

    val drawerHeaderBackgroundUri = if (useDarkTheme) darkBgUri else lightBgUri

    LaunchedEffect(Unit) {
        val credentials = AuthManager.getCredentials(context)
        if (credentials != null) {
            tryAutoLogin(credentials.first, credentials.second, context, navController)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Box(modifier = Modifier.width(360.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DrawerHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        backgroundUri = drawerHeaderBackgroundUri
                    )

                    NavigationDrawerItems(
                        navController = navController,
                        currentDestination = currentBackStackEntry?.destination,
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            }
        },
        gesturesEnabled = true,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                // 播放器屏幕：完全隐藏 MainActivity 顶栏
                // 社区屏幕：隐藏 MainActivity 顶栏
                // 其他屏幕：显示 MainActivity 顶栏
                if (!isPlayerScreen && !isCommunityScreen) {
                    TopAppBar(
                        title = {
                            Text(
                                text = getTitleForDestination(currentBackStackEntry),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            if (showBackButton) {
                                // 返回按钮
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                // 菜单按钮
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = stringResource(R.string.open_drawer),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        actions = {
                            if (currentBackStackEntry?.destination?.route != Login.route) {
                                IconButton(onClick = { navController.navigate(Search.route) }) {
                                    Icon(Icons.Default.Search, "搜索", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { navController.navigate(CreatePost.route) }) {
                                    Icon(Icons.Default.Add, "发帖", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { navController.navigate(BrowseHistory.route) }) {
                                    Icon(Icons.Default.History, "浏览历史", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            content = { innerPadding ->
                // 为播放器和社区屏幕应用不同的内边距（无顶栏时使用 0.dp）
                val contentPadding = when {
                    isPlayerScreen || isCommunityScreen -> PaddingValues(0.dp)
                    else -> innerPadding
                }

                AppNavHost(
                    navController = navController,
                    modifier = Modifier.padding(contentPadding)//,
//                    restartAppCallback = restartAppCallback
                )
            }
        )
    }
}