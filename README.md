# 小趣空间第三方客户端 (OpenQu)

![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF.svg?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-4285F4.svg?logo=jetpackcompose)
![Android](https://img.shields.io/badge/Android-35-3DDC84.svg?logo=android)

**小趣空间第三方客户端 (OpenQu)** 是对原 iApp 版本 "小趣空间" 的一次彻底的现代化重构。本项目旨在利用当前主流的 Android 开发技术栈，为用户提供一个更流畅、更美观、功能更完善的社区和资源分享平台。

项目完全开源，遵循 **GNU General Public License v3.0** 协议。所有数据和资源均来源于官方平台 `app.xiaoqu.online`，本项目仅作为其第三方客户端。

## 🎯 技术特色 🚀

本次重构采用了一整套现代化的 Android 开发技术和架构模式，**所有依赖库均为截至 2025年11月23日 的最新稳定版本**，确保项目的技术先进性和长期可维护性。

- **现代架构 (MVVM)**: 项目严格遵循 **Model-View-ViewModel (MVVM)** 架构模式。通过 `ViewModel` 将业务逻辑与UI分离，利用 `Repository` 模式（如 `PlazaRepository`）封装数据源，使得代码结构清晰，职责分明。

- **100% 声明式UI (Jetpack Compose)**: 整个应用的 UI 层完全由 **Jetpack Compose** 构建，采用最新的 Compose BOM (2025.11.01) 管理版本。相较于传统的 XML 布局，Compose 提供了更直观、更灵活的 UI 开发体验。项目广泛使用了 Material 3 设计语言，实现了动态主题、组件化和响应式布局。

- **Kotlin 2.2.21 优先**: 全面拥抱 Kotlin 2.2.21 最新稳定版本，享受最新的语言特性和性能优化。

- **现代化网络层 (Ktor 3.3.2)**: 网络层采用 **Ktor Client 3.3.2** 替代了传统的 Retrofit。Ktor 提供了一流的协程支持，使得异步网络请求的编写如同同步代码一样简洁直观。配合 `kotlinx.serialization 1.9.0` 进行高效的 JSON 序列化/反序列化，不仅消除了 R8/ProGuard 混淆带来的潜在问题，还显著减小了最终 APK 的体积（从约 9MB 降至约 6.5MB），并为未来的 Kotlin Multiplatform (KMP) 扩展铺平了道路。

- **先进的并发模型 (Kotlin Coroutines 1.10.2)**: 所有异步操作，如网络请求、数据库访问，均通过 **Kotlin 协程** 在 `viewModelScope` 中进行管理。这确保了主线程的流畅性，避免了回调地狱，使异步代码如同步代码般易于读写。

- **响应式编程 (Kotlin Flow)**: `ViewModel` 中的数据状态通过 **StateFlow** 对外暴露，UI 层使用 `collectAsState` 进行订阅。这种响应式数据流确保了数据驱动UI的模式，当数据发生变化时，UI 会自动、高效地更新，保证了数据的一致性。

- **模块化设计**: 项目代码按功能模块进行组织，例如 `ui/community`、`ui/user`、`ui/plaza` 等，每个模块都包含其独立的 `ViewModel` 和 `Screen`。`data` 包负责本地数据持久化，结构清晰，易于扩展和维护。

## ✨ 功能特性

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

## 📚 技术栈

### 核心框架
- **Android Gradle Plugin**: 8.13.0 (最新稳定版)
- **Kotlin**: 2.2.21 (最新稳定版)
- **编译目标**: 
  - Compile SDK: 35
  - Target SDK: 35
  - Mini SDK: 21

### UI 框架
- [Jetpack Compose](https://developer.android.com/jetpack/compose) (UI Toolkit) - BOM 2025.11.01
- [Material 3](https://m3.material.io/) (Design System)
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) 2.9.6

### 架构与数据
- [MVVM (Model-View-ViewModel)](https://developer.android.com/jetpack/guide)
- [Repository Pattern](https://developer.android.com/jetpack/guide/data-layer)
- [Room Database](https://developer.android.com/training/data-storage/room) 2.7.2
- [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) 1.1.7

### 网络与序列化
- [Ktor Client](https://ktor.io/) 3.3.2 (Kotlin 原生异步 HTTP 客户端)
- [OkHttp](https://square.github.io/okhttp/) 5.3.2 (作为 Ktor 的引擎)
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) 1.9.0 (Kotlin 多平台序列化框架)
- [Protobuf](https://protobuf.dev/) 4.32.1

### 异步编程
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) 1.10.2
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html)

### 图片与媒体
- [Coil](https://coil-kt.github.io/coil/) 3.3.0 (图片加载)
- [ijkplayer](https://github.com/bilibili/ijkplayer) 0.0.2 (视频播放)
- [DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster) (弹幕支持)

### 工具库
- [KSP](https://github.com/google/ksp) 2.2.21-2.0.4 (注解处理)
- [ImagePicker](https://github.com/Dhaval2404/ImagePicker) 2.1 (图片选择与裁剪)
- [PhotoView](https://github.com/chrisbanes/PhotoView) 2.3.0 (图片手势缩放)

## 🚀 版本亮点

### 技术升级
- ✅ **所有依赖库均为最新稳定版本** (截至 2025年11月23日)
- ✅ Kotlin 2.2.21 完整支持
- ✅ Compose BOM 2025.11.01 统一管理
- ✅ Ktor 3.3.2 现代化网络层
- ✅ Room 2.7.2 数据库优化
- ✅ 全面移除 alpha/beta 依赖，确保稳定性

### 性能优化
- ✅ 构建工具链全面升级
- ✅ 编译时处理优化
- ✅ 包体积进一步优化
- ✅ 运行时性能提升

## 项目结构

项目采用清晰的、基于功能的模块化包结构，便于维护和扩展。

## 🙏 特别鸣谢

- 感谢同属 GPLv3 项目的哔哩终端，播放器部分借鉴了其 Java 代码
- 感谢同是开发第三方前端的如能的"三方小趣"的经验榜实现
- 部分图标使用了图标包 Whicons 中的图标
- 虽然 Whicons 图标包许可证是 CC BY-SA 4.0，但知识共享组织官方表示，CC BY-SA 4.0 是一个"兼容 GPLv3 的协议"
- 见 https://creativecommons.org/2015/10/08/cc-by-sa-4-0-now-one-way-compatible-with-gplv3/