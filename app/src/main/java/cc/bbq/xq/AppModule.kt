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
import cc.bbq.xq.ui.user.MyCommentsViewModel

val appModule = module {
    // ViewModel definitions
    viewModel { LoginViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { BillingViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { CommunityViewModel() }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { FollowingPostsViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { HotPostsViewModel() }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { MyLikesViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { LogViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { MessageViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    
    // 修正：注入 repositories 参数
    viewModel { AppDetailComposeViewModel(androidApplication(), get()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewMode
    
    viewModel { AppReleaseViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    
    // PlazaViewModel
    viewModel { PlazaViewModel(androidApplication(), get()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    
    viewModel { PlayerViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { SearchViewModel() }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { UserListViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { PostCreateViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { MyPostsViewModel() }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { PaymentViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { UserDetailViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { StoreManagerViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    
    viewModel { BrowseHistoryViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { PostDetailViewModel(androidApplication()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { RankingListViewModel() }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { UpdateSettingsViewModel() }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { HomeViewModel() }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { VersionListViewModel(androidApplication(), get<SineShopRepository>()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel
    viewModel { MyCommentsViewModel(androidApplication(), get()) }//@KoinViewModelimport org.koin.core.annotation.KoinViewModel

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

