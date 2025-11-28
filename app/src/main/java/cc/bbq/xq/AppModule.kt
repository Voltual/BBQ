//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq

import cc.bbq.xq.data.repository.IAppStoreRepository
import cc.bbq.xq.data.repository.SineShopRepository
import cc.bbq.xq.data.repository.XiaoQuRepository
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
import cc.bbq.xq.ui.settings.storage.StoreManagerViewModel 
import cc.bbq.xq.data.StorageSettingsDataStore 
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import cc.bbq.xq.ui.community.BrowseHistoryViewModel
import cc.bbq.xq.ui.community.PostDetailViewModel
import cc.bbq.xq.ui.rank.RankingListViewModel
import cc.bbq.xq.ui.settings.update.UpdateSettingsViewModel
import cc.bbq.xq.ui.home.HomeViewModel

val appModule = module {
    // ViewModel definitions - 保持原样，不要乱加参数
    viewModel { LoginViewModel(androidApplication()) }
    viewModel { BillingViewModel(androidApplication()) }
    viewModel { CommunityViewModel() } // 原来没有参数，保持没有参数
    viewModel { FollowingPostsViewModel(androidApplication()) }
    viewModel { HotPostsViewModel() } // 原来没有参数
    viewModel { MyLikesViewModel(androidApplication()) }
    viewModel { LogViewModel(androidApplication()) }
    viewModel { MessageViewModel(androidApplication()) }
    viewModel { AppDetailComposeViewModel(androidApplication()) }
    viewModel { AppReleaseViewModel(androidApplication()) }
    
    // 修改 PlazaViewModel 以支持新的 Repository 架构
    viewModel { PlazaViewModel(androidApplication(), get()) }
    
    viewModel { PlayerViewModel(androidApplication()) }
    viewModel { SearchViewModel() } // 原来没有参数
    viewModel { UserListViewModel(androidApplication()) }
    viewModel { PostCreateViewModel(androidApplication()) }
    viewModel { MyPostsViewModel() } // 原来没有参数
    viewModel { PaymentViewModel(androidApplication()) }
    viewModel { UserDetailViewModel(androidApplication()) }
    viewModel { StoreManagerViewModel(androidApplication()) }
    
    viewModel { BrowseHistoryViewModel(androidApplication()) }
    viewModel { PostDetailViewModel(androidApplication()) }
    viewModel { RankingListViewModel() } // 原来没有参数
    viewModel { UpdateSettingsViewModel() } // 原来没有参数
    viewModel { HomeViewModel() } // 原来没有参数

    // Singletons
    single { AuthManager }
    single { BBQApplication.instance.database }
    single { BBQApplication.instance.processedPostsDataStore }
    single { BBQApplication.instance.searchHistoryDataStore }
    single { StorageSettingsDataStore(androidApplication()) }

    // === 新增 Repository 定义 ===
    
    // XiaoQuRepository 需要 ApiService
    single { XiaoQuRepository(KtorClient.ApiServiceImpl) }

    // SineShopRepository
    single { SineShopRepository() }

    // Repository Map
    single<Map<AppStore, IAppStoreRepository>> {
        mapOf(
            AppStore.XIAOQU_SPACE to get<XiaoQuRepository>(),
            AppStore.SIENE_SHOP to get<SineShopRepository>()
        )
    }
}