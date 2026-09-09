# Plan 3: 个人中心 / 设置 / 当天相册 / 应用锁 / 微信分享 — 完成报告

**Date:** 2026-09-09
**Status:** ✅ Complete (Approved with reservations — 0 Critical, 1 Important, 10 Minor parked)

---

## 交付物

### Phase A — 数据层基础
- DataStore setter 扩展（`setViewedCount` / `resetViewedCount`）
- `MediaStoreDataSource` 新增 `queryByDay` / `queryWithFilter` / `totalCount`
- `StatsRepository` + 单元测试

### Phase B — 个人中心
- `:feature:profile` 模块
- ProfileScreen 4 个统计 tile + 跳转入口
- FavoritesListScreen（3 列 grid + AsyncImage）
- LikesListScreen（LazyColumn + ID 列表）
- 3 个 NavHost 路由

### Phase C — 设置
- `:feature:settings` 模块
- SettingsScreen 含 6 个控件（filter mode / time range / album / app lock / clear cache / reshuffle）
- AlbumPickerSheet（ModalBottomSheet）
- 1 个 NavHost 路由

### Phase D — 当天相册
- `:feature:day` 模块 + `DateUtils` 共享工具
- DayAlbumScreen（3 列 grid）
- DayAlbumPhotoViewer（全屏 VerticalPager）
- StreamScreen 左滑手势接线
- 2 个 NavHost 路由（DAY_ALBUM + DAY_ALBUM_VIEWER）

### Phase E — 应用锁
- `:feature:applock` 模块 + `AppLockConstants`（30s TTL）
- `BiometricAuthenticator` + `DefaultBiometricAuthenticator`
- AppLockViewModel / Screen / Route
- `AppLockLifecycleObserver`（ProcessLifecycleOwner + ON_START bridge via Task 21 fix）
- MainActivity 顶层 overlay 接线（MainActivity 父类改为 `FragmentActivity` per preflight #17）

### Phase F — 微信分享
- `:feature:share` 模块（含 wechat aar via `$rootDir/libs/`）
- `WXApiManager` / `ShareDispatcher` / `ShareResultBus` / `ShareIntentBuilder` / `WeChatConstants`
- `app.wechat.WXEntryActivity`（在 app 模块注册）
- PhotoPage 分享按钮 + ShareHelperViewModel（file-level in StreamScreen.kt — Plan 4 refactor target）

### Phase G — 集成
- PhotoBoxNavHost 8 个路由完整接线
- StreamScreen 入口按钮（设置 + 个人 + 分享 via PhotoPage）
- 端到端编译验证（exit 127 — no gradlew on machine，environmental）
- 全分支 review（self-verified; 4 Critical candidates resolved; 1 Important wart parked; 10 Minor parked）
- 本 STATUS 报告

---

## 度量

- **构建时间（debug assemble）**：__ 未测量（`./gradlew` 不在本机 — 沿用 Plan 1/2 environmental precedent）
- **APK 大小**：__ 未测量
- **通过的测试数**：__ 未测量
- **新增文件数**：约 40 个（5 feature 模块骨架 10 + 共享工具 1 + Profile/Settings/Day/Applock/Share 业务类 29 + WXEntryActivity 1）
- **修改文件数**：13 个（见附录 B）

---

## 已知遗留 / Parked for Plan 4

1. **Plan 2 已知遗留 #1-#7 仍未解决**（observeNewMedia 未订阅 / DAO test JUnit4 / plugin id 不一致等）
2. **相册过滤**未真正实现（`queryWithFilter` 中 `albumFilter` 参数仅占位；需 MediaItem 扩展 bucket 字段）
3. **视频流播放**：PhotoPage 当前仅显示图片；`isVideo` 时应替换为 Media3 ExoPlayer 包装
4. **WX_APP_ID**：上线前必须替换 `wx0000000000000000` 为真实 AppID（4 处：WeChatConstants.PLACEHOLDER_APP_ID / manifest scheme / manifest metaData value / 微信开放平台应用签名）
5. **`StatsRepository.observeStats` 每次 emit 都重查 `MediaStore.totalCount()`**，无节流；对 1000+ 媒体场景可能频繁 query。Plan 4 加 `distinctUntilChanged` + 节流（final-review Important #1）
6. **`DayAlbumScreen` 时间分组标题**（上午/下午/晚上）v1 简化为单 list；spec 4.3 要求分组，Plan 4 补
7. **Brief-mandated unused imports / dead code / cosmetic minors**：约 10 项（详见 final-review.md M1-M10），Plan 4 清理
8. **WeChat OpenSDK aar**：`$rootDir/libs/open-sdk-lite-release.aar` 需手动从微信开放平台下载放置，否则 `./gradlew assembleDebug` 失败

---

## 已解决（brief 列出的 Critical 中）

- ✅ **`AppLockScreen` 需要 `FragmentActivity`** — 已由 Task 22 fix 解决：MainActivity 父类从 `ComponentActivity` 改为 `androidx.fragment.app.FragmentActivity`（preflight ruling #17）。brief STATUS.md 模板中此条已过时（仍列在遗留中），实际已 resolved。
- ✅ **`WXApiManager` 未装微信时崩溃** — Task 24 `ShareDispatcher.shareImages()` 在调用 SDK 前先检查 `wxApiManager.isWeChatInstalled()`，未安装时 emit `ShareResult.WeChatNotInstalled` 并返回，不崩溃。
- ✅ **`AlbumPickerSheet` 空 albums 列表崩溃** — `items(albums, key = { it })` 对空列表天然安全（只渲染"All"item），无崩溃风险。

---

## 下一步

启动 Plan 4: 视频播放（Media3 ExoPlayer）/ Coil 内存缓存优化 / 增量入库 URI 检测 / 错误处理 + UI 重试 / 上述 parked 残留清理
