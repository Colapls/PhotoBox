package com.photobox.feature.applock

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.photobox.core.common.AppLockConstants
import com.photobox.core.data.datastore.UserPreferencesDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 监听 App 切回前台事件（ProcessLifecycleOwner.ON_START）。
 * 行为：
 * - 若 appLockEnabled == false → 标记为 unlocked（实际由 ViewModel 订阅 prefs 自动处理）
 * - 若 lastUnlockAt 在 30s 内 → 不重新弹锁
 * - 否则 → 重新进入 locked 状态
 *
 * 本 Observer **只**通知 DataStore 不直接更新；ViewModel 会订阅 prefs 自动调整 isLocked。
 * 真正的 isLocked 来源是 [AppLockViewModel] 的 [kotlinx.coroutines.flow.launchIn] 链路。
 *
 * 本类存在的目的：
 * 1. 提供「应用回到前台」这个触发点（便于后续扩展：例如记录「应用进入次数」）。
 * 2. 在 ON_STOP 时清零 lastUnlockAt（spec §4.5：30s 计时从 onStart 起算，background → onStart 重置）。
 */
@Singleton
class AppLockLifecycleObserver @Inject constructor(
    private val prefs: UserPreferencesDataSource,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStart(owner: LifecycleOwner) {
        // ProcessLifecycleOwner 触发 ON_START 时调用（App 切回前台）。
        // TTL-reset 逻辑由 simulateOnStart() 实现，这里桥接以确保 spec §4.5 的 30s 窗口生效。
        simulateOnStart()
    }

    override fun onStop(owner: LifecycleOwner) {
        // 进入后台：保留 lastUnlockAt，由 ViewModel 在下次 onStart 时根据时间窗口判断。
        // 不在这里清零，否则 spec 的 30s 免认证失效。
    }

    /**
     * 用于测试：在外部触发「模拟 ON_START」。生产代码不需要调用，由 ProcessLifecycleOwner 自动回调。
     */
    fun simulateOnStart() {
        scope.launch {
            val p = prefs.userPreferences.first()
            if (p.appLockEnabled) {
                val now = System.currentTimeMillis()
                // 若超过 TTL → 清零 lastUnlockAt → ViewModel 会看到 isLocked=true
                if (p.appLockLastUnlockAt > 0L && (now - p.appLockLastUnlockAt) > AppLockConstants.UNLOCK_TTL_MS) {
                    prefs.setAppLockLastUnlockAt(0L)
                }
            }
        }
    }
}
