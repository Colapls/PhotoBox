# PhotoBox 照片盲盒

PhotoBox 是一款纯本地运行的 Android 照片重发现应用。它把系统相册中的照片和视频随机排列，以类似短视频的方式逐张展示，让用户重新遇见自己可能已经忘记的回忆。

> 像开盲盒一样，重新发现自己的照片。

## 功能

- **随机照片流**：全屏展示本地照片和视频，上滑下一张、下滑上一张，浏览完一轮后重新洗牌。
- **点赞与收藏**：双击或点击按钮点赞，收藏照片后可在个人中心独立查看。
- **删除**：通过系统确认流程删除原文件，并同步清理点赞、收藏和随机队列中的引用。
- **当天相册**：从照片流左滑进入拍摄当天的全部媒体，支持网格浏览和全屏查看。
- **多选管理**：在当天相册中长按照片进入多选模式，可批量收藏、取消收藏或删除。
- **媒体播放**：支持普通图片、GIF 动图以及视频播放。
- **个人中心**：展示相册总数、已浏览数量、点赞数量和收藏数量。
- **内容筛选**：支持混合、仅照片、仅视频模式，以及多种时间范围筛选。
- **首次引导**：首次启动展示隐私说明、媒体权限申请和手势引导。

## 隐私设计

PhotoBox 不要求注册或登录，不上传照片、视频或个人数据。

- 媒体文件始终保留在设备本地。
- 点赞、收藏、筛选和浏览进度仅保存在本机。
- 应用只读取用户授权的照片、视频和媒体位置元数据。

## 环境要求

- Android 8.0（API 26）及以上
- JDK 17
- Android SDK 35
- Gradle 8.10.2

## 下载

从 [GitHub Releases](https://github.com/Colapls/PhotoBox/releases/latest) 下载预编译 APK：

- [PhotoBox-1.0.0.apk](https://github.com/Colapls/PhotoBox/releases/download/v1.0.0/PhotoBox-1.0.0.apk) — 正式版 1.0.0

下载后启用"未知来源应用"权限即可直接安装到 Android 8.0 及以上设备。

## 构建

克隆仓库后，在项目根目录执行：

```bash
# macOS / Linux
./gradlew :app:assembleDebug

# Windows
gradlew.bat :app:assembleDebug
```

调试包输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

运行单元测试：

```bash
./gradlew :core:common:testDebugUnitTest :feature:day:testDebugUnitTest
```

## 项目结构

```text
PhotoBox/
|-- app/                    # 应用入口、导航和依赖装配
|-- core/
|   |-- common/             # 通用调度器、日期和图片加载器
|   |-- data/               # MediaStore、Room、DataStore 和仓库
|   |-- media/              # 视频播放与媒体元数据 UI
|   `-- ui/                 # 主题和通用 UI
|-- feature/
|   |-- day/                # 当天相册、多选和全屏查看
|   |-- onboarding/         # 首次引导和权限申请
|   |-- profile/            # 个人中心、点赞和收藏列表
|   |-- settings/           # 内容与时间筛选、缓存和洗牌设置
|   `-- stream/             # 随机照片流
|-- build-logic/            # Gradle 约定插件
`-- docs/                   # 设计与实施文档
```

## 技术栈

- Kotlin 2.0.21
- Jetpack Compose + Material 3
- Navigation Compose
- Hilt
- Room
- DataStore Preferences
- MediaStore
- Media3 ExoPlayer
- Coil 3，包含 GIF 解码支持
- Kotlin Coroutines 和 Flow
- Gradle Kotlin DSL

## 使用方式

- 上滑或下滑：切换照片和视频。
- 双击：点赞或取消点赞。
- 左滑：进入当前照片拍摄当天的相册。
- 长按照片：进入多选模式。
- 底部按钮：点赞、收藏或删除当前媒体。

## 当前限制

- 相册选择入口已经存在，但指定相册尚未完整接入随机流筛选。
- 应用锁尚未实现。
- 部分视频和设备格式的兼容性取决于系统 MediaStore 与设备解码能力。

## 版本

当前版本：`1.0.0`
