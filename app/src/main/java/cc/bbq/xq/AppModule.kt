// /app/src/main/java/cc/bbq/xq/AppModule.kt
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
import cc.bbq.xq.ui.plaza.VersionListViewModel

val appModule = module {
    // ViewModel definitions
    viewModel { LoginViewModel(androidApplication()) }
    viewModel { BillingViewModel(androidApplication()) }
    viewModel { CommunityViewModel() }
    viewModel { FollowingPostsViewModel(androidApplication()) }
    viewModel { HotPostsViewModel() }
    viewModel { MyLikesViewModel(androidApplication()) }
    viewModel { LogViewModel(androidApplication()) }
    viewModel { MessageViewModel(androidApplication()) }
    
    // 修正：注入 repositories 参数
    viewModel { AppDetailComposeViewModel(androidApplication(), get()) }
    
    viewModel { AppReleaseViewModel(androidApplication()) }
    
    // PlazaViewModel
    viewModel { PlazaViewModel(androidApplication(), get()) }
    
    viewModel { PlayerViewModel(androidApplication()) }
    viewModel { SearchViewModel() }
    viewModel { UserListViewModel(androidApplication()) }
    viewModel { PostCreateViewModel(androidApplication()) }
    viewModel { MyPostsViewModel() }
    viewModel { PaymentViewModel(androidApplication()) }
    viewModel { UserDetailViewModel(androidApplication()) }
    viewModel { StoreManagerViewModel(androidApplication()) }
    
    viewModel { BrowseHistoryViewModel(androidApplication()) }
    viewModel { PostDetailViewModel(androidApplication()) }
    viewModel { RankingListViewModel() }
    viewModel { UpdateSettingsViewModel() }
    viewModel { HomeViewModel() }
    viewModel { VersionListViewModel(androidApplication(), get<SineShopRepository>()) }

    // Singletons
    single { AuthManager }
    single { BBQApplication.instance.database }
    single { BBQApplication.instance.processedPostsDataStore }
    single { BBQApplication.instance.searchHistoryDataStore }
    single { StorageSettingsDataStore(androidApplication()) }

    // Repositories
    single { XiaoQuRepository(KtorClient.ApiServiceImpl) }
    single { SineShopRepository() }
    single<Map<AppStore, IAppStoreRepository>> {
        mapOf(
            AppStore.XIAOQU_SPACE to get<XiaoQuRepository>(),
            AppStore.SIENE_SHOP to get<SineShopRepository>()
        )
    }
}