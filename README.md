# 小趣空间第三方客户端 (BBQ)

![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7F52FF.svg?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6.8-4285F4.svg?logo=jetpackcompose)
![Version](https://img.shields.io/badge/Version-9.1-brightgreen.svg)

**小趣空间第三方客户端 (BBQ)** 是对原 iApp 版本 "小趣空间" 的一次彻底的现代化重构。本项目旨在利用当前主流的 Android 开发技术栈，为用户提供一个更流畅、更美观、功能更完善的社区和资源分享平台。

项目完全开源，遵循 **GNU General Public License v3.0** 协议。所有数据和资源均来源于官方平台 `app.xiaoqu.online`，本项目仅作为其第三方客户端。

## 技术特色 🚀

本次重构的核心是采用了一整套现代化的 Android 开发技术和架构模式，旨在提高开发效率、应用性能和代码可维护性。

- **现代架构 (MVVM)**: 项目严格遵循 **Model-View-ViewModel (MVVM)** 架构模式。通过 `ViewModel` 将业务逻辑与UI分离，利用 `Repository` 模式（如 `PlazaRepository`）封装数据源，使得代码结构清晰，职责分明。

- **100% 声明式UI (Jetpack Compose)**: 整个应用的 UI 层完全由 **Jetpack Compose** 构建。相较于传统的 XML 布局，Compose 提供了更直观、更灵活的 UI 开发体验。项目广泛使用了 Material 3 设计语言，实现了动态主题、组件化和响应式布局。

- **先进的并发模型 (Kotlin Coroutines)**: 所有异步操作，如网络请求、数据库访问，均通过 **Kotlin 协程** 在 `viewModelScope` 中进行管理。这确保了主线程的流畅性，避免了回调地狱，使异步代码如同步代码般易于读写。

- **响应式编程 (Kotlin Flow)**: `ViewModel` 中的数据状态通过 **StateFlow** 对外暴露，UI 层使用 `collectAsState` 进行订阅。这种响应式数据流确保了数据驱动UI的模式，当数据发生变化时，UI 会自动、高效地更新，保证了数据的一致性。

- **模块化设计**: 项目代码按功能模块进行组织，例如 `ui/community`、`ui/user`、`ui/plaza` 等，每个模块都包含其独立的 `Activity`、`ViewModel` 和 `Screen`。`data` 包负责本地数据持久化，`api` 包定义网络服务，结构清晰，易于扩展和维护。

## 功能特性 ✨

本项目不仅复刻了原版的核心功能，还在此基础上进行了扩展和优化。

- **社区系统**:
  - **帖子浏览**: 支持热点、关注、社区等多种模式的帖子列表，实现下拉刷新和滚动到底部自动加载更多。
  - **帖子详情**: 富文本内容展示，支持代码、链接、图片和视频的解析与交互。
  - **发帖与互动**: 支持创建图文并茂的帖子，实现点赞、评论、回复、删除、分享等全套互动功能。
  - **内容搜索**: 帖子搜索功能，并记录搜索历史。

- **资源广场**:
  - **应用市场**: 分类展示应用资源，支持搜索和分页浏览。
  - **应用详情**: 展示应用的详细信息、截图、版本历史和用户评论。
  - **应用发布与更新**: 提供完整的应用发布流程，包括 APK 解析（自动填充包名、版本等）、图标上传、介绍图上传、付费设置等。支持发布新应用和更新已有应用。
  - **退款申请**: 为已购买的用户提供便捷的退款申请流程。

- **用户中心**:
  - **账户体系**: 完整的登录、注册、自动登录和凭证管理。
  - **个人主页**: 展示用户头像、昵称、等级、硬币、关注/粉丝数等详细信息。
  - **关注与粉丝**: 查看关注列表和粉丝列表。
  - **个人资料编辑**: 支持修改昵称、QQ、设备名称和上传新头像。

- **高级播放器**:
  - **Bilibili 视频解析**: 内置播放器可直接解析和播放帖子中插入的 Bilibili 视频链接。
  - **弹幕支持**: 集成 `DanmakuFlameMaster`，支持实时弹幕渲染。
  - **高级控制**: 提供手势缩放、亮度/音量调节、播放模式切换、弹幕字号调整等高级功能。

- **深度主题定制**:
  - **动态颜色**: 允许用户自定义应用的完整色彩方案（日间/夜间），覆盖所有 Material 3 颜色。
  - **显示设置**: 支持调整全局 DPI 和字体缩放比例，满足不同用户的视觉需求。
  - **侧边栏美化**: 支持为日间和夜间模式分别设置侧边栏背景图片。

- **支付与经济系统**:
  - **应用购买**: 支持使用硬币购买付费应用。
  - **帖子打赏**: 用户可以对喜欢的帖子进行硬币打赏。
  - **账单中心**: 清晰展示所有硬币收支记录。

- **历史与记录**:
  - **浏览历史**: 自动记录用户浏览过的帖子，并提供批量管理功能（多选、反选、复制分享链接、删除）。
  - **我的喜欢**: 集中展示所有点赞过的帖子。

## 技术栈 📚

- **核心语言**: [Kotlin](https://kotlinlang.org/)
- **UI 框架**:
  - [Jetpack Compose](https://developer.android.com/jetpack/compose) (UI Toolkit)
  - [Material 3](https://m3.material.io/) (Design System)
- **架构**:
  - [MVVM (Model-View-ViewModel)](https://developer.android.com/jetpack/guide)
  - [Repository Pattern](https://developer.android.com/jetpack/guide/data-layer)
- **网络请求**:
  - [Retrofit2](https://square.github.io/retrofit/) (Type-safe HTTP client)
  - [OkHttp3](https://square.github.io/okhttp/) (HTTP client)
  - [Moshi](https://github.com/square/moshi) (JSON serialization)
- **异步编程**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **本地存储**: [Jetpack DataStore (Preferences)](https://developer.android.com/topic/libraries/architecture/datastore)
- **导航**: [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **图片加载**: [Coil (Coroutine Image Loader)](https://coil-kt.github.io/coil/)
- **视频播放**: [ijkplayer](https://github.com/bilibili/ijkplayer)
- **弹幕**: [DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster)
- **其他**:
  - [ImagePicker](https://github.com/Dhaval2404/ImagePicker) (图片选择与裁剪)
  - [PhotoView](https://github.com/chrisbanes/PhotoView) (图片手势缩放)

## 项目结构

项目采用清晰的、基于功能的模块化包结构，便于维护和扩展。