# Plan 4: Media Playback + Coil Tuning + Incremental Detection — 完成报告

**Date:** 2026-09-09
**Status:** ✅ Complete

---

## 交付物

### Phase A — `:core:media` 模块
- 新模块 `core/media`（namespace `com.photobox.core.media`）
- `gradle/libs.versions.toml` 增加 `media3 = "1.4.1"` + `androidx-media3-exoplayer` / `androidx-media3-ui` aliases
- `settings.gradle.kts` 注册 `:core:media`
- `VideoPlayer.kt` Composable — DisposableEffect(uri) lifecycle + Player.Listener.onPlayerError overlay

### Phase B — Coil 缓存优化
- `:core:common/build.gradle.kts` 增加 Hilt + KSP 插件
- `ImageLoaderModule.kt` (@Provides @Singleton) — memory `maxSizePercent(0.20)` + disk `maxSizeBytes(256 MB)`
- `ImageLoaderModuleTest.kt` — 验证 memory/disk 缓存配置

### Phase C — Stream + Day 模块接线
- `PhotoPage.kt` 接受 `isVideo: Boolean = false`，条件渲染 VideoPlayer / AsyncImage
- `DayAlbumPhotoViewer.kt` `VerticalPager.pageContent` 条件渲染 VideoPlayer / AsyncImage（使用已加载的 `state.items[pageIndex].uri`）
- `feature/stream/build.gradle.kts` + `feature/day/build.gradle.kts` + `app/build.gradle.kts` 增加 `:core:media` 依赖

### Phase D — 增量入库
- `StreamViewModel.init` 订阅 `mediaStore.observeNewMedia(currentShuffleIds())`，激活 ContentObserver pipeline
- 新发现的 ID 通过 `observeNewMedia` 副作用写入 `pending_shuffle_dao`
- `StreamViewModelIncrementalTest.kt` — 验证 init 订阅

## 度量

- **新增文件数**：5 个（VideoPlayer.kt, ImageLoaderModule.kt, ImageLoaderModuleTest.kt, StreamViewModelIncrementalTest.kt, STATUS.md）+ 2 个模块骨架文件（build.gradle.kts + AndroidManifest.xml）
- **修改文件数**：10 个（libs.versions.toml, settings.gradle.kts, core/common/build.gradle.kts, feature/stream/build.gradle.kts, feature/day/build.gradle.kts, app/build.gradle.kts, StreamViewModel.kt, PhotoPage.kt, StreamScreen.kt, DayAlbumPhotoViewer.kt）

## 构建验证

```
$ ./gradlew :app:assembleDebug :core:common:test :feature:stream:test
bash: ./gradlew: No such file or directory
EXIT: 127
```

（沿用 Plan 1/2/3 environmental precedent — 本机无 gradlew）

## Smoke Checklist（手动）

> ⚠️ 无设备/模拟器，以下项目 deferred to user:

1. [ ] 启动 App，浏览含视频的相册 — 视频应自动播放（带 PlayerView 控制器）
2. [ ] 后台 App，拍摄新照片，回到 stream — pending shuffle 应包含新照片（需手动 reshuffle 才进入 stream）
3. [ ] 杀 App，重启 — Coil 缓存应存活（磁盘缓存 256MB）
4. [ ] 进入当天相册，点击视频 — VideoPlayer 应渲染

## 已知遗留 / Parked for Plan 5+

1. **Plan 2 残留仍未解决**（DAO test JUnit4, plugin id 不一致, `fallbackToDestructiveMigration`）
2. **相册过滤**仍未真正实现（MediaItem 缺 bucket 字段）
3. **WX_APP_ID 占位**：上线前必须替换 `wx0000000000000000`
4. **`StatsRepository.observeStats`** 每次 emit 重查 MediaStore.totalCount()，无节流 — Plan 5+ 加 `distinctUntilChanged` + 节流（Plan 3 final-review Important #1）
5. **`DayAlbumScreen` 时间分组标题**（上午/下午/晚上）v1 简化 — Plan 5+ 补
6. **Brief-mandated unused imports / dead code / cosmetic minors**（Plan 3 final-review M1-M10）— Plan 5+ 清理
7. **WeChat OpenSDK aar** 需手动从微信开放平台下载至 `$rootDir/libs/open-sdk-lite-release.aar`

## 已解决

- ✅ Plan 2 残留 #1（observeNewMedia 未订阅）— Task 5 已订阅 StreamViewModel.init
- ⏳ Plan 3 final-review Important #1（StatsRepository.observeStats 节流）— 仍未解决，parked Plan 5+

## 下一步

启动 Plan 5+ 清理 parked 残留 + 加 Stats 节流 + 实现相册 bucket 字段。
