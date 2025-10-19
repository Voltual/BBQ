//AppModule.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq

import cc.bbq.xq.ui.auth.LoginViewModel
import cc.bbq.xq.ui.billing.BillingViewModel
import cc.bbq.xq.ui.community.CommunityViewModel
import cc.bbq.xq.ui.community.FollowingPostsViewModel
import cc.bbq.xq.ui.community.HotPostsViewModel
import cc.bbq.xq.ui.community.MyLikesViewModel
import cc.bbq.xq.ui.payment.PaymentViewModel
import cc.bbq.xq.ui.log.LogViewModel
import cc.bbq.xq.ui.user.UserListViewModel
import cc.bbq.xq.ui.message.MessageViewModel
import cc.bbq.xq.ui.plaza.AppDetailComposeViewModel
import cc.bbq.xq.ui.community.PostCreateViewModel
import cc.bbq.xq.ui.plaza.AppReleaseViewModel
import cc.bbq.xq.ui.plaza.PlazaViewModel
import cc.bbq.xq.ui.player.PlayerViewModel
import cc.bbq.xq.ui.search.SearchViewModel
import cc.bbq.xq.ui.user.MyPostsViewModel
import cc.bbq.xq.ui.user.UserDetailViewModel
import cc.bbq.xq.ui.settings.storage.StoreManagerViewModel // 导入 StoreManagerViewModel
import cc.bbq.xq.data.StorageSettingsDataStore // 导入 StorageSettingsDataStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.single
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // HttpClient instance
    single {
        HttpClient(OkHttp) {
            KtorClient.initConfig(this)
        }
    }

    // ApiService instance
    single<KtorClient.ApiService> {
        KtorClient.ApiServiceImpl(get()) // Inject HttpClient
    }

    // ViewModel definitions
    viewModel { LoginViewModel(androidApplication(), get()) } // Inject ApiService
    viewModel { BillingViewModel(androidApplication(), get()) } // Inject ApiService
    viewModel { CommunityViewModel() }
    viewModel { FollowingPostsViewModel(androidApplication(), get()) } // Inject ApiService
    viewModel { HotPostsViewModel(get()) } // Inject ApiService
    viewModel { MyLikesViewModel(androidApplication(), get()) } // Inject ApiService
    viewModel { LogViewModel(androidApplication()) }
    viewModel { MessageViewModel(androidApplication(), get()) } // Inject ApiService
    viewModel { AppDetailComposeViewModel(androidApplication(), get()) } // Inject ApiService
    viewModel { AppReleaseViewModel(androidApplication(), get()) } // Inject ApiService
    viewModel { (initialMode: Boolean) -> PlazaViewModel(androidApplication(), initialMode, get()) } // Inject ApiService
    viewModel { PlayerViewModel(androidApplication()) }
    viewModel { SearchViewModel(get()) } // Inject ApiService
    viewModel { UserListViewModel(androidApplication(), get()) } // Inject ApiService
    viewModel { PostCreateViewModel(androidApplication(), get()) } // Inject ApiService
    viewModel { MyPostsViewModel(get()) } // Inject ApiService
    viewModel { PaymentViewModel(androidApplication(), get()) } // Inject ApiService
    viewModel { UserDetailViewModel(androidApplication(), get()) } // Inject ApiService
    viewModel { StoreManagerViewModel(androidApplication()) } // 添加 StoreManagerViewModel

    // Singletons (if needed)
    single { AuthManager }
    //移除 RetrofitClient.instance
    //single { RetrofitClient.instance }
    single { BBQApplication.instance.database }
    single { BBQApplication.instance.processedPostsDataStore }
    single { BBQApplication.instance.searchHistoryDataStore }
    single { StorageSettingsDataStore(androidApplication()) } // 添加 StorageSettingsDataStore
    single {
    HttpClient(OkHttp) {
       defaultRequest {
            url(KtorClient.UPLOAD_BASE_URL)
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            })
        }
    }
}