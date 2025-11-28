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
import cc.bbq.xq.ui.community.BrowseHistoryViewModel
import cc.bbq.xq.ui.community.CommunityViewModel
import cc.bbq.xq.ui.community.FollowingPostsViewModel
import cc.bbq.xq.ui.community.HotPostsViewModel
import cc.bbq.xq.ui.community.MyLikesViewModel
import cc.bbq.xq.ui.community.PostCreateViewModel
import cc.bbq.xq.ui.community.PostDetailViewModel
import cc.bbq.xq.ui.home.HomeViewModel
import cc.bbq.xq.ui.log.LogViewModel
import cc.bbq.xq.ui.message.MessageViewModel
import cc.bbq.xq.ui.payment.PaymentViewModel
import cc.bbq.xq.ui.player.PlayerViewModel
import cc.bbq.xq.ui.plaza.AppDetailComposeViewModel
import cc.bbq.xq.ui.plaza.AppReleaseViewModel
import cc.bbq.xq.ui.plaza.PlazaViewModel
import cc.bbq.xq.ui.rank.RankingListViewModel
import cc.bbq.xq.ui.search.SearchViewModel
import cc.bbq.xq.ui.settings.storage.StoreManagerViewModel
import cc.bbq.xq.ui.settings.update.UpdateSettingsViewModel
import cc.bbq.xq.ui.user.MyPostsViewModel
import cc.bbq.xq.ui.user.UserDetailViewModel
import cc.bbq.xq.ui.user.UserListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // === ViewModel Definitions ===
    viewModel { LoginViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { CommunityViewModel(get()) }
    viewModel { MessageViewModel(get()) }
    viewModel { PlayerViewModel(get()) }
    viewModel { PostDetailViewModel(get()) }
    viewModel { UserDetailViewModel(get()) }
    viewModel { PostCreateViewModel(get()) }
    viewModel { MyLikesViewModel(get()) }
    viewModel { FollowingPostsViewModel(get()) }
    viewModel { HotPostsViewModel(get()) }
    viewModel { UserListViewModel(get()) }
    viewModel { RankingListViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { BrowseHistoryViewModel(get()) }
    viewModel { StoreManagerViewModel(get()) }
    viewModel { UpdateSettingsViewModel(get()) }
    viewModel { MyPostsViewModel(get()) }
    viewModel { LogViewModel(get()) }
    viewModel { BillingViewModel(get()) }
    viewModel { AppReleaseViewModel(get()) }
    viewModel { PaymentViewModel(get()) }
    viewModel { AppDetailComposeViewModel(get()) }

    // === NEW: PlazaViewModel Definition using Koin ===
    viewModel { PlazaViewModel(get(), get()) }

    // === NEW: Repository Definitions for Plaza ===

    // 提供 KtorClient 的 ApiService 实现
    // KtorClient.ApiServiceImpl 是一个 object, Koin 可以直接使用
    single { KtorClient.ApiServiceImpl }

    // 提供 XiaoQuRepository
    single { XiaoQuRepository(get()) }

    // 提供 SineShopRepository
    single { SineShopRepository() }

    // 提供 Repository Map，用于在 ViewModel 中根据 AppStore 类型动态切换
    single<Map<AppStore, IAppStoreRepository>> {
        mapOf(
            AppStore.XIAOQU_SPACE to get<XiaoQuRepository>(),
            AppStore.SIENE_SHOP to get<SineShopRepository>()
        )
    }
}