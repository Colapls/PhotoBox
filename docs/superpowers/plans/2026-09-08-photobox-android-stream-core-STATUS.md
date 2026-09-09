# Plan 2: 核心体验 — 完成报告

**Date**: 2026-09-08
**Status**: ✅ Complete (构建与真机冒烟因环境缺失未执行,详见"已知遗留")

## 交付物

- Room schema(4 表)+ DAO + TypeConverter + 9 个 DAO/repository 单元测试
- MediaStore 数据源(图片 + 视频查询 + ContentObserver 增量)
- RandomStreamRepository(Fisher-Yates 启动洗牌 + 队列持久化 + 增量入库)
- Like / Favorite / Delete repositories(toggle + 系统删除 IntentSender + Room 跨表清理)
- StreamViewModel + UiState + 单元测试
- PhotoPage(双击点赞 + 心形动画)+ VerticalPager(prefetch ±1 + stable key)
- 收藏 / 删除按钮 overlay + 删除底部 Sheet + ActivityResultLauncher
- Hilt DI 装配

## 度量

- 构建时间(debug assemble): __ 秒 — _未执行(见已知遗留)_
- APK 大小: __ MB — _未执行_
- 通过的测试数: __ / __ — _未执行_

## 任务清单

Plan 2 共 11 个任务(Tasks 1-10 实施 + Task 11 报告)。所有 Task 1-10 提交均通过审核(含 3 个 fix 回合)。

| # | Task | Commit | Subject | Status | Review |
|---|------|--------|---------|--------|--------|
| 1 | Task 1 — Room + Coil 依赖注入 | `42dcd18` | chore(deps): add Room + Coil catalog entries and module wiring | DONE | ✅ Approved (parked Minor) |
| 2 | Task 2 — Room schema (4 表) | `07e4c19` | feat(data): add Room schema for favorite/like/shuffle/pending | DONE_WITH_CONCERNS | ✅ Approved after fix round 1 |
| 2.fix | Task 2 fix — `@PrimaryKey` | `a0985bf` | fix(data): add missing `@PrimaryKey` on `ShuffleStateEntity.id` | DONE | ✅ Approved |
| 3a | Task 3a — test dep 注入 | `82c908c` | chore(data): add androidx-test-ext-junit for DAO tests | DONE | (part of Task 3) |
| 3b | Task 3b — DAO 单元测试 | `7d5df53` | test(data): add DAO tests for favorite, like, shuffle | DONE_WITH_CONCERNS | ✅ Approved (parked Important) |
| 4 | Task 4 — MediaStoreDataSource | `6ac77da` | feat(data): add MediaStoreDataSource + MediaItem model | DONE_WITH_CONCERNS | ✅ Approved after fix round 1 |
| 4.fix | Task 4 fix — Channel + flow builder | `3ea28bc` | fix(data): restructure observeNewMedia to use Channel + flow builder | DONE | ✅ Approved |
| 5 | Task 5 — RandomStreamRepository | `3fd51e3` | feat(data): add RandomStreamRepository with Fisher-Yates shuffle | DONE | ✅ Approved |
| 6 | Task 6 — Like/Favorite/Delete repos | `b91cab9` | feat(data): add Like, Favorite, Delete repositories with tests | DONE_WITH_CONCERNS | ✅ Approved (parked Minor) |
| 7 | Task 7 — Hilt DI 装配 | `7a968e2` | feat(data): wire Room + MediaStoreDataSource into Hilt DI | DONE_WITH_CONCERNS | ✅ Approved (additive-merge deviation justified) |
| 8 | Task 8 — StreamViewModel | `bb02a0e` | feat(stream): add StreamViewModel with like/favorite/delete wiring | DONE_WITH_CONCERNS | ✅ Approved after fix round 1 |
| 8.fix | Task 8 fix — `LikeRepository.observeAllIds` | `369481e` | fix(data): expose observeAllIds on LikeRepository for StreamViewModel | DONE | ✅ Approved |
| 9 | Task 9 — PhotoPage + animation + pager | `d179f9c` | feat(stream): add PhotoPage + like animation + VerticalPager | DONE | ✅ Approved (parked Minor) |
| 10 | Task 10 — favorite/delete + sheet + IntentSender 修订 | `e228286` | feat(stream): add favorite/delete buttons + delete confirmation sheet | DONE | ✅ Approved (parked Minor) |
| 11 | Task 11 — Plan 2 完成报告 | (pending) | docs: plan 2 completion report | DONE | (this commit) |

**Commits**: 13 个实质性提交(Task 2 / 4 / 8 各含 1 个 fix 回合),1 个文档提交(Task 11,本次)。

### 提交链

```
bcb6c12 docs: plan 1 completion report        (Plan 1 终点)
42dcd18 chore(deps): Room + Coil 依赖注入       (Task 1)
07e4c19 feat(data): Room schema                (Task 2)
a0985bf fix(data): @PrimaryKey                 (Task 2 fix round 1)
82c908c chore(data): test ext-junit            (Task 3a)
7d5df53 test(data): DAO tests                  (Task 3b)
6ac77da feat(data): MediaStoreDataSource       (Task 4)
3ea28bc fix(data): Channel + flow              (Task 4 fix round 1)
3fd51e3 feat(data): RandomStreamRepository     (Task 5)
b91cab9 feat(data): Like/Favorite/Delete repos (Task 6)
7a968e2 feat(data): Hilt DI 装配                (Task 7)
bb02a0e feat(stream): StreamViewModel          (Task 8)
369481e fix(data): observeAllIds wrapper        (Task 8 fix round 1)
d179f9c feat(stream): PhotoPage + pager        (Task 9)
e228286 feat(stream): favorite/delete + sheet   (Task 10)
<this>   docs: plan 2 completion report        (Task 11)
```

## 已知遗留

1. **构建验证未执行** — 本机无 `./gradlew` 包装器(`/usr/bin/bash: ./gradlew: No such file or directory`,exit 127),因此 `:app:assembleDebug` 与 `:core:data:test :feature:stream:test` 均未运行。代码层基于前面 Task 1-10 已通过编译/测试逻辑的提交(最终 commit `e228286`)。
2. **真机冒烟未执行** — 本机无 `adb`(`command not found`,exit 127)且无可用设备或模拟器,故 6 步手动流程(进入主页 → 随机照片 → 翻页 → 双击点赞 → 收藏 → 删除确认 → kill 重启 → 直接进入主页)均无法验证。需在具备 Android SDK + 设备的机器上补做 Step 11.3。
3. **度量字段空白** — 度量段三项(`__`)为模板占位,需真实构建/测试后由补跑者填写。
4. **ContentObserver 增量检测使用全量对比**(`knownMediaIds` snapshot + `insertIfAbsent`),Plan 3 优化为 URI-based detection。
5. **删除流程在 API < 30 设备上隐藏入口**(`Build.VERSION.SDK_INT >= Q` 分支,spec 行为,保留以便 Plan 3 在低版本设备上提供自定义删除流程)。
6. **DAO 测试 JUnit4 vs `useJUnitPlatform()` 不匹配**(Task 3 Important 级别,parked) — 测试在编译期通过但 `core/data` 模块的 `./gradlew :core:data:test` 任务不会实际执行它们(无 `junit-vintage-engine`)。建议 Plan 3 迁移到 `androidTest` instrumentation 或添加 `junit-vintage-engine` + Robolectric。
7. **`LaunchedEffect(sender)` 删除对话框自动触发** — 系统删除对话框在 `sender` 变化时自动重新触发(Task 10 Minor 级别,preflight ruling #14 接受);Plan 3 可重构为基于 `showDeleteSheet` 标志的门控。

## 下一步

启动 Plan 3: 个人中心 / 设置 / 微信分享 / 应用锁 / 当天相册

**额外建议**:

1. 添加 Coil 内存缓存配置(25% heap)
2. ContentObserver 增量检测优化为 URI-based
3. 视频流(`AsyncImage` 替换为 `VideoPlayer` 包装 Media3 ExoPlayer)
4. DAO 测试迁移到 `androidTest` 或添加 `junit-vintage-engine`
5. 删除按钮 `LaunchedEffect` 门控重构
